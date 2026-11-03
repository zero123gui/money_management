package br.com.financeiro.backend.entity;

/**
 * Sentido do dinheiro em uma categoria ou lançamento. Os nomes são gravados no
 * banco e expostos na API, por isso ficam em português.
 */
public enum Tipo {

    /** Entrada de dinheiro. */
    RECEITA,

    /** Saída de dinheiro. */
    DESPESA
}
