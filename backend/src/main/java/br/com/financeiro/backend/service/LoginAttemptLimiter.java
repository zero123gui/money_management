package br.com.financeiro.backend.service;

import br.com.financeiro.backend.security.LoginLimitProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Conta as falhas de login por e-mail e bloqueia novas tentativas depois do
 * limite. E-mails que não existem contam igual: tratá-los de outro jeito
 * revelaria quais estão cadastrados.
 *
 * <p>A contagem fica na memória, então vale para uma única instância da API e
 * recomeça quando ela reinicia.
 */
@Service
public class LoginAttemptLimiter {

    /** Maior e-mail aceito no cadastro: um texto maior não tem conta para proteger. */
    private static final int MAX_EMAIL_LENGTH = 254;
    /** Teto da memória usada. Cada entrada expira sozinha, e o login é lento demais para encher isto. */
    private static final int MAX_TRACKED_EMAILS = 100_000;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration blockDuration;
    private final Clock clock;

    /**
     * Cria o limitador com o relógio do sistema.
     *
     * @param properties limite e duração do bloqueio
     */
    @Autowired
    public LoginAttemptLimiter(LoginLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptLimiter(LoginLimitProperties properties, Clock clock) {
        this.maxAttempts = properties.maxAttempts();
        this.blockDuration = properties.blockDuration();
        this.clock = clock;
    }

    /**
     * Informa se o e-mail está bloqueado.
     *
     * @param email e-mail da tentativa
     * @return tempo que falta para o desbloqueio, ou vazio se ele puder tentar
     */
    public Optional<Duration> remainingBlock(String email) {
        Attempts current = attempts.get(key(email));
        Instant now = clock.instant();
        if (current == null || current.failures() < maxAttempts || current.isExpired(now)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, current.expiresAt()));
    }

    /**
     * Registra uma tentativa errada.
     *
     * @param email e-mail da tentativa
     */
    public void recordFailure(String email) {
        String key = key(email);
        if (key.length() > MAX_EMAIL_LENGTH) {
            return;
        }
        Instant now = clock.instant();
        if (attempts.size() >= MAX_TRACKED_EMAILS) {
            attempts.values().removeIf(entry -> entry.isExpired(now));
            if (attempts.size() >= MAX_TRACKED_EMAILS && !attempts.containsKey(key)) {
                return;
            }
        }
        attempts.compute(key, (ignored, current) -> {
            int previous = current == null || current.isExpired(now) ? 0 : current.failures();
            return new Attempts(previous + 1, now.plus(blockDuration));
        });
    }

    /**
     * Zera a contagem depois de um login certo.
     *
     * @param email e-mail da tentativa
     */
    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    /** Segue a mesma regra do banco, em que "Ana@x.com" e "ana@x.com" são a mesma conta. */
    private static String key(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private record Attempts(int failures, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
