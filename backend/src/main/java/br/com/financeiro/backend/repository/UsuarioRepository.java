package br.com.financeiro.backend.repository;

import br.com.financeiro.backend.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acesso a usuários. Não tem listagem geral: nada na API lista todos os usuários.
 */
public interface UsuarioRepository extends Repository<Usuario, Long> {

    /**
     * Grava um usuário novo ou alterado.
     *
     * @param usuario usuário a gravar
     * @return o usuário gravado
     */
    Usuario save(Usuario usuario);

    /**
     * Busca um usuário pelo identificador.
     *
     * @param id identificador, vindo do token do usuário autenticado
     * @return o usuário, se existir
     */
    Optional<Usuario> findById(Long id);

    /**
     * Busca pelo e-mail de login, sem diferenciar maiúsculas.
     *
     * @param email e-mail de login
     * @return o usuário, se existir
     */
    // lower() em vez de "IgnoreCase" no nome do método, que geraria upper() e
    // não usaria o índice único em lower(email).
    @Query("select u from Usuario u where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmail(@Param("email") String email);

    /**
     * Exclui a conta. O banco apaga em cascata as categorias e os lançamentos.
     *
     * @param id identificador do usuário autenticado
     */
    void deleteById(Long id);
}
