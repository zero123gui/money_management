package br.com.financeiro.backend.dto;

import java.time.Instant;

/**
 * Resposta de {@code POST /auth/login}.
 *
 * @param token    JWT a enviar no cabeçalho {@code Authorization}
 * @param tipo     esquema de autenticação, sempre {@code Bearer}
 * @param expiraEm data e hora em que o token expira, em UTC
 */
public record TokenResponse(String token, String tipo, Instant expiraEm) {
}
