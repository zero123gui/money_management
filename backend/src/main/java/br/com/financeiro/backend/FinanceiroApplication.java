package br.com.financeiro.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API REST do Controle Financeiro Familiar.
 */
@SpringBootApplication
public class FinanceiroApplication {

    /**
     * Inicia a aplicação Spring Boot.
     *
     * @param args argumentos de linha de comando repassados ao Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(FinanceiroApplication.class, args);
    }
}
