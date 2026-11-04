package br.com.financeiro.desktop.api.dto;

import java.util.List;

/**
 * Corpo de erro da API, no formato RFC 9457.
 *
 * @param title  título do erro
 * @param detail explicação em português
 * @param erros  campos recusados; só existe em erros de validação
 */
public record ProblemResponse(String title, String detail, List<FieldProblem> erros) {

    /**
     * Campo recusado pela validação.
     *
     * @param campo    nome do campo no JSON
     * @param mensagem motivo em português
     */
    public record FieldProblem(String campo, String mensagem) {
    }
}
