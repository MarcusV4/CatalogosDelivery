package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.FalhaIntegracaoException;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ValidacaoOnlineRbacTest {

    private static final String URL = "http://rbac:8003/v1/tokens/validar";
    private MockRestServiceServer rbac;
    private ValidacaoOnlineRbac validacao;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://rbac:8003");
        rbac = MockRestServiceServer.bindTo(builder).build();
        validacao = new ValidacaoOnlineRbac(builder.build(), "teste");
    }

    @Test
    void enviaOTokenNoAuthorizationEDevolveOPapelAtual() {
        rbac.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-do-usuario"))
                .andRespond(withSuccess("{\"sub\":\"" + TokensDeTeste.CONTA + "\",\"role\":\"LOJA\"}",
                        MediaType.APPLICATION_JSON));

        ValidacaoOnlineRbac.Resultado resultado = validacao.validar("token-do-usuario");

        assertEquals(TokensDeTeste.CONTA, resultado.contaId());
        assertEquals("LOJA", resultado.papel());
        rbac.verify();
    }

    @Test
    void aceitaOsNomesEmPortuguesDosCampos() {
        rbac.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"conta_id\":\"" + TokensDeTeste.CONTA + "\",\"papel\":\"CLIENTE\"}", MediaType.APPLICATION_JSON));

        assertEquals("CLIENTE", validacao.validar("t").papel());
    }

    @Test
    void tokenRecusadoPeloRbacViraNaoAutenticado() {
        rbac.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(NaoAutenticadoException.class, () -> validacao.validar("revogado"));
    }

    @Test
    void tokenInativoViraNaoAutenticado() {
        rbac.expect(requestTo(URL)).andRespond(withSuccess("{\"active\":false}", MediaType.APPLICATION_JSON));

        assertThrows(NaoAutenticadoException.class, () -> validacao.validar("t"));
    }

    /** Formato real do RBAC da Identidade: {"ativo", "conta_id", "role", "jti", "permissoes"}. */
    @Test
    void tokenInativoNoFormatoDoRbacViraNaoAutenticado() {
        rbac.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"ativo\":false,\"conta_id\":\"" + TokensDeTeste.CONTA + "\",\"role\":\"LOJA\"}",
                MediaType.APPLICATION_JSON));

        assertThrows(NaoAutenticadoException.class, () -> validacao.validar("t"));
    }

    @Test
    void tokenAtivoNoFormatoDoRbacDevolveContaEPapel() {
        rbac.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"ativo\":true,\"conta_id\":\"" + TokensDeTeste.CONTA + "\",\"role\":\"LOJA\","
                        + "\"jti\":\"abc\",\"permissoes\":[]}",
                MediaType.APPLICATION_JSON));

        ValidacaoOnlineRbac.Resultado resultado = validacao.validar("t");

        assertEquals(TokensDeTeste.CONTA, resultado.contaId());
        assertEquals("LOJA", resultado.papel());
    }

    @Test
    void rbacForaDoArViraIndisponivel() {
        rbac.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(ServicoIndisponivelException.class, () -> validacao.validar("t"));
    }

    @Test
    void respostaSemPapelEhFalhaDeIntegracao() {
        rbac.expect(requestTo(URL)).andRespond(withSuccess("{\"sub\":\"x\"}", MediaType.APPLICATION_JSON));

        assertThrows(FalhaIntegracaoException.class, () -> validacao.validar("t"));
    }
}
