package br.com.financeiro.desktop.api.dto;

/**
 * Corpo de {@code POST /auth/cadastro}.
 *
 * @param nome  nome do usuário
 * @param email e-mail, usado no login
 * @param senha senha em texto puro, enviada só para a API
 */
public record CadastroRequest(String nome, String email, String senha) {
}
