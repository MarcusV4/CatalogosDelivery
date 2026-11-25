/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_RESTAURANTES_URL?: string;
  readonly VITE_API_CARDAPIO_URL?: string;
  readonly VITE_API_BUSCA_URL?: string;
  /** Reservada para o login com a Identidade (#31). */
  readonly VITE_API_IDENTIDADE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
