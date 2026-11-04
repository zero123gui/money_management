package br.com.financeiro.desktop.api;

import br.com.financeiro.desktop.api.dto.CadastroRequest;
import br.com.financeiro.desktop.api.dto.CategoriaResponse;
import br.com.financeiro.desktop.api.dto.CategoriaTotalResponse;
import br.com.financeiro.desktop.api.dto.LancamentoRequest;
import br.com.financeiro.desktop.api.dto.LancamentoResponse;
import br.com.financeiro.desktop.api.dto.PageResponse;
import br.com.financeiro.desktop.api.dto.LoginRequest;
import br.com.financeiro.desktop.api.dto.ProblemResponse;
import br.com.financeiro.desktop.api.dto.SaldoResponse;
import br.com.financeiro.desktop.api.dto.TokenResponse;
import br.com.financeiro.desktop.api.dto.UsuarioResponse;
import br.com.financeiro.desktop.session.Session;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente da API REST. Os métodos bloqueiam até a resposta, portanto devem
 * rodar fora da thread do JavaFX.
 */
public class ApiClient {

    private static final String API_PREFIX = "/api/v1";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String UNAVAILABLE_MESSAGE =
            "Não foi possível conectar à API. Confira a internet e tente novamente.";
    private static final String UNEXPECTED_MESSAGE = "A API respondeu de forma inesperada. Tente novamente.";

    private final String baseUrl;
    private final Session session;
    private final HttpClient http;
    private final JsonMapper json;

    /**
     * Cria o cliente.
     *
     * @param config  endereço da API
     * @param session sessão de onde vem o token
     */
    public ApiClient(ApiConfig config, Session session) {
        this.baseUrl = config.baseUrl();
        this.session = session;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        // A API pode ganhar campos novos sem quebrar um desktop já instalado.
        this.json = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    /**
     * Cadastra um usuário (RF-01).
     *
     * @param request dados do cadastro
     * @return usuário criado
     * @throws ApiException se a API recusar ou não responder
     */
    public UsuarioResponse register(CadastroRequest request) {
        return send(publicPost("/auth/cadastro", request), UsuarioResponse.class);
    }

    /**
     * Faz o login (RF-02). Não altera a sessão: quem chama decide quando abri-la.
     *
     * @param request e-mail e senha
     * @return token emitido
     * @throws ApiException se as credenciais forem recusadas ou a API não responder
     */
    public TokenResponse login(LoginRequest request) {
        return send(publicPost("/auth/login", request), TokenResponse.class);
    }

    /**
     * Busca o usuário dono do token.
     *
     * @return usuário logado
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public UsuarioResponse currentUser() {
        HttpRequest.Builder request = request("/usuarios/me").GET();
        return sendAuthenticated(request, typeOf(UsuarioResponse.class));
    }

    /**
     * Lista as categorias padrão e as do usuário, inclusive as desativadas (RF-14, RF-15).
     *
     * @return categorias, ordenadas por nome
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public List<CategoriaResponse> listCategorias() {
        JavaType type = json.getTypeFactory().constructCollectionType(List.class, CategoriaResponse.class);
        return sendAuthenticated(request("/categorias?incluirInativas=true").GET(), type);
    }

    /**
     * Lista os lançamentos ativos do usuário (RF-10).
     *
     * @param filter  filtros da consulta
     * @param page    número da página, começando em 0
     * @param perPage quantidade de itens por página
     * @return página de lançamentos, do mais recente para o mais antigo
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public PageResponse<LancamentoResponse> listLancamentos(LancamentoFilter filter, int page, int perPage) {
        List<String> query = new ArrayList<>();
        // Datas, enums e números não têm caracteres que precisem de codificação na URL.
        if (filter.inicio() != null) {
            query.add("inicio=" + filter.inicio());
        }
        if (filter.fim() != null) {
            query.add("fim=" + filter.fim());
        }
        if (filter.tipo() != null) {
            query.add("tipo=" + filter.tipo());
        }
        if (filter.categoriaId() != null) {
            query.add("categoriaId=" + filter.categoriaId());
        }
        query.add("pagina=" + page);
        query.add("tamanho=" + perPage);

        JavaType type = json.getTypeFactory().constructParametricType(PageResponse.class, LancamentoResponse.class);
        return sendAuthenticated(request("/lancamentos?" + String.join("&", query)).GET(), type);
    }

    /**
     * Cria um lançamento (RF-06, RF-07).
     *
     * @param request dados do lançamento
     * @return lançamento criado
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public LancamentoResponse createLancamento(LancamentoRequest request) {
        HttpRequest.Builder builder = request("/lancamentos")
                .header("Content-Type", "application/json")
                .POST(jsonBody(request));
        return sendAuthenticated(builder, typeOf(LancamentoResponse.class));
    }

    /**
     * Altera um lançamento (RF-08).
     *
     * @param id      identificador do lançamento
     * @param request novos dados
     * @return lançamento alterado
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public LancamentoResponse updateLancamento(long id, LancamentoRequest request) {
        HttpRequest.Builder builder = request("/lancamentos/" + id)
                .header("Content-Type", "application/json")
                .PUT(jsonBody(request));
        return sendAuthenticated(builder, typeOf(LancamentoResponse.class));
    }

    /**
     * Exclui um lançamento, de forma lógica (RF-09).
     *
     * @param id identificador do lançamento
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public void deleteLancamento(long id) {
        sendAuthenticated(request("/lancamentos/" + id).DELETE(), typeOf(Void.class));
    }

    /**
     * Busca o saldo de um período (RF-16).
     *
     * @param start primeira data; sem ela, a API responde com o campo faltando
     * @param end   última data; sem ela, a API responde com o campo faltando
     * @return totais e saldo do período
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public SaldoResponse getSaldo(LocalDate start, LocalDate end) {
        return sendAuthenticated(request("/relatorios/saldo?" + periodQuery(start, end)).GET(), typeOf(SaldoResponse.class));
    }

    /**
     * Busca o total por categoria de um período (RF-17).
     *
     * @param start primeira data; sem ela, a API responde com o campo faltando
     * @param end   última data; sem ela, a API responde com o campo faltando
     * @return totais, do maior para o menor
     * @throws SessionExpiredException se não houver token ou a API recusá-lo
     * @throws ApiException            se a API recusar ou não responder
     */
    public List<CategoriaTotalResponse> listCategoriaTotals(LocalDate start, LocalDate end) {
        JavaType type = json.getTypeFactory().constructCollectionType(List.class, CategoriaTotalResponse.class);
        return sendAuthenticated(request("/relatorios/categorias?" + periodQuery(start, end)).GET(), type);
    }

    private static String periodQuery(LocalDate start, LocalDate end) {
        List<String> query = new ArrayList<>();
        if (start != null) {
            query.add("inicio=" + start);
        }
        if (end != null) {
            query.add("fim=" + end);
        }
        return String.join("&", query);
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        return HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
    }

    private JavaType typeOf(Class<?> type) {
        return json.getTypeFactory().constructType(type);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + API_PREFIX + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
    }

    private HttpRequest.Builder publicPost(String path, Object body) {
        return request(path)
                .header("Content-Type", "application/json")
                .POST(jsonBody(body));
    }

    private <T> T sendAuthenticated(HttpRequest.Builder request, JavaType responseType) {
        String token = session.token();
        if (token == null) {
            throw new SessionExpiredException();
        }
        request.header("Authorization", "Bearer " + token);
        try {
            return send(request, responseType);
        } catch (ApiException e) {
            // Nas rotas públicas, 401 é credencial errada; aqui, é o token que não vale mais.
            if (e.getStatus() == 401) {
                throw new SessionExpiredException();
            }
            throw e;
        }
    }

    private <T> T send(HttpRequest.Builder request, Class<T> responseType) {
        return send(request, typeOf(responseType));
    }

    private <T> T send(HttpRequest.Builder request, JavaType responseType) {
        HttpResponse<String> response = execute(request.build());
        if (response.statusCode() / 100 != 2) {
            throw toException(response);
        }
        // Respostas 204 não têm corpo para ler.
        if (responseType.hasRawClass(Void.class)) {
            return null;
        }
        try {
            return json.readValue(response.body(), responseType);
        } catch (JacksonException e) {
            throw new ApiException(response.statusCode(), UNEXPECTED_MESSAGE, List.of());
        }
    }

    private HttpResponse<String> execute(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException(ApiException.NO_RESPONSE, UNAVAILABLE_MESSAGE, List.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiException.NO_RESPONSE, UNAVAILABLE_MESSAGE, List.of());
        }
    }

    private ApiException toException(HttpResponse<String> response) {
        try {
            ProblemResponse problem = json.readValue(response.body(), ProblemResponse.class);
            String message = problem.detail() != null ? problem.detail() : problem.title();
            return new ApiException(
                    response.statusCode(),
                    message != null ? message : UNEXPECTED_MESSAGE,
                    problem.erros() != null ? problem.erros() : List.of());
        } catch (JacksonException e) {
            // Um proxy ou balanceador pode responder com HTML em vez do formato da API.
            return new ApiException(response.statusCode(), UNEXPECTED_MESSAGE, List.of());
        }
    }
}
