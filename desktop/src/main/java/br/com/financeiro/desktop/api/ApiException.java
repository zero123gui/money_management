package br.com.financeiro.desktop.api;

import br.com.financeiro.desktop.api.dto.ProblemResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Falha em uma chamada à API. A mensagem está pronta para exibir ao usuário.
 */
public class ApiException extends RuntimeException {

    /** Status usado quando a API não chegou a responder. */
    public static final int NO_RESPONSE = 0;

    private final int status;
    private final transient List<ProblemResponse.FieldProblem> fieldProblems;

    /**
     * Cria a falha.
     *
     * @param status        status HTTP, ou {@link #NO_RESPONSE}
     * @param message       mensagem para o usuário
     * @param fieldProblems campos recusados pela validação da API
     */
    public ApiException(int status, String message, List<ProblemResponse.FieldProblem> fieldProblems) {
        super(message);
        this.status = status;
        this.fieldProblems = List.copyOf(fieldProblems);
    }

    /** @return status HTTP, ou {@link #NO_RESPONSE} se a API não respondeu */
    public int getStatus() {
        return status;
    }

    /** @return campos recusados pela validação da API; vazio nos demais erros */
    public List<ProblemResponse.FieldProblem> getFieldProblems() {
        return fieldProblems;
    }

    /**
     * Monta o texto do erro para a tela, com uma linha por campo recusado.
     *
     * @param labels nome de cada campo do JSON na tela, como {@code email} para "E-mail"
     * @return texto pronto para exibir
     */
    public String describe(Map<String, String> labels) {
        if (fieldProblems.isEmpty()) {
            return getMessage();
        }
        return fieldProblems.stream()
                .map(problem -> labels.getOrDefault(problem.campo(), problem.campo()) + ": " + problem.mensagem())
                .collect(Collectors.joining("\n"));
    }
}
