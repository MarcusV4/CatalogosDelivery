package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;


public record RestauranteDTO(

        @NotBlank(message = "O nome do restaurante não pode ser vazio.")
        String restauranteNome,

        @NotBlank(message = "A descrição do restaurante não pode ser vazia.")
        String restauranteDesc,

        @NotBlank(message = "O logradouro do restaurante não pode ser vazio.")
        String logradouro,

        @NotBlank(message = "O número do endereço do restaurante não pode ser vazio.")
        String numero,

        //Opicional
        String complemento,

        @NotBlank(message = "O bairro do endereço do restaurante não pode ser vazio.")
        String bairro,

        @NotBlank(message = "A cidade do endereço do restaurante não pode ser vazia.")
        String cidade,

        @NotBlank(message = "O Estado do endereço do restaurante não pode ser vazio.")
        String estado,

        @Pattern(
                regexp = "^\\d{5}-?\\d{3}$",
                message = "CEP inválido. Use o formato 00000-000 ou 00000000."
        )
        String cep,

        @NotBlank(message = "O telefone não pode ser vazio.")
        @Pattern(
                regexp = "^\\d{10,11}$",
                message = "Formado de telefone inválido. Use o formato 01234567891"
        )
        String restauranteTelefone,

        @Schema(description = "Taxa fixa informada pelo restaurante, em centavos; não é cálculo de frete", example = "550", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        Long taxaEntrega,

        @NotNull(message = "O tempo mínimo estimado deve ser informado.")
        @Positive(message = "A tempo mínimo estimado deve ser maior que zero.")
        Integer tempoEstimadoMin,

        @NotNull(message = "O tempo máximo estimado deve ser informado.")
        @Positive(message = "O tempo máximo estimado deve ser maior que zero.")
        Integer tempoEstimadoMax,

        String urlImagem,

        @NotNull(message = "O ID do proprietário deve ser informado.")
        Long ownerUserId,

        StatusRestaurante statusRestaurante,
        CategoriaRestaurante categoriaRestaurante
) {}
