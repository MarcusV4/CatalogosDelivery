import { useRef, type KeyboardEvent, type ReactNode } from 'react';
import type { AbaBusca } from '../../../utils/filtrosBusca';

const ID_PAINEL = 'painel-busca';

const ABAS: readonly { aba: AbaBusca; rotulo: string }[] = [
  { aba: 'restaurantes', rotulo: 'Restaurantes' },
  { aba: 'pratos', rotulo: 'Pratos' },
];

const idDaAba = (aba: AbaBusca) => `aba-${aba}`;

interface AbasBuscaProps {
  aba: AbaBusca;
  aoTrocar: (aba: AbaBusca) => void;
  /** Conteúdo do painel da aba ativa. */
  children: ReactNode;
}

/** Abas "Restaurantes | Pratos" no padrão ARIA (setas, Home e End trocam de aba) e o painel. */
export function AbasBusca({ aba, aoTrocar, children }: AbasBuscaProps) {
  const botoes = useRef<(HTMLButtonElement | null)[]>([]);

  const irPara = (indice: number) => {
    const destino = ABAS[(indice + ABAS.length) % ABAS.length];
    if (destino) {
      aoTrocar(destino.aba);
      botoes.current[ABAS.indexOf(destino)]?.focus();
    }
  };

  const aoTeclar = (evento: KeyboardEvent, indice: number) => {
    const destinos: Record<string, number> = {
      ArrowRight: indice + 1,
      ArrowLeft: indice - 1,
      Home: 0,
      End: ABAS.length - 1,
    };
    const destino = destinos[evento.key];
    if (destino !== undefined) {
      evento.preventDefault();
      irPara(destino);
    }
  };

  return (
    <>
      <div className="abas" role="tablist" aria-label="Tipo de busca">
        {ABAS.map((item, indice) => {
          const ativa = item.aba === aba;
          return (
            <button
              key={item.aba}
              ref={(botao) => {
                botoes.current[indice] = botao;
              }}
              type="button"
              role="tab"
              id={idDaAba(item.aba)}
              className="abas__aba"
              aria-selected={ativa}
              aria-controls={ID_PAINEL}
              tabIndex={ativa ? 0 : -1}
              onClick={() => aoTrocar(item.aba)}
              onKeyDown={(evento) => aoTeclar(evento, indice)}
            >
              {item.rotulo}
            </button>
          );
        })}
      </div>
      <div role="tabpanel" id={ID_PAINEL} className="painel-busca" aria-labelledby={idDaAba(aba)}>
        {children}
      </div>
    </>
  );
}
