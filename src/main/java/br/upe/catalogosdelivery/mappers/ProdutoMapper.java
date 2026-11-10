package br.upe.catalogosdelivery.mappers;

import br.upe.catalogosdelivery.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.entities.Produto;
import org.springframework.stereotype.Component;

@Component
public class ProdutoMapper {
    public ProdutoResponseDTO toDTO(Produto produto) {
        return new ProdutoResponseDTO(
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.isDisponivel(),
                produto.getCategoria()
        );
    }
}
