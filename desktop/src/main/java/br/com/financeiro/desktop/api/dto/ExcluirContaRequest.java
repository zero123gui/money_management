package br.com.financeiro.desktop.api.dto;

/**
 * Corpo de {@code DELETE /usuarios/me}.
 *
 * @param senha senha atual em texto puro, enviada só para a API
 */
public record ExcluirContaRequest(String senha) {
}
