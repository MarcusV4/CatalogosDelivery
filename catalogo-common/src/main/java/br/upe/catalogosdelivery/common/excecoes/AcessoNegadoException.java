package br.upe.catalogosdelivery.common.excecoes;

/**
 * Usuário autenticado sem permissão para a operação (por exemplo, conta que não é de loja
 * ou que não é dona do restaurante): responde 403.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
