import type { CategoriaProduto } from '../services/cardapio';
import type { CategoriaRestaurante, DiaSemana, StatusRestaurante } from '../services/restaurantes';

/** Nomes dos enums do back para exibição (o `Record` obriga a cobrir todos os valores). */
export const ROTULOS_CATEGORIA_RESTAURANTE: Record<CategoriaRestaurante, string> = {
  HAMBURGUERIA: 'Hamburgueria',
  PIZZARIA: 'Pizzaria',
  COMIDA_BRASILEIRA: 'Comida brasileira',
  COMIDA_REGIONAL: 'Comida regional',
  COMIDA_JAPONESA: 'Comida japonesa',
  COMIDA_CHINESA: 'Comida chinesa',
  COMIDA_ITALIANA: 'Comida italiana',
  COMIDA_MEXICANA: 'Comida mexicana',
  COMIDA_ARABE: 'Comida árabe',
  LANCHONETE: 'Lanchonete',
  MARMITARIA: 'Marmitaria',
  CHURRASCARIA: 'Churrascaria',
  COMIDA_SAUDAVEL: 'Comida saudável',
  COMIDA_VEGETARIANA_E_VEGANA: 'Vegetariana e vegana',
  PADARIA: 'Padaria',
  DOCERIA: 'Doceria',
  SORVETERIA: 'Sorveteria',
  ACAI: 'Açaí',
  OUTROS: 'Outros',
};

/** Nomes curtos dos cartões de categoria da Home, como no protótipo ("Brasileira", "Japonesa"…). */
export const ROTULOS_CATEGORIA_RESTAURANTE_CURTOS: Record<CategoriaRestaurante, string> = {
  HAMBURGUERIA: 'Hamburgueria',
  PIZZARIA: 'Pizzaria',
  COMIDA_BRASILEIRA: 'Brasileira',
  COMIDA_REGIONAL: 'Regional',
  COMIDA_JAPONESA: 'Japonesa',
  COMIDA_CHINESA: 'Chinesa',
  COMIDA_ITALIANA: 'Italiana',
  COMIDA_MEXICANA: 'Mexicana',
  COMIDA_ARABE: 'Árabe',
  LANCHONETE: 'Lanchonete',
  MARMITARIA: 'Marmitaria',
  CHURRASCARIA: 'Churrascaria',
  COMIDA_SAUDAVEL: 'Saudável',
  COMIDA_VEGETARIANA_E_VEGANA: 'Vegetariana e vegana',
  PADARIA: 'Padaria',
  DOCERIA: 'Doceria',
  SORVETERIA: 'Sorveteria',
  ACAI: 'Açaí',
  OUTROS: 'Outros',
};

export const ROTULOS_STATUS_RESTAURANTE: Record<StatusRestaurante, string> = {
  ABERTO: 'Aberto',
  FECHADO: 'Fechado',
  INDISPONIVEL: 'Indisponível',
};

/** Títulos das seções do cardápio (no plural). */
export const ROTULOS_CATEGORIA_PRODUTO: Record<CategoriaProduto, string> = {
  LANCHE: 'Lanches',
  PRATO_PRINCIPAL: 'Pratos principais',
  PIZZA: 'Pizzas',
  ENTRADA: 'Entradas',
  SOBREMESA: 'Sobremesas',
  BEBIDA: 'Bebidas',
  VEGETARIANO: 'Vegetarianos',
  SALGADO: 'Salgados',
  ORIENTAL: 'Orientais',
};

export const ROTULOS_DIA_SEMANA: Record<DiaSemana, string> = {
  DOMINGO: 'Domingo',
  SEGUNDA: 'Segunda',
  TERCA: 'Terça',
  QUARTA: 'Quarta',
  QUINTA: 'Quinta',
  SEXTA: 'Sexta',
  SABADO: 'Sábado',
};
