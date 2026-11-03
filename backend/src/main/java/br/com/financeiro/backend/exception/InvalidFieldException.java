package br.com.financeiro.backend.exception;

/**
 * Campo recusado por uma regra de negócio, depois de passar pela validação de
 * formato. A resposta é igual à de um campo inválido: {@code 400} com a lista
 * {@code erros}.
 */
public class InvalidFieldException extends RuntimeException {

    private final String field;

    /**
     * Cria o erro.
     *
     * @param field   nome do campo no JSON
     * @param message o que está errado, pronto para exibir
     */
    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** @return nome do campo no JSON */
    public String getField() {
        return field;
    }
}
