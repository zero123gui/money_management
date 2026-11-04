package br.com.financeiro.desktop.api;

import java.net.URI;
import java.util.Set;

/**
 * Endereço da API.
 *
 * @param baseUrl endereço sem barra no fim, por exemplo {@code http://localhost:8080}
 */
public record ApiConfig(String baseUrl) {

    /** Variável de ambiente que define o endereço da API. */
    public static final String ENVIRONMENT_VARIABLE = "API_BASE_URL";

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    /**
     * Lê o endereço da variável de ambiente, com {@code http://localhost:8080} como padrão.
     *
     * @return configuração validada
     * @throws IllegalArgumentException se o endereço for inválido ou inseguro
     */
    public static ApiConfig fromEnvironment() {
        return of(System.getenv(ENVIRONMENT_VARIABLE));
    }

    /**
     * Valida um endereço. Fora da própria máquina, só aceita HTTPS (RNF-03), pois
     * senhas e tokens passariam abertos pela rede.
     *
     * @param value endereço informado; vazio ou {@code null} usa o padrão
     * @return configuração validada
     * @throws IllegalArgumentException se o endereço for inválido ou inseguro
     */
    public static ApiConfig of(String value) {
        if (value == null || value.isBlank()) {
            return new ApiConfig(DEFAULT_BASE_URL);
        }
        String url = value.strip().replaceAll("/+$", "");
        URI uri = parse(url);
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme()) && LOCAL_HOSTS.contains(uri.getHost());
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null) {
            throw new IllegalArgumentException(ENVIRONMENT_VARIABLE + " não é um endereço válido: " + url);
        }
        if (!secure && !localHttp) {
            throw new IllegalArgumentException(ENVIRONMENT_VARIABLE + " precisa usar HTTPS fora da máquina local: " + url);
        }
        return new ApiConfig(url);
    }

    private static URI parse(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ENVIRONMENT_VARIABLE + " não é um endereço válido: " + url, e);
        }
    }
}
