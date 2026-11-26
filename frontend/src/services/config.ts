/** APIs do Catálogo acessadas pelo frontend. */
export type Servico = 'restaurantes' | 'cardapio' | 'busca';

export const NOMES_DOS_SERVICOS: Record<Servico, string> = {
  restaurantes: 'Restaurantes',
  cardapio: 'Cardápio',
  busca: 'Busca',
};

const VARIAVEIS_DOS_SERVICOS: Record<Servico, keyof ImportMetaEnv> = {
  restaurantes: 'VITE_API_RESTAURANTES_URL',
  cardapio: 'VITE_API_CARDAPIO_URL',
  busca: 'VITE_API_BUSCA_URL',
};

/** Origem (ex.: `http://localhost:8004`) do serviço, lida das variáveis `VITE_API_*` gravadas no build. */
export function origemDoServico(servico: Servico): string {
  const variavel = VARIAVEIS_DOS_SERVICOS[servico];
  const origem = import.meta.env[variavel]?.trim();
  if (!origem) {
    throw new Error(`Variável ${variavel} não configurada. Veja frontend/.env.example.`);
  }
  return origem.replace(/\/+$/, '');
}
