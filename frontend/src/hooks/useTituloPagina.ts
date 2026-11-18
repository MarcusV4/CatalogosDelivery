import { useEffect } from 'react';

const NOME_DO_SITE = 'Zeppel';

/** Atualiza o título da aba (`"Entrar · Zeppel"`); ajuda leitores de tela a perceber a troca de página. */
export function useTituloPagina(titulo?: string): void {
  useEffect(() => {
    document.title = titulo ? `${titulo} · ${NOME_DO_SITE}` : NOME_DO_SITE;
  }, [titulo]);
}
