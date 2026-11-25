package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import jakarta.validation.constraints.*;

public record AtualizarRestauranteDTO(
        @NotBlank(message = "O nome do restaurante não pode ser vazio.")
        String restauranteNome,

        @NotBlank(message = "A descrição do restaurante não pode ser vazia.")
        String restauranteDesc,

        @NotBlank(message = "O logradouro não pode ser vazio.")
        String logradouro,

        @NotBlank(message = "O número do endereço não pode ser vazio.")
        String numero,

        // Opcional
        String complemento,

        @NotBlank(message = "O bairro não pode ser vazio.")
        String bairro,

        @NotBlank(message = "A cidade não pode ser vazia.")
        String cidade,

        @NotBlank(message = "O estado não pode ser vazio.")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "Informe uma sigla de estado válida, como PE.")
        String estado,

        @NotBlank(message = "O CEP não pode ser vazio.")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido. Use o formato 00000-000 ou 00000000.")
        String cep,

        @NotBlank(message = "O telefone não pode ser vazio.")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone inválido. Informe DDD e número, só dígitos, como 81999990000.")
        String restauranteTelefone,

        @NotNull(message = "A taxa de entrega deve ser informada.")
        @PositiveOrZero(message = "A taxa de entrega não pode ser negativa.")
        Long taxaEntrega,

        @NotNull(message = "O tempo mínimo estimado deve ser informado.")
        @Positive(message = "O tempo mínimo estimado deve ser maior que zero.")
        Integer tempoEstimadoMin,

        @NotNull(message = "O tempo máximo estimado deve ser informado.")
        @Positive(message = "O tempo máximo estimado deve ser maior que zero.")
        Integer tempoEstimadoMax,

        String urlImagem,

        @NotNull(message = "O ID do proprietário deve ser informado.")
        Long ownerUserId,

        @NotNull(message = "A categoria do restaurante deve ser informada.")
        CategoriaRestaurante categoriaRestaurante
) {}
