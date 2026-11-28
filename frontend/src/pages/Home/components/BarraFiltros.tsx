import type { CategoriaRestaurante } from '../../../services/restaurantes';
import { ROTULOS_CATEGORIA_RESTAURANTE_CURTOS } from '../../../utils/rotulos';

const CATEGORIAS = Object.entries(ROTULOS_CATEGORIA_RESTAURANTE_CURTOS) as [
  CategoriaRestaurante,
  string,
][];

interface BarraFiltrosProps {
  categoria: CategoriaRestaurante | null;
  abertoAgora: boolean;
  entregaGratis: boolean;
  /** Só na aba Pratos; sem `aoAlternarDisponivel`, o chip "Só disponíveis" não aparece. */
  disponivel?: boolean;
  podeLimpar: boolean;
  aoEscolherCategoria: (categoria: CategoriaRestaurante | null) => void;
  aoAlternarAbertoAgora: () => void;
  aoAlternarEntregaGratis: () => void;
  aoAlternarDisponivel?: () => void;
  aoLimpar: () => void;
}

/**
 * Categoria do restaurante em cartões (uma por vez; clicar de novo desmarca), mais os chips
 * "Abertos agora", "Entrega grátis", "Só disponíveis" (aba Pratos) e "Limpar".
 */
export function BarraFiltros({
  categoria,
  abertoAgora,
  entregaGratis,
  disponivel = false,
  podeLimpar,
  aoEscolherCategoria,
  aoAlternarAbertoAgora,
  aoAlternarEntregaGratis,
  aoAlternarDisponivel,
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
        <button
          type="button"
          className="chip"
          aria-pressed={entregaGratis}
          onClick={aoAlternarEntregaGratis}
        >
          Entrega grátis
        </button>
        {aoAlternarDisponivel ? (
          <button
            type="button"
            className="chip"
            aria-pressed={disponivel}
            onClick={aoAlternarDisponivel}
          >
            Só disponíveis
          </button>
        ) : null}
        {podeLimpar ? (
          <button type="button" className="chip" onClick={aoLimpar}>
            Limpar filtros
          </button>
        ) : null}
      </div>
    </div>
  );
}
