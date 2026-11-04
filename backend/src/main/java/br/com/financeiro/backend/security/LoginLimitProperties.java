package br.com.financeiro.backend.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limite de tentativas de login, lido de {@code app.login.*}.
 *
 * @param maxAttempts   falhas seguidas que bloqueiam o e-mail
 * @param blockDuration tempo sem tentativas que encerra o bloqueio e zera a contagem
 */
@ConfigurationProperties("app.login")
public record LoginLimitProperties(int maxAttempts, Duration blockDuration) {

    /**
     * Valida a configuração.
     *
     * @throws IllegalStateException se os valores não fizerem sentido; a API não sobe assim
     */
    public LoginLimitProperties {
        if (maxAttempts < 1 || blockDuration == null || blockDuration.isNegative() || blockDuration.isZero()) {
            throw new IllegalStateException("LOGIN_MAX_ATTEMPTS e LOGIN_BLOCK_DURATION precisam ser positivos.");
        }
    }
}
