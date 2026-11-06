package br.com.financeiro.backend.security;

import br.com.financeiro.backend.repository.TokenRevogadoRepository;
import br.com.financeiro.backend.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Recusa tokens com assinatura e validade corretas que já foram encerrados:
 * os emitidos antes da última troca de senha e os revogados pelo logout.
 * Roda em toda requisição autenticada, antes de chegar a qualquer controller.
 */
@Component
public class TokenRevocationValidator implements OAuth2TokenValidator<Jwt> {

    /** Claim com a versão de token do usuário quando o token foi emitido. */
    public static final String VERSION_CLAIM = "ver";

    private static final OAuth2TokenValidatorResult REVOKED = OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Token encerrado", null));

    private final UsuarioRepository usuarios;
    private final TokenRevogadoRepository revokedTokens;

    /**
     * Cria o validador.
     *
     * @param usuarios      acesso à versão de token de cada usuário
     * @param revokedTokens tokens encerrados pelo logout
     */
    public TokenRevocationValidator(UsuarioRepository usuarios, TokenRevogadoRepository revokedTokens) {
        this.usuarios = usuarios;
        this.revokedTokens = revokedTokens;
    }

    /**
     * Confere se o token ainda vale.
     *
     * @param jwt token já com assinatura e validade conferidas
     * @return sucesso, ou falha se o token tiver sido encerrado ou não tiver as claims esperadas
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        UUID tokenId;
        long userId;
        try {
            tokenId = UUID.fromString(jwt.getId());
            userId = Long.parseLong(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException missingOrMalformedClaim) {
            return REVOKED;
        }
        if (!(jwt.getClaims().get(VERSION_CLAIM) instanceof Number version)) {
            return REVOKED;
        }
        boolean currentVersion = usuarios.findById(userId)
                .map(usuario -> usuario.getTokenVersion() == version.intValue())
                .orElse(false);
        if (!currentVersion || revokedTokens.existsById(tokenId)) {
            return REVOKED;
        }
        return OAuth2TokenValidatorResult.success();
    }
}
