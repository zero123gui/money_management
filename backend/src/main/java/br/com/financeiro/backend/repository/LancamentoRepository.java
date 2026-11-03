package br.com.financeiro.backend.repository;

import br.com.financeiro.backend.entity.Lancamento;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Acesso a lançamentos. Toda consulta exige o dono e separa ativos de excluídos,
 * e não há exclusão física: excluir é preencher {@code deletedAt} na entidade.
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
    Optional<Lancamento> findByIdAndOwnerIdAndDeletedAtIsNull(Long id, Long ownerId);

    /**
     * Busca um lançamento excluído do usuário.
     *
     * @param id      identificador do lançamento
     * @param ownerId identificador do usuário autenticado
     * @return o lançamento, se existir, for do usuário e estiver excluído
     */
    Optional<Lancamento> findByIdAndOwnerIdAndDeletedAtIsNotNull(Long id, Long ownerId);

    /**
     * Lista os lançamentos ativos do usuário, do mais recente para o mais antigo.
     *
     * @param ownerId  identificador do usuário autenticado
     * @param pageable página pedida
     * @return a página de lançamentos ativos
     */
    Page<Lancamento> findByOwnerIdAndDeletedAtIsNullOrderByDateDescIdDesc(Long ownerId, Pageable pageable);

    /**
     * Lista os lançamentos excluídos do usuário, do excluído mais recentemente
     * para o mais antigo.
     *
     * @param ownerId  identificador do usuário autenticado
     * @param pageable página pedida
     * @return a página de lançamentos excluídos
     */
    Page<Lancamento> findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long ownerId, Pageable pageable);
}
