package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.FalhaIntegracaoException;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Segunda etapa da validação (INT-05 do contrato): pergunta ao serviço de Autorização/RBAC se o
 * token continua válido. O RBAC consulta a introspecção do Auth, então aqui se detecta o que a
 * validação local não vê: token revogado (logout) e papel alterado depois da emissão.
 * <p>
 * Resposta: 200 com o papel atual; 401/403 → token recusado; queda, timeout ou 5xx → 503
 * (falha fechada: sem confirmação, a escrita não acontece).
 */
public class ValidacaoOnlineRbac {

    private static final Logger log = LoggerFactory.getLogger(ValidacaoOnlineRbac.class);
    static final String CAMINHO = "/v1/tokens/validar";
    private static final String INDISPONIVEL =
            "O serviço de autorização está indisponível no momento. Tente novamente mais tarde.";

    private final RestClient restClient;
    private final String origem;

    ValidacaoOnlineRbac(RestClient restClient, String origem) {
        this.restClient = restClient;
        this.origem = origem;
    }

    public static ValidacaoOnlineRbac criar(IdentidadeProperties config, String origem) {
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(config.timeout());
        http.setReadTimeout(config.timeout());
        RestClient restClient = RestClient.builder()
                .baseUrl(config.rbacUrl().trim().replaceAll("/+$", ""))
                .requestFactory(http)
                .build();
        return new ValidacaoOnlineRbac(restClient, origem);
    }

    /** Resultado da consulta: o papel atual e, se o RBAC informar, a conta. */
    public record Resultado(String contaId, String papel) {
    }

    public Resultado validar(String accessToken) {
        long inicio = System.currentTimeMillis();
        log.info("[{}] -> POST {} (validando o token no RBAC)", origem, CAMINHO);
        Map<String, Object> corpo;
        try {
            corpo = restClient.post()
                    .uri(CAMINHO)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.info("[{}] <- {} do RBAC: token inválido, expirado, revogado ou com papel desatualizado",
                    origem, e.getStatusCode().value());
            throw new NaoAutenticadoException("Sua sessão expirou ou foi encerrada. Entre novamente.", e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("[{}] xx RBAC indisponível ({})", origem, e.getMessage());
            throw new ServicoIndisponivelException(INDISPONIVEL, e);
        } catch (HttpClientErrorException e) {
            log.warn("[{}] <- {} inesperado do RBAC", origem, e.getStatusCode().value());
            throw new FalhaIntegracaoException(
                    "O serviço de autorização retornou um erro inesperado (" + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            log.warn("[{}] xx resposta ilegível do RBAC ({})", origem, e.getMessage());
            throw new FalhaIntegracaoException("Resposta inválida do serviço de autorização.", e);
        }

        if (corpo == null) {
            throw new FalhaIntegracaoException("O serviço de autorização respondeu sem corpo.", null);
        }
        // O RBAC da Identidade responde "ativo"; "active" é o nome da introspecção OAuth (RFC 7662).
        if (Boolean.FALSE.equals(corpo.get("ativo")) || Boolean.FALSE.equals(corpo.get("active"))) {
            log.info("[{}] <- RBAC informou token inativo", origem);
            throw new NaoAutenticadoException("Sua sessão expirou ou foi encerrada. Entre novamente.");
        }
        String papel = texto(corpo, "role", "papel");
        if (papel == null || papel.isBlank()) {
            log.warn("[{}] <- resposta do RBAC sem o papel da conta", origem);
            throw new FalhaIntegracaoException("O serviço de autorização não informou o papel da conta.", null);
        }
        log.info("[{}] <- 200 do RBAC em {} ms: papel {}", origem, System.currentTimeMillis() - inicio, papel);
        return new Resultado(texto(corpo, "sub", "conta_id", "contaId"), papel);
    }

    /** O contrato não fixa os nomes dos campos da resposta; aceita os usados pela Identidade. */
    private static String texto(Map<String, Object> corpo, String... nomes) {
        for (String nome : nomes) {
            Object valor = corpo.get(nome);
            if (valor != null) {
                return String.valueOf(valor);
            }
        }
        return null;
    }
}
