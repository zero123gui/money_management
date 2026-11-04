package br.com.financeiro.desktop.api.dto;

/**
 * Corpo de {@code PUT /categorias/{id}}.
 *
 * @param nome  nome da categoria
 * @param ativa {@code false} para desativar
 */
public record CategoriaUpdateRequest(String nome, boolean ativa) {
}
