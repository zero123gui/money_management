package br.com.financeiro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Token encerrado pelo logout antes de expirar. Fica guardado só até a
 * expiração, quando a própria assinatura passa a recusá-lo.
 */
@Entity
@Table(name = "token_revogado")
public class TokenRevogado {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiresAt;

    /** Exigido pelo JPA. */
    protected TokenRevogado() {
    }

    /**
     * Registra um token revogado.
     *
     * @param id        identificador do token (claim {@code jti})
     * @param ownerId   dono do token
     * @param expiresAt quando o token expiraria
     */
    public TokenRevogado(UUID id, Long ownerId, Instant expiresAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
    }

    /** @return identificador do token */
    public UUID getId() {
        return id;
    }

    /** @return dono do token */
    public Long getOwnerId() {
        return ownerId;
    }

    /** @return quando o token expiraria */
    public Instant getExpiresAt() {
        return expiresAt;
    }
}
