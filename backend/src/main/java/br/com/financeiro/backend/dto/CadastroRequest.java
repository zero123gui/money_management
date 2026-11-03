package br.com.financeiro.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /auth/cadastro}.
 *
 * @param nome  nome de exibição
 * @param email e-mail de login
 * @param senha senha em texto puro, que só vira hash no service
 */
public record CadastroRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 254, message = "deve ter no máximo 254 caracteres")
        String email,

        @NotBlank(message = "é obrigatória")
        @ValidPassword
        String senha) {
}
