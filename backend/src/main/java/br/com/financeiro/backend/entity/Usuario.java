package br.com.financeiro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Pessoa com login próprio. O e-mail é o login e a senha só existe como hash.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "compartilha_resumo", nullable = false)
    private boolean sharesSummary;

    @Column(name = "versao_token", nullable = false)
    private int tokenVersion;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo JPA. */
    protected Usuario() {
    }

    /**
     * Cria um usuário que ainda não compartilha resumos.
     *
     * @param name         nome de exibição
     * @param email        e-mail de login
     * @param passwordHash hash BCrypt da senha, nunca a senha em texto puro
     */
    public Usuario(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * Troca a senha do usuário e encerra todos os tokens já emitidos para ele.
     *
     * @param passwordHash hash BCrypt da nova senha
     */
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        tokenVersion++;
    }

    /** @return versão de token atual; tokens de outra versão não valem mais */
    public int getTokenVersion() {
        return tokenVersion;
    }

    /** @return identificador gerado pelo banco */
    public Long getId() {
        return id;
    }

    /** @return nome de exibição */
    public String getName() {
        return name;
    }

    /** @return e-mail de login */
    public String getEmail() {
        return email;
    }

    /** @return hash BCrypt da senha */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @return se o usuário compartilha resumos com o grupo familiar */
    public boolean isSharesSummary() {
        return sharesSummary;
    }

    /** @return data e hora de criação, em UTC */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return data e hora da última alteração, em UTC */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
