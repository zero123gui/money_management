package br.com.financeiro.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Descrição geral da especificação OpenAPI. O contrato oficial continua sendo
 * {@code docs/API.md}; a especificação serve para testar as rotas no Swagger.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER = "bearer";

    /**
     * Descreve a API e exige o token por padrão, o que faz o Swagger mostrar o
     * botão "Authorize". As rotas públicas abrem essa exigência em seu controller.
     *
     * @return a descrição da API
     */
    @Bean
    public OpenAPI financeiroOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Controle Financeiro Familiar").version("v1"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
