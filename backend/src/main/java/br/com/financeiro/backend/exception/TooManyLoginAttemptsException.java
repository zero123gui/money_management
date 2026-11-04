package br.com.financeiro.backend.exception;

import java.time.Duration;
import org.springframework.http.HttpStatus;

/**
 * Login recusado sem conferir a senha, porque o e-mail está bloqueado.
 */
public class TooManyLoginAttemptsException extends ApiException {

    private final transient Duration retryAfter;

    /**
     * Cria o erro.
     *
     * @param retryAfter tempo que falta para o desbloqueio
     */
    public TooManyLoginAttemptsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas",
                "Muitas tentativas de login. Tente novamente em %d %s.".formatted(
                        minutesRoundedUp(retryAfter), minutesRoundedUp(retryAfter) == 1 ? "minuto" : "minutos"));
        this.retryAfter = retryAfter;
    }

    /** @return tempo que falta para o desbloqueio */
    public Duration getRetryAfter() {
        return retryAfter;
    }

    private static long minutesRoundedUp(Duration duration) {
        return duration.plusSeconds(59).toMinutes();
    }
}
