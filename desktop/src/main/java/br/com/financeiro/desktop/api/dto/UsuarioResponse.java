package br.com.financeiro.desktop.api.dto;

import java.time.Instant;

/**
 * Usuário devolvido pela API.
 *
 * @param id       identificador
 * @param nome     nome
 * @param email    e-mail
 * @param criadoEm data e hora de criação, em UTC
 */
public record UsuarioResponse(Long id, String nome, String email, Instant criadoEm) {
}
