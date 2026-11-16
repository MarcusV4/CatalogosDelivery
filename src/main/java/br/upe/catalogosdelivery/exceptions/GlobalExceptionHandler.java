
package br.upe.catalogosdelivery.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestauranteNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDTO> tratarRestauranteNaoEncontrado(
            RestauranteNaoEncontradoException ex) {

        ErroResponseDTO erro = new ErroResponseDTO(
                true,
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }
}
