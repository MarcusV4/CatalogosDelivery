/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_RESTAURANTES_URL?: string;
  readonly VITE_API_CARDAPIO_URL?: string;
  readonly VITE_API_BUSCA_URL?: string;
  /** Emissor (issuer) do Authorization Server. */
  readonly VITE_API_IDENTIDADE_URL?: string;
  readonly VITE_IDENTIDADE_CLIENT_ID?: string;
  readonly VITE_IDENTIDADE_REDIRECT_URI?: string;
  readonly VITE_IDENTIDADE_SCOPES?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
