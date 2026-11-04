package br.com.financeiro.desktop.api.dto;

import java.time.Instant;

/**
 * Resposta de {@code POST /auth/login}.
 *
 * @param token    token JWT
 * @param tipo     tipo do token, sempre {@code Bearer}
 * @param expiraEm data e hora em que o token expira, em UTC
 */
public record TokenResponse(String token, String tipo, Instant expiraEm) {
}
