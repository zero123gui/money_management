package br.com.financeiro.desktop.api.dto;

/**
 * Corpo de {@code PUT /usuarios/me/senha}.
 *
 * @param senhaAtual senha atual em texto puro, enviada só para a API
 * @param novaSenha  nova senha em texto puro, enviada só para a API
 */
public record AlterarSenhaRequest(String senhaAtual, String novaSenha) {
}
