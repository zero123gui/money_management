package br.com.financeiro.backend.dto;

import br.com.financeiro.backend.entity.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Dados do usuário devolvidos pela API, sem a senha.
 *
 * @param id       identificador do usuário
 * @param nome     nome de exibição
 * @param email    e-mail de login
 * @param criadoEm data e hora de criação, em UTC
 */
public record UsuarioResponse(Long id, String nome, String email, Instant criadoEm) {

    /**
     * Monta a resposta a partir da entidade.
     *
     * @param usuario usuário a expor
     * @return os dados públicos do usuário
     */
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getName(), usuario.getEmail(),
                usuario.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
