package br.com.financeiro.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /auth/login}.
 *
 * @param email e-mail de login
 * @param senha senha em texto puro
 */
public record LoginRequest(
        @NotBlank(message = "é obrigatório")
        String email,

        @NotBlank(message = "é obrigatória")
        String senha) {
}
