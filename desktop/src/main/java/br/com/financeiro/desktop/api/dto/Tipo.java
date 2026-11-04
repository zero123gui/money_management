package br.com.financeiro.desktop.api.dto;

/**
 * Sentido de um lançamento ou de uma categoria.
 */
public enum Tipo {
    /** Dinheiro que entra. */
    RECEITA("Receita"),
    /** Dinheiro que sai. */
    DESPESA("Despesa");

    private final String label;

    Tipo(String label) {
        this.label = label;
    }

    /** @return nome para exibir na tela */
    public String label() {
        return label;
    }
}
