package br.upe.catalogosdelivery.common.excecoes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as exceções de integração no formato de erro padrão, para que a queda
 * de um serviço vire uma resposta amigável em vez de derrubar quem depende dele.
 * O tratamento das demais exceções fica com a issue #16.
 */
@RestControllerAdvice
public class IntegracaoExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResponse.de(e.getMessage()));
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> indisponivel(ServicoIndisponivelException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErroResponse.de(e.getMessage()));
    }

    @ExceptionHandler(FalhaIntegracaoException.class)
    public ResponseEntity<ErroResponse> falhaIntegracao(FalhaIntegracaoException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErroResponse.de(e.getMessage()));
    }
}
