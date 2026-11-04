package br.com.financeiro.desktop.api.dto;

import java.util.List;

/**
 * Página de uma lista da API.
 *
 * @param conteudo       itens da página
 * @param pagina         número da página, começando em 0
 * @param tamanho        quantidade máxima de itens por página
 * @param totalElementos total de itens em todas as páginas
 * @param totalPaginas   total de páginas
 * @param <T>            tipo dos itens
 */
public record PageResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {
}
