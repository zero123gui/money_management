package br.com.financeiro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Receita ou despesa de um usuário. É sempre privada: só o dono pode ler.
 * O valor é sempre positivo; o tipo diz o sentido do dinheiro.
 */
@Entity
@Table(name = "lancamento")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 7)
    private Tipo type;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "data", nullable = false)
    private LocalDate date;

    // A chave estrangeira do banco é composta por (categoria_id, tipo), o que
    // barra categoria de outro tipo mesmo que o service deixe passar.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria category;

    @Column(name = "descricao", length = 255)
    private String description;

    @Column(name = "excluido_em")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo JPA. */
    protected Lancamento() {
    }

    /**
     * Cria um lançamento ativo.
     *
     * @param owner       usuário dono
     * @param type        tipo do lançamento, igual ao da categoria
     * @param amount      valor positivo, com até 2 casas decimais
     * @param date        data do lançamento
     * @param category    categoria do lançamento
     * @param description descrição opcional
     */
    public Lancamento(Usuario owner, Tipo type, BigDecimal amount, LocalDate date,
            Categoria category, String description) {
        this.owner = owner;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    /**
     * Substitui os dados editáveis do lançamento.
     *
     * @param type        tipo do lançamento, igual ao da categoria
     * @param amount      valor positivo, com até 2 casas decimais
     * @param date        data do lançamento
     * @param category    categoria do lançamento
     * @param description descrição opcional
     */
    public void update(Tipo type, BigDecimal amount, LocalDate date, Categoria category, String description) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    /**
     * Exclui logicamente: o lançamento some das listas, do saldo e dos resumos,
     * sem perder o valor.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /** Volta um lançamento excluído a ser ativo. */
    public void restore() {
        this.deletedAt = null;
    }

    /** @return identificador gerado pelo banco */
    public Long getId() {
        return id;
    }

    /** @return usuário dono */
    public Usuario getOwner() {
        return owner;
    }

    /** @return tipo do lançamento */
    public Tipo getType() {
        return type;
    }

    /** @return valor positivo */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return data do lançamento */
    public LocalDate getDate() {
        return date;
    }

    /** @return categoria do lançamento */
    public Categoria getCategory() {
        return category;
    }

    /** @return descrição, ou {@code null} se não houver */
    public String getDescription() {
        return description;
    }

    /** @return data e hora da exclusão em UTC, ou {@code null} se estiver ativo */
    public Instant getDeletedAt() {
        return deletedAt;
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
