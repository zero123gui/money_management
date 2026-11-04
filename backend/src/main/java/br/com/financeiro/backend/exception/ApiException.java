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

    /**
     * Cria o erro de recurso inexistente. É o mesmo para um recurso de outro
     * usuário, para não revelar que ele existe.
     *
     * @return o erro 404
     */
    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Não encontrado", "O recurso pedido não existe.");
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
