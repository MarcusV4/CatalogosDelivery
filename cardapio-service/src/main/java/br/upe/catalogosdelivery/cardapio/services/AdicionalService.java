package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.entities.Adicional;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalPatchDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalResponseDTO;
import br.upe.catalogosdelivery.cardapio.repositories.AdicionalRepo;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdicionalService {
    private final AdicionalRepo adicionalRepo;
    private final ProdutoService produtoService;

    @Transactional(readOnly = true)
    public List<AdicionalResponseDTO> listar(Long produtoId) {
        produtoService.obterProdutoAtivo(produtoId);
        return adicionalRepo.findByProduto_IdOrderById(produtoId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public AdicionalResponseDTO criar(Long produtoId, AdicionalRequestDTO request) {
        Produto produto = produtoService.obterProdutoAtivo(produtoId);
        produtoService.validarEscrita(produto);
        Adicional adicional = new Adicional();
        adicional.setProduto(produto);
        adicional.setNome(request.nome());
        adicional.setPreco(request.preco());
        adicional.setDisponivel(request.disponivel());
        return toDTO(adicionalRepo.save(adicional));
    }

    @Transactional
    public AdicionalResponseDTO atualizar(Long produtoId, Long adicionalId, AdicionalRequestDTO request) {
        Adicional adicional = obterAdicional(produtoId, adicionalId);
        produtoService.validarEscrita(adicional.getProduto());
        adicional.setNome(request.nome());
        adicional.setPreco(request.preco());
        adicional.setDisponivel(request.disponivel());
        return toDTO(adicionalRepo.save(adicional));
    }

    @Transactional
    public AdicionalResponseDTO atualizarParcialmente(Long produtoId, Long adicionalId, AdicionalPatchDTO request) {
        Adicional adicional = obterAdicional(produtoId, adicionalId);
        produtoService.validarEscrita(adicional.getProduto());
        if (request.nome() == null && request.preco() == null && request.disponivel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um campo para atualizar.");
        }
        if (request.nome() != null) adicional.setNome(request.nome());
        if (request.preco() != null) adicional.setPreco(request.preco());
        if (request.disponivel() != null) adicional.setDisponivel(request.disponivel());
        return toDTO(adicionalRepo.save(adicional));
    }

    @Transactional
    public AdicionalResponseDTO alterarDisponibilidade(Long produtoId, Long adicionalId, boolean disponivel) {
        Adicional adicional = obterAdicional(produtoId, adicionalId);
        produtoService.validarEscrita(adicional.getProduto());
        adicional.setDisponivel(disponivel);
        return toDTO(adicionalRepo.save(adicional));
    }

    @Transactional
    public void excluir(Long produtoId, Long adicionalId) {
        Adicional adicional = obterAdicional(produtoId, adicionalId);
        produtoService.validarEscrita(adicional.getProduto());
        adicionalRepo.delete(adicional);
    }

    private Adicional obterAdicional(Long produtoId, Long adicionalId) {
        produtoService.obterProdutoAtivo(produtoId);
        return adicionalRepo.findByIdAndProduto_Id(adicionalId, produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Adicional " + adicionalId + " não encontrado no produto " + produtoId + "."));
    }

    private AdicionalResponseDTO toDTO(Adicional adicional) {
        return new AdicionalResponseDTO(
                adicional.getId(), adicional.getNome(), adicional.getPreco(), adicional.isDisponivel());
    }
}
