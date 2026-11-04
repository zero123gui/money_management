package br.com.financeiro.backend.repository;

import br.com.financeiro.backend.dto.ResumoCategoriaResponse;
import br.com.financeiro.backend.entity.Lancamento;
import br.com.financeiro.backend.entity.Tipo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acesso a lançamentos. Toda consulta exige o dono e separa ativos de excluídos,
 * e não há exclusão física: excluir é preencher {@code deletedAt} na entidade.
 * As consultas já trazem a categoria, que toda resposta da API exibe.
 */
public interface LancamentoRepository extends Repository<Lancamento, Long> {

    /**
     * Grava um lançamento novo ou alterado.
     *
     * @param lancamento lançamento a gravar
     * @return o lançamento gravado
     */
    Lancamento save(Lancamento lancamento);

    /**
     * Busca um lançamento ativo do usuário.
     *
     * @param id      identificador do lançamento
     * @param ownerId identificador do usuário autenticado
     * @return o lançamento, se existir, for do usuário e estiver ativo
     */
    @EntityGraph(attributePaths = "category")
    Optional<Lancamento> findByIdAndOwnerIdAndDeletedAtIsNull(Long id, Long ownerId);

    /**
     * Busca um lançamento excluído do usuário.
     *
     * @param id      identificador do lançamento
     * @param ownerId identificador do usuário autenticado
     * @return o lançamento, se existir, for do usuário e estiver excluído
     */
    @EntityGraph(attributePaths = "category")
    Optional<Lancamento> findByIdAndOwnerIdAndDeletedAtIsNotNull(Long id, Long ownerId);

    /**
     * Lista os lançamentos ativos do usuário, do mais recente para o mais antigo.
     * Os filtros são opcionais: {@code null} não filtra.
     *
     * @param ownerId    identificador do usuário autenticado
     * @param start      data inicial, inclusiva
     * @param end        data final, inclusiva
     * @param type       tipo do lançamento
     * @param categoryId identificador da categoria
     * @param pageable   página pedida
     * @return a página de lançamentos ativos
     */
    // coalesce em vez de ":x is null or ...": sem a coluna ao lado, o PostgreSQL
    // não consegue deduzir o tipo de um parâmetro nulo e recusa a consulta.
    @EntityGraph(attributePaths = "category")
    @Query("""
            select l from Lancamento l
            where l.owner.id = :ownerId and l.deletedAt is null
              and l.date >= coalesce(:start, l.date)
              and l.date <= coalesce(:end, l.date)
              and l.type = coalesce(:type, l.type)
              and l.category.id = coalesce(:categoryId, l.category.id)
            order by l.date desc, l.id desc
            """)
    Page<Lancamento> findActive(@Param("ownerId") Long ownerId, @Param("start") LocalDate start,
            @Param("end") LocalDate end, @Param("type") Tipo type, @Param("categoryId") Long categoryId,
            Pageable pageable);

    /**
     * Lista os lançamentos excluídos do usuário, do excluído mais recentemente
     * para o mais antigo.
     *
     * @param ownerId  identificador do usuário autenticado
     * @param pageable página pedida
     * @return a página de lançamentos excluídos
     */
    @EntityGraph(attributePaths = "category")
    Page<Lancamento> findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long ownerId, Pageable pageable);

    /**
     * Soma os lançamentos ativos do usuário em um período, por tipo. Um tipo sem
     * lançamentos não aparece no resultado.
     *
     * @param ownerId identificador do usuário autenticado
     * @param start   data inicial, inclusiva
     * @param end     data final, inclusiva
     * @return o total de cada tipo com lançamentos
     */
    @Query("""
            select new br.com.financeiro.backend.repository.TypeTotal(l.type, sum(l.amount))
            from Lancamento l
            where l.owner.id = :ownerId and l.deletedAt is null and l.date between :start and :end
            group by l.type
            """)
    List<TypeTotal> sumActiveByType(@Param("ownerId") Long ownerId, @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Soma os lançamentos ativos do usuário em um período, por categoria, do
     * maior total para o menor. Entra também categoria hoje desativada, pois o
     * dinheiro daquele período continua lançado nela.
     *
     * @param ownerId identificador do usuário autenticado
     * @param start   data inicial, inclusiva
     * @param end     data final, inclusiva
     * @param type    tipo do lançamento, ou {@code null} para todos
     * @return o total e a quantidade de cada categoria com lançamentos
     */
    @Query("""
            select new br.com.financeiro.backend.dto.ResumoCategoriaResponse(
                c.id, c.name, l.type, sum(l.amount), count(l))
            from Lancamento l join l.category c
            where l.owner.id = :ownerId and l.deletedAt is null and l.date between :start and :end
              and l.type = coalesce(:type, l.type)
            group by c.id, c.name, l.type
            order by sum(l.amount) desc, c.name, c.id
            """)
    List<ResumoCategoriaResponse> sumActiveByCategory(@Param("ownerId") Long ownerId,
            @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("type") Tipo type);
}
