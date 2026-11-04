package br.com.financeiro.desktop.api.dto;

/**
 * Corpo de {@code POST /auth/login}.
 *
 * @param email e-mail do usuário
 * @param senha senha em texto puro, enviada só para a API
 */
public record LoginRequest(String email, String senha) {
}
