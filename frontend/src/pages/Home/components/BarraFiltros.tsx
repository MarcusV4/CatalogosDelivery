import type { CategoriaRestaurante } from '../../../services/restaurantes';
import { ROTULOS_CATEGORIA_RESTAURANTE_CURTOS } from '../../../utils/rotulos';

const CATEGORIAS = Object.entries(ROTULOS_CATEGORIA_RESTAURANTE_CURTOS) as [
  CategoriaRestaurante,
  string,
][];

interface BarraFiltrosProps {
  categoria: CategoriaRestaurante | null;
  abertoAgora: boolean;
  podeLimpar: boolean;
  aoEscolherCategoria: (categoria: CategoriaRestaurante | null) => void;
  aoAlternarAbertoAgora: () => void;
  aoLimpar: () => void;
}

/**
 * Categoria do restaurante em cartões (uma por vez; clicar de novo desmarca), mais os chips
 * "Abertos agora" e "Limpar".
 */
export function BarraFiltros({
  categoria,
  abertoAgora,
  podeLimpar,
  aoEscolherCategoria,
  aoAlternarAbertoAgora,
  aoLimpar,
}: BarraFiltrosProps) {
  return (
    <div className="filtros-busca">
      <div className="categorias" role="group" aria-label="Categoria do restaurante">
        {CATEGORIAS.map(([valor, rotulo]) => (
          <button
            key={valor}
            type="button"
            className="categoria"
            aria-pressed={categoria === valor}
            onClick={() => aoEscolherCategoria(categoria === valor ? null : valor)}
          >
            {rotulo}
          </button>
        ))}
      </div>
      <div className="chips" role="group" aria-label="Outros filtros">
        <button
          type="button"
          className="chip"
          aria-pressed={abertoAgora}
          onClick={aoAlternarAbertoAgora}
        >
          Abertos agora
        </button>
        {podeLimpar ? (
          <button type="button" className="chip" onClick={aoLimpar}>
            Limpar filtros
          </button>
        ) : null}
      </div>
    </div>
  );
}
