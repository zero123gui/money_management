package br.com.financeiro.backend.service;

import br.com.financeiro.backend.dto.TokenResponse;
import br.com.financeiro.backend.entity.Usuario;
import br.com.financeiro.backend.security.JwtProperties;
import br.com.financeiro.backend.security.TokenRevocationValidator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite os tokens JWT. O token só carrega o id do usuário: nome e e-mail ficam
 * fora porque o conteúdo de um JWT pode ser lido por quem o tiver.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    /**
     * Cria o service.
     *
     * @param encoder    assinador de tokens
     * @param properties configuração do JWT
     */
    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /**
     * Emite um token para o usuário.
     *
     * @param usuario usuário autenticado
     * @return o token e a data em que ele expira
     */
    public TokenResponse issue(Usuario usuario) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                // O id identifica este token no logout; a versão o encerra na troca de senha.
                .id(UUID.randomUUID().toString())
                .claim(TokenRevocationValidator.VERSION_CLAIM, usuario.getTokenVersion())
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiresAt);
    }
}
