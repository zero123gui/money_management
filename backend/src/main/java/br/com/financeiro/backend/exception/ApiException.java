package br.com.financeiro.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de negócio com a resposta já definida. As mensagens são exibidas ao
 * usuário, por isso ficam em português.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    /**
     * Cria o erro.
     *
     * @param status código HTTP da resposta
     * @param title  título do erro
     * @param detail explicação pronta para exibir
     */
    public ApiException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    /** @return código HTTP da resposta */
    public HttpStatus getStatus() {
        return status;
    }

    /** @return título do erro */
    public String getTitle() {
        return title;
    }
}
