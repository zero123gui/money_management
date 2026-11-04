package br.com.financeiro.backend.exception;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz exceções para o formato de erro da API (RFC 9457, {@code docs/API.md}
 * seção 1.6), com mensagens em português e sem detalhes internos.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Item da lista {@code erros} de uma resposta de validação.
     *
     * @param campo    nome do campo no JSON
     * @param mensagem o que está errado
     */
    public record FieldProblem(String campo, String mensagem) {
    }

    /**
     * Responde a um erro de negócio.
     *
     * @param exception erro lançado por um service
     * @return a resposta com o código e as mensagens do erro
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        ProblemDetail problem = problem(exception.getStatus(), exception.getTitle(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }

    /**
     * Responde a um campo recusado por uma regra de negócio.
     *
     * @param exception campo recusado
     * @return a resposta 400
     */
    @ExceptionHandler(InvalidFieldException.class)
    public ResponseEntity<ProblemDetail> handleInvalidField(InvalidFieldException exception) {
        ProblemDetail problem = invalidFields(List.of(new FieldProblem(exception.getField(), exception.getMessage())));
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Responde a uma requisição sem autenticação válida. A mensagem é a mesma
     * para todos os casos, para não indicar por que o token foi recusado.
     *
     * @param exception motivo da recusa
     * @return a resposta 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception) {
        ProblemDetail problem = problem(HttpStatus.UNAUTHORIZED, "Não autenticado",
                "Token ausente, inválido ou expirado. Entre novamente.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem);
    }

    /**
     * Responde a uma ação não permitida ao usuário autenticado.
     *
     * @param exception motivo da recusa
     * @return a resposta 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        ProblemDetail problem = problem(HttpStatus.FORBIDDEN, "Ação não permitida",
                "Você não tem permissão para esta ação.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Responde a um erro inesperado. O detalhe fica só no log do servidor.
     *
     * @param exception erro inesperado
     * @return a resposta 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Erro inesperado ao atender a requisição", exception);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente.");
        return ResponseEntity.internalServerError().body(problem);
    }

    /**
     * Responde a campos inválidos, listando cada um em {@code erros}.
     *
     * @param exception erros de validação
     * @param headers   cabeçalhos da resposta
     * @param status    código HTTP definido pelo Spring
     * @param request   requisição recebida
     * @return a resposta 400
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldProblem(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldProblem::campo))
                .toList();
        return ResponseEntity.badRequest().headers(headers).body(invalidFields(errors));
    }

    /**
     * Responde a um parâmetro obrigatório ausente na URL, indicando qual é.
     *
     * @param exception parâmetro ausente
     * @param headers   cabeçalhos da resposta
     * @param status    código HTTP definido pelo Spring
     * @param request   requisição recebida
     * @return a resposta 400
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = invalidFields(List.of(new FieldProblem(exception.getParameterName(), "é obrigatório")));
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    /**
     * Responde a um corpo que não é um JSON válido para a rota, como um valor
     * de {@code tipo} que não existe.
     *
     * @param exception erro de leitura
     * @param headers   cabeçalhos da resposta
     * @param status    código HTTP definido pelo Spring
     * @param request   requisição recebida
     * @return a resposta 400
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "O corpo da requisição é inválido. Confira o JSON e os valores dos campos.");
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    /**
     * Padroniza os demais erros do Spring MVC, como rota inexistente, cujas
     * mensagens originais são em inglês.
     *
     * @param exception erro do Spring MVC
     * @param body      corpo proposto pelo Spring, descartado
     * @param headers   cabeçalhos da resposta
     * @param status    código HTTP definido pelo Spring
     * @param request   requisição recebida
     * @return a resposta com o mesmo código e as mensagens em português
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = switch (status.value()) {
            case 404 -> problem(status, "Não encontrado", "O recurso pedido não existe.");
            case 405 -> problem(status, "Método não permitido", "Esta rota não aceita o método usado.");
            case 415 -> problem(status, "Formato não suportado", "Envie o corpo em JSON.");
            default -> status.is4xxClientError()
                    ? problem(status, "Requisição inválida", "A requisição não pôde ser atendida.")
                    : problem(status, "Erro interno", "Ocorreu um erro inesperado. Tente novamente.");
        };
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    private static ProblemDetail invalidFields(List<FieldProblem> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos.");
        problem.setProperty("erros", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
