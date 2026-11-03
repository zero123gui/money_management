package br.com.financeiro.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Repassa os erros dos filtros de segurança ao tratamento do Spring MVC. Eles
 * acontecem antes de chegar a um controller; assim, o formato de erro da API
 * fica definido em um só lugar, no {@code ApiExceptionHandler}.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    /**
     * Cria o tratador de erros de segurança.
     *
     * @param resolver tratamento de exceções do Spring MVC
     */
    public SecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Responde a uma requisição sem autenticação válida.
     *
     * @param request   requisição recebida
     * @param response  resposta a preencher
     * @param exception motivo da recusa
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {
        resolver.resolveException(request, response, null, exception);
    }

    /**
     * Responde a uma ação não permitida ao usuário autenticado.
     *
     * @param request   requisição recebida
     * @param response  resposta a preencher
     * @param exception motivo da recusa
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) {
        resolver.resolveException(request, response, null, exception);
    }
}
