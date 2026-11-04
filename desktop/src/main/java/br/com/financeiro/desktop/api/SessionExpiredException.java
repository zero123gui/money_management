package br.com.financeiro.desktop.api;

import java.util.List;

/**
 * A API recusou o token, ou não há token para enviar: o usuário precisa entrar de novo.
 */
public class SessionExpiredException extends ApiException {

    /** Cria a falha, com a mensagem padrão de sessão expirada. */
    public SessionExpiredException() {
        super(401, "Sua sessão expirou. Entre novamente.", List.of());
    }
}
