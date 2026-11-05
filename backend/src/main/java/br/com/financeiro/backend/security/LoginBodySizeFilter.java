package br.com.financeiro.backend.security;

import br.com.financeiro.backend.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Recusa um pedido de login com corpo grande demais. A rota é pública, então
 * qualquer pessoa poderia ocupar a memória do servidor com corpos enormes.
 */
@Component
public class LoginBodySizeFilter extends OncePerRequestFilter {

    /** Folga larga para um e-mail de 254 caracteres e uma senha longa, mesmo com escapes no JSON. */
    static final int MAX_BODY_BYTES = 4 * 1024;

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final HandlerExceptionResolver resolver;

    /**
     * Cria o filtro.
     *
     * @param resolver tratamento de exceções do Spring MVC, que define o formato de erro da API
     */
    public LoginBodySizeFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // getServletPath, e não getRequestURI: já vem decodificado, então
        // "/auth/%6Cogin" não escapa do limite e ainda chega à rota de login.
        return !("POST".equals(request.getMethod()) && LOGIN_PATH.equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            reject(request, response);
            return;
        }
        // O tamanho declarado não basta: um corpo em partes (chunked) não declara nenhum.
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            reject(request, response);
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) {
        resolver.resolveException(request, response, null, new ApiException(HttpStatus.CONTENT_TOO_LARGE,
                "Pedido muito grande", "O corpo do pedido de login é grande demais."));
    }

    /**
     * Devolve o corpo já lido pelo filtro, que não pode ser lido duas vezes do pedido original.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream content = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return content.read();
                }

                @Override
                public int read(byte[] target, int offset, int length) {
                    return content.read(target, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return content.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("O corpo já foi lido; não há leitura assíncrona.");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
