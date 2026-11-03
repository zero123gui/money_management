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

/**
 * Classificação de lançamentos. Sem dono, é uma categoria padrão do sistema,
 * visível a todos e inserida por migração. Nunca é apagada, só desativada.
 */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 50)
    private String name;

    // O tipo não muda depois de criado: mudar deixaria lançamentos antigos incoerentes.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 7, updatable = false)
    private Tipo type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario owner;

    @Column(name = "ativa", nullable = false)
    private boolean active = true;

    /** Exigido pelo JPA. */
    protected Categoria() {
    }

    /**
     * Cria uma categoria própria e ativa.
     *
     * @param name  nome da categoria
     * @param type  tipo da categoria
     * @param owner usuário dono
     */
    public Categoria(String name, Tipo type, Usuario owner) {
        this.name = name;
        this.type = type;
        this.owner = owner;
    }

    /**
     * Renomeia, desativa ou reativa a categoria.
     *
     * @param name   novo nome
     * @param active se a categoria aparece nas opções de novos lançamentos
     */
    public void update(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    /** @return se é uma categoria padrão do sistema, sem dono */
    public boolean isStandard() {
        return owner == null;
    }

    /** @return identificador gerado pelo banco */
    public Long getId() {
        return id;
    }

    /** @return nome da categoria */
    public String getName() {
        return name;
    }

    /** @return tipo da categoria */
    public Tipo getType() {
        return type;
    }

    /** @return usuário dono, ou {@code null} se for uma categoria padrão */
    public Usuario getOwner() {
        return owner;
    }

    /** @return se a categoria aparece nas opções de novos lançamentos */
    public boolean isActive() {
        return active;
    }
}
