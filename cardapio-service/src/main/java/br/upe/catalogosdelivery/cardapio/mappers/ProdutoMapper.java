package br.upe.catalogosdelivery.cardapio.mappers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import org.springframework.stereotype.Component;

@Component
public class ProdutoMapper {
    public ProdutoResponseDTO toDTO(Produto produto) {
        return new ProdutoResponseDTO(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.isDisponivel(),
                produto.getCategoria(),
                produto.getCardapio().getRestauranteId(),
                produto.getUrlImagem(),
                produto.getAdicionais().stream()
                        .map(a -> new AdicionalResponseDTO(a.getId(), a.getNome(), a.getPreco(), a.isDisponivel()))
                        .toList()
        );
    }

    public Produto toEntity(ProdutoRequestDTO request) {
        Produto produto = new Produto();
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setDisponivel(request.disponivel());
        produto.setUrlImagem(request.urlImagem());
        produto.setCategoria(request.categoria());
        return produto;
    }
}
