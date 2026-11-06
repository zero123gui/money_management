package br.com.financeiro.backend.repository;

import br.com.financeiro.backend.entity.TokenRevogado;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Tokens encerrados pelo logout. A consulta é pelo id do token, que só o dono
 * dele conhece; nada aqui lista tokens de um usuário.
 */
public interface TokenRevogadoRepository extends Repository<TokenRevogado, UUID> {

    /**
     * Registra um token revogado.
     *
     * @param token token a registrar
     * @return o registro gravado
     */
    TokenRevogado save(TokenRevogado token);

    /**
     * Diz se o token foi revogado.
     *
     * @param id identificador do token (claim {@code jti})
     * @return {@code true} se ele estiver na lista
     */
    boolean existsById(UUID id);

    /**
     * Apaga os registros de tokens que já expiraram, pois a assinatura já os recusa.
     *
     * @param now instante atual
     * @return quantos registros foram apagados
     */
    @Modifying
    @Query("delete from TokenRevogado t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
