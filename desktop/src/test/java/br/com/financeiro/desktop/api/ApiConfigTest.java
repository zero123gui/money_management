package br.com.financeiro.desktop.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApiConfigTest {

    @Test
    void usesLocalAddressWhenNotConfigured() {
        assertThat(ApiConfig.of(null).baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(ApiConfig.of("  ").baseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void removesTrailingSlash() {
        assertThat(ApiConfig.of("https://api.exemplo.com/").baseUrl()).isEqualTo("https://api.exemplo.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://api.exemplo.com", "http://localhost:9000", "http://127.0.0.1:8080", "http://[::1]:8080"})
    void acceptsHttpsAndLocalHttp(String url) {
        assertThat(ApiConfig.of(url).baseUrl()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://api.exemplo.com", "http://localhost.exemplo.com", "http://192.168.0.10:8080"})
    void refusesHttpOutsideTheLocalMachine(String url) {
        assertThatThrownBy(() -> ApiConfig.of(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "localhost:8080", "ftp://localhost", "isto não é url", "http://localhost@exemplo.com",
            "https://usuario:senha@exemplo.com", "https://exemplo.com?x=1"})
    void refusesInvalidAddresses(String url) {
        assertThatThrownBy(() -> ApiConfig.of(url)).isInstanceOf(IllegalArgumentException.class);
    }
}
