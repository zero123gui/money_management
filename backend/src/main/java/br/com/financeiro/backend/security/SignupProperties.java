package br.com.financeiro.backend.security;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Quem pode criar conta, lido de {@code app.signup.*}.
 *
 * @param restricted    se o cadastro aceita só os e-mails da lista
 * @param allowedEmails e-mails que podem criar conta; com a restrição ligada e a
 *                      lista vazia, ninguém consegue se cadastrar
 */
@ConfigurationProperties("app.signup")
public record SignupProperties(boolean restricted, Set<String> allowedEmails) {

    /**
     * Normaliza a lista, pois o e-mail não diferencia maiúsculas no resto da API.
     */
    public SignupProperties {
        allowedEmails = allowedEmails.stream()
                .map(SignupProperties::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Diz se o e-mail pode criar conta.
     *
     * @param email e-mail informado no cadastro
     * @return {@code true} sem restrição ou se o e-mail estiver na lista
     */
    public boolean allows(String email) {
        return !restricted || allowedEmails.contains(normalize(email));
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
