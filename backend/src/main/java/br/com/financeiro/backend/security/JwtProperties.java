package br.com.financeiro.backend.security;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do JWT, lida de {@code app.jwt.*}.
 *
 * @param secret     chave de assinatura em Base64
 * @param expiration validade do token
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final int MIN_KEY_BYTES = 32;

    /**
     * Monta a chave de assinatura HS256.
     *
     * @return a chave decodificada
     * @throws IllegalStateException se a chave não for Base64 válido ou tiver
     *                               menos de 32 bytes; a API não sobe assim
     */
    public SecretKey signingKey() {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException invalidBase64) {
            // Sem a causa: a mensagem dela repete um caractere da chave.
            throw new IllegalStateException("JWT_SECRET não está em Base64 válido.");
        }
        if (key.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos " + MIN_KEY_BYTES + " bytes.");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }
}
