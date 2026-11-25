import type { CategoriaRestaurante, StatusRestaurante } from '../services/restaurantes';

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

export const ROTULOS_STATUS_RESTAURANTE: Record<StatusRestaurante, string> = {
  ABERTO: 'Aberto',
  FECHADO: 'Fechado',
  INDISPONIVEL: 'Indisponível',
};
