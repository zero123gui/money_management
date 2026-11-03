package br.com.financeiro.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code PUT /usuarios/me/senha}.
 *
 * @param senhaAtual senha atual, em texto puro
 * @param novaSenha  nova senha, em texto puro, que só vira hash no service
 */
public record AlterarSenhaRequest(
        @NotBlank(message = "é obrigatória")
        String senhaAtual,

        @NotBlank(message = "é obrigatória")
        @ValidPassword
        String novaSenha) {
}
