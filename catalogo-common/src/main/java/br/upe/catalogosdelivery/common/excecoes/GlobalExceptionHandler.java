package br.upe.catalogosdelivery.common.excecoes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Handler global dos serviços do Catálogo: toda resposta de erro sai no formato
 * da plataforma, {"erro": true, "mensagem": "..."}, com o código HTTP adequado.
 * <p>
 * Estende {@link ResponseEntityExceptionHandler} para que os erros do próprio Spring MVC
 * (JSON inválido, tipo errado na URL, rota inexistente, método ou Content-Type não
 * suportados, falha de @Valid) também saiam nesse formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Recurso inexistente (ex.: RestauranteNaoEncontradoException) ou 404 de outro serviço. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResponse.de(e.getMessage()));
    }

    /** Token ausente ou inválido nas rotas de escrita. */
    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ErroResponse> naoAutenticado(NaoAutenticadoException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ErroResponse.de(e.getMessage()));
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

    /** Dados recusados pelo banco (ex.: texto maior que a coluna, campo obrigatório nulo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(DataIntegrityViolationException e) {
        log.warn("Dados recusados pelo banco: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResponse.de(
                "Dados inválidos: verifique os campos obrigatórios e o tamanho dos textos."));
    }

    /** Qualquer erro não previsto: registra tudo no log, mas não expõe detalhes na resposta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.de("Erro interno no servidor."));
    }

    /**
     * Ponto único por onde passam os erros tratados pelo ResponseEntityExceptionHandler
     * (inclusive ResponseStatusException): troca o corpo padrão pelo formato da plataforma.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return ResponseEntity.status(statusCode).headers(headers).body(ErroResponse.de(mensagemPara(ex, statusCode)));
    }

    private static String mensagemPara(Exception ex, HttpStatusCode statusCode) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (ex instanceof MethodArgumentNotValidException invalido) {
            return invalido.getBindingResult().getFieldErrors().stream()
                    .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Corpo da requisição inválido: verifique o JSON, os campos obrigatórios e os valores aceitos.";
        }
        if (ex instanceof MethodArgumentTypeMismatchException tipoErrado) {
            return "Valor inválido para '" + tipoErrado.getName() + "'.";
        }
        if (ex instanceof TypeMismatchException tipoErrado) {
            return "Valor inválido para '" + tipoErrado.getPropertyName() + "'.";
        }
        if (ex instanceof NoResourceFoundException) {
            return "Rota não encontrada.";
        }
        if (ex instanceof HttpRequestMethodNotSupportedException metodo) {
            return "Método " + metodo.getMethod() + " não suportado nesta rota.";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "Content-Type não suportado. Use application/json.";
        }
        return statusCode.is4xxClientError() ? "Requisição inválida." : "Erro interno no servidor.";
    }
}
