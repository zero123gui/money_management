package br.com.financeiro.desktop.api.dto;

/**
 * Categoria devolvida pela API.
 *
 * @param id     identificador
 * @param nome   nome
 * @param tipo   sentido dos lançamentos da categoria
 * @param padrao {@code true} se for uma categoria do sistema
 * @param ativa  {@code false} se tiver sido desativada
 */
public record CategoriaResponse(Long id, String nome, Tipo tipo, boolean padrao, boolean ativa) {
}
