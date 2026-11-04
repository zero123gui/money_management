package br.com.financeiro.backend.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Lista paginada no formato do contrato ({@code docs/API.md}, seção 1.7).
 *
 * @param conteudo       itens da página
 * @param pagina         número da página, a partir de 0
 * @param tamanho        tamanho da página
 * @param totalElementos total de itens em todas as páginas
 * @param totalPaginas   total de páginas
 * @param <T>            tipo dos itens
 */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    /**
     * Monta a resposta a partir de uma página do Spring Data.
     *
     * @param page   página consultada
     * @param mapper converte cada item para o formato da API
     * @param <E>    tipo dos itens consultados
     * @param <T>    tipo dos itens da resposta
     * @return a página no formato do contrato
     */
    public static <E, T> PaginaResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
