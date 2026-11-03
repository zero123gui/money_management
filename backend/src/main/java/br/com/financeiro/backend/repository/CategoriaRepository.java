package br.com.financeiro.backend.repository;

import br.com.financeiro.backend.entity.Categoria;
import br.com.financeiro.backend.entity.Tipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acesso a categorias. As consultas devolvem só o que o usuário pode ver: as
 * categorias padrão e as dele. Não há exclusão, pois categorias só são desativadas.
 */
public interface CategoriaRepository extends Repository<Categoria, Long> {

    /**
     * Grava uma categoria nova ou alterada.
     *
     * @param categoria categoria a gravar
     * @return a categoria gravada
     */
    Categoria save(Categoria categoria);

    /**
     * Busca uma categoria padrão ou do próprio usuário.
     *
     * @param id      identificador da categoria
     * @param ownerId identificador do usuário autenticado
     * @return a categoria, se existir e o usuário puder vê-la
     */
    @Query("select c from Categoria c where c.id = :id and (c.owner is null or c.owner.id = :ownerId)")
    Optional<Categoria> findVisibleById(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /**
     * Lista as categorias padrão e as do usuário, sem ordem definida.
     *
     * @param ownerId identificador do usuário autenticado
     * @return as categorias que o usuário pode ver
     */
    @Query("select c from Categoria c where c.owner is null or c.owner.id = :ownerId")
    List<Categoria> findAllVisible(@Param("ownerId") Long ownerId);

    /**
     * Verifica se o usuário já tem uma categoria própria com esse nome e tipo,
     * sem diferenciar maiúsculas.
     *
     * @param ownerId identificador do usuário autenticado
     * @param name    nome da categoria
     * @param type    tipo da categoria
     * @return {@code true} se já existir
     */
    @Query("""
            select count(c) > 0 from Categoria c
            where c.owner.id = :ownerId and lower(c.name) = lower(:name) and c.type = :type
            """)
    boolean existsOwned(@Param("ownerId") Long ownerId, @Param("name") String name, @Param("type") Tipo type);

    /**
     * Verifica se outra categoria própria do usuário já usa esse nome e tipo,
     * sem diferenciar maiúsculas. Serve para renomear sem conflitar consigo mesma.
     *
     * @param ownerId   identificador do usuário autenticado
     * @param name      nome da categoria
     * @param type      tipo da categoria
     * @param excludeId categoria que está sendo alterada
     * @return {@code true} se já existir
     */
    @Query("""
            select count(c) > 0 from Categoria c
            where c.owner.id = :ownerId and lower(c.name) = lower(:name) and c.type = :type and c.id <> :excludeId
            """)
    boolean existsOwnedExcept(@Param("ownerId") Long ownerId, @Param("name") String name,
            @Param("type") Tipo type, @Param("excludeId") Long excludeId);

    /**
     * Grava a alteração no banco na hora, para que uma violação do índice único
     * apareça aqui e não só no fim da transação.
     *
     * @param categoria categoria a gravar
     * @return a categoria gravada
     */
    Categoria saveAndFlush(Categoria categoria);
}
