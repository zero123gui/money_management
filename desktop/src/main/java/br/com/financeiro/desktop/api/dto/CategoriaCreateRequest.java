package br.com.financeiro.desktop.api.dto;

/**
 * Corpo de {@code POST /categorias}.
 *
 * @param nome nome da categoria
 * @param tipo sentido dos lançamentos da categoria; não muda depois de criada
 */
public record CategoriaCreateRequest(String nome, Tipo tipo) {
}
