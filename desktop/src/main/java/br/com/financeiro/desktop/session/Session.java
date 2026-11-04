package br.com.financeiro.desktop.session;

/**
 * Sessão do usuário logado. O token fica só na memória: nunca vai para o disco,
 * então fechar o aplicativo encerra a sessão.
 */
public class Session {

    // volatile porque a tela grava o token e as chamadas à API o leem em outra thread.
    private volatile String token;

    /**
     * Inicia a sessão.
     *
     * @param token token JWT recebido no login
     */
    public void open(String token) {
        this.token = token;
    }

    /** Descarta o token. É o logout do RF-03, pois a API não guarda sessão. */
    public void clear() {
        token = null;
    }

    /** @return o token atual, ou {@code null} se não houver sessão */
    public String token() {
        return token;
    }

    /** @return {@code true} se houver um token guardado */
    public boolean isActive() {
        return token != null;
    }
}
