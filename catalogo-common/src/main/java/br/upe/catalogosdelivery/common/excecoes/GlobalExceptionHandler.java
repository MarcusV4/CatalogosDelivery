package br.upe.catalogosdelivery.common.excecoes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handler global dos serviços do Catálogo: toda resposta de erro sai no formato
 * da plataforma, {"erro": true, "mensagem": "..."}, com o código HTTP adequado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Recurso inexistente (ex.: RestauranteNaoEncontradoException) ou 404 de outro serviço. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResponse.de(e.getMessage()));
    }

    /** Outro serviço fora do ar, em timeout ou com erro 5xx. */
    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> indisponivel(ServicoIndisponivelException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErroResponse.de(e.getMessage()));
    }

    /** Outro serviço respondeu com um erro inesperado. */
    @ExceptionHandler(FalhaIntegracaoException.class)
    public ResponseEntity<ErroResponse> falhaIntegracao(FalhaIntegracaoException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErroResponse.de(e.getMessage()));
    }

    /** Erros lançados com status explícito (ex.: 400 de validação nos services). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> comStatus(ResponseStatusException e) {
        String mensagem = e.getReason() != null ? e.getReason() : "Requisição inválida.";
        return ResponseEntity.status(e.getStatusCode()).body(ErroResponse.de(mensagem));
    }
}
