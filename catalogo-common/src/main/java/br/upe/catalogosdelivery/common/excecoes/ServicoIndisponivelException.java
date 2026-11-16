package br.upe.catalogosdelivery.common.excecoes;

/** Outro serviço está fora do ar, demorou demais ou retornou erro 5xx: responde 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
