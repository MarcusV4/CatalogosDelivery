package br.upe.catalogosdelivery.common.http;

import br.upe.catalogosdelivery.common.excecoes.FalhaIntegracaoException;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Cliente HTTP usado por um serviço do Catálogo para chamar outro.
 * <p>
 * Toda chamada é logada (é a prova da integração nos logs do terminal) e as falhas
 * viram exceções tratadas: 404 → {@link RecursoNaoEncontradoException}; queda, timeout
 * ou 5xx → {@link ServicoIndisponivelException}; demais erros → {@link FalhaIntegracaoException}.
 */
public class ClienteHttp {

    private static final Logger log = LoggerFactory.getLogger(ClienteHttp.class);

    private final String origem;
    private final String destino;
    private final String baseUrl;
    private final RestClient restClient;

    public ClienteHttp(String origem, String destino, String baseUrl, RestClient restClient) {
        this.origem = origem;
        this.destino = destino;
        this.baseUrl = baseUrl;
        this.restClient = restClient;
    }

    public static ClienteHttp criar(String origem, String destino, String baseUrl, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        return new ClienteHttp(origem, destino, baseUrl, restClient);
    }

    public <T> T get(String caminho, Class<T> tipo, String mensagemNaoEncontrado, Object... variaveis) {
        return executar(caminho, mensagemNaoEncontrado, variaveis,
                () -> restClient.get().uri(caminho, variaveis).retrieve().body(tipo));
    }

    public <T> T get(String caminho, ParameterizedTypeReference<T> tipo, String mensagemNaoEncontrado, Object... variaveis) {
        return executar(caminho, mensagemNaoEncontrado, variaveis,
                () -> restClient.get().uri(caminho, variaveis).retrieve().body(tipo));
    }

    private <T> T executar(String caminho, String mensagemNaoEncontrado, Object[] variaveis, Supplier<T> chamada) {
        String url = baseUrl + expandir(caminho, variaveis);
        log.info("[{}] -> GET {} (consultando o serviço de {})", origem, url, destino);
        long inicio = System.currentTimeMillis();
        try {
            T resposta = chamada.get();
            log.info("[{}] <- 200 de {} em {} ms", origem, destino, System.currentTimeMillis() - inicio);
            return resposta;
        } catch (HttpClientErrorException.NotFound e) {
            log.info("[{}] <- 404 de {}: {}", origem, destino, mensagemNaoEncontrado);
            throw new RecursoNaoEncontradoException(mensagemNaoEncontrado);
        } catch (HttpServerErrorException e) {
            log.warn("[{}] <- {} de {}: serviço com erro interno", origem, e.getStatusCode().value(), destino);
            throw new ServicoIndisponivelException(indisponivel(), e);
        } catch (HttpClientErrorException e) {
            log.warn("[{}] <- {} inesperado de {}", origem, e.getStatusCode().value(), destino);
            throw new FalhaIntegracaoException(
                    "O serviço de " + destino + " retornou um erro inesperado (" + e.getStatusCode().value() + ").", e);
        } catch (ResourceAccessException e) {
            log.warn("[{}] xx serviço de {} não respondeu ({})", origem, destino, e.getMessage());
            throw new ServicoIndisponivelException(indisponivel(), e);
        } catch (RestClientException e) {
            log.warn("[{}] xx falha ao ler resposta de {} ({})", origem, destino, e.getMessage());
            throw new FalhaIntegracaoException("Resposta inválida do serviço de " + destino + ".", e);
        }
    }

    private String indisponivel() {
        return "O serviço de " + destino + " está indisponível no momento. Tente novamente mais tarde.";
    }

    private static String expandir(String caminho, Object[] variaveis) {
        String resultado = caminho;
        for (Object variavel : variaveis) {
            resultado = resultado.replaceFirst("\\{[^}]*}", java.util.regex.Matcher.quoteReplacement(String.valueOf(variavel)));
        }
        return resultado;
    }
}
