package br.upe.catalogosdelivery.common.excecoes;

/** Outro serviço respondeu com um erro inesperado (4xx diferente de 404): responde 502. */
public class FalhaIntegracaoException extends RuntimeException {

    public FalhaIntegracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
