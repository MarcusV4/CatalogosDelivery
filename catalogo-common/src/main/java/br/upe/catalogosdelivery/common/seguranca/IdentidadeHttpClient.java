package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.FalhaIntegracaoException;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Valida o token chamando o serviço de Identidade (Equipe 1) por HTTP.
 * <p>
 * Contrato esperado (ajustável por propriedades enquanto a #20 não fecha o da turma):
 * {@code GET {catalogo.identidade.url}{catalogo.identidade.caminho-validacao}} com o header
 * {@code Authorization: Bearer <token>}, respondendo {@code 200 {"sub": 57, "role": "LOJISTA"}}
 * para token válido e {@code 401} para token inválido ou expirado.
 * <p>
 * Toda chamada é logada (sem o token). Se a Identidade cair, demorar mais que o timeout ou
 * responder 5xx, a requisição recebe 503 e o processo continua no ar.
 */
@Component
@Profile("!dev")
public class IdentidadeHttpClient implements IdentidadeClient {

    static final String MENSAGEM_INDISPONIVEL = "Serviço de identidade indisponível";
    static final String MENSAGEM_TOKEN_INVALIDO = "Token inválido ou expirado.";

    private static final Logger log = LoggerFactory.getLogger(IdentidadeHttpClient.class);

    private final String origem;
    private final String url;
    private final String caminho;
    private final RestClient restClient;

    @Autowired
    public IdentidadeHttpClient(
            @Value("${spring.application.name:catalogo}") String origem,
            @Value("${catalogo.identidade.url:http://localhost:8001}") String url,
            @Value("${catalogo.identidade.caminho-validacao:/api/auth/validar}") String caminho,
            @Value("${catalogo.identidade.timeout:3s}") Duration timeout) {
        this(origem, url, caminho, RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabricaComTimeout(timeout))
                .build());
    }

    IdentidadeHttpClient(String origem, String url, String caminho, RestClient restClient) {
        this.origem = origem;
        this.url = url;
        this.caminho = caminho;
        this.restClient = restClient;
    }

    @Override
    public UsuarioAutenticado validarToken(String token) {
        log.info("[{}] -> GET {}{} (validando token no serviço de Identidade)", origem, url, caminho);
        long inicio = System.currentTimeMillis();
        RespostaIdentidade resposta;
        try {
            resposta = restClient.get()
                    .uri(caminho)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(RespostaIdentidade.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.info("[{}] <- {} da Identidade: token recusado", origem, e.getStatusCode().value());
            throw new NaoAutenticadoException(MENSAGEM_TOKEN_INVALIDO);
        } catch (HttpServerErrorException e) {
            log.warn("[{}] <- {} da Identidade: serviço com erro interno", origem, e.getStatusCode().value());
            throw new ServicoIndisponivelException(MENSAGEM_INDISPONIVEL, e);
        } catch (ResourceAccessException e) {
            log.warn("[{}] xx serviço de Identidade não respondeu ({})", origem, e.getMessage());
            throw new ServicoIndisponivelException(MENSAGEM_INDISPONIVEL, e);
        } catch (HttpClientErrorException e) {
            log.warn("[{}] <- {} inesperado da Identidade", origem, e.getStatusCode().value());
            throw new FalhaIntegracaoException(
                    "O serviço de Identidade retornou um erro inesperado (" + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            log.warn("[{}] xx falha ao ler resposta da Identidade ({})", origem, e.getMessage());
            throw new FalhaIntegracaoException("Resposta inválida do serviço de Identidade.", e);
        }

        UsuarioAutenticado usuario = converter(resposta);
        log.info("[{}] <- 200 da Identidade em {} ms: usuário {} ({})", origem,
                System.currentTimeMillis() - inicio, usuario.id(), usuario.role());
        return usuario;
    }

    private UsuarioAutenticado converter(RespostaIdentidade resposta) {
        if (resposta == null || resposta.sub() == null || resposta.sub().isBlank()) {
            log.warn("[{}] xx resposta da Identidade sem o identificador do usuário (sub)", origem);
            throw new FalhaIntegracaoException("Resposta inválida do serviço de Identidade.", null);
        }
        try {
            return new UsuarioAutenticado(Long.valueOf(resposta.sub().trim()), resposta.role());
        } catch (NumberFormatException e) {
            log.warn("[{}] xx sub não numérico na resposta da Identidade: {}", origem, resposta.sub());
            throw new FalhaIntegracaoException("Resposta inválida do serviço de Identidade.", e);
        }
    }

    private static SimpleClientHttpRequestFactory fabricaComTimeout(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }

    /** Claims devolvidas pela Identidade. Aceita {@code id}/{@code userId} como sinônimos de {@code sub}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaIdentidade(@JsonAlias({"id", "userId"}) String sub, String role) {
    }
}
