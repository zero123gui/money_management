package br.com.financeiro.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code DELETE /usuarios/me}.
 *
 * @param senha senha atual, em texto puro, que confirma a exclusão
 */
public record ExcluirContaRequest(
        @NotBlank(message = "é obrigatória")
        String senha) {
}
