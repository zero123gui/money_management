package br.com.financeiro.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de segurança da API: tudo exige token, exceto cadastro e login.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, LoginLimitProperties.class, SignupProperties.class})
public class SecurityConfig {

    /**
     * Define as rotas públicas e a validação do token. A API não guarda sessão
     * nem usa cookies, por isso a proteção contra CSRF não se aplica.
     *
     * @param http           construtor da configuração do Spring Security
     * @param errors         responde 401 e 403 no formato de erro da API
     * @param apiDocsEnabled se o Swagger está ligado; só então as rotas dele são públicas
     * @return a cadeia de filtros da API
     * @throws Exception se a configuração for inválida
     */
    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, SecurityErrorHandler errors,
            @Value("${springdoc.api-docs.enabled}") boolean apiDocsEnabled) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> {
                    requests.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/auth/cadastro", "/api/v1/auth/login").permitAll();
                    if (apiDocsEnabled) {
                        requests.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    requests.anyRequest().authenticated();
                })
                .oauth2ResourceServer(server -> server
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .build();
    }

    /**
     * Cria o codificador de senhas (RNF-01).
     *
     * @return o codificador BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cria o assinador de tokens.
     *
     * @param properties configuração do JWT
     * @return o codificador de tokens HS256
     */
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        SecretKey key = properties.signingKey();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * Cria o validador de tokens.
     *
     * @param properties configuração do JWT
     * @return o decodificador de tokens HS256
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.signingKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Sem a tolerância padrão de 60 segundos: quem emite e quem valida é o
        // mesmo servidor, e o token deve valer exatamente o tempo configurado.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO)));
        return decoder;
    }
}
