import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import {
  comFiltro,
  escreverFiltros,
  lerFiltros,
  limparFiltros,
  temFiltros,
  trocarAba,
  type FiltrosBusca,
} from '../../utils/filtrosBusca';
import { AbasBusca } from './components/AbasBusca';
import { BarraFiltros } from './components/BarraFiltros';
import { CampoBusca } from './components/CampoBusca';
import { FaixaPreco } from './components/FaixaPreco';
import type { OpcoesNavegacao } from './components/ResultadosBusca';
import { ResultadosPratos } from './components/ResultadosPratos';
import { ResultadosRestaurantes } from './components/ResultadosRestaurantes';
import './styles.css';

const PLACEHOLDER = {
  restaurantes: 'Buscar restaurante pelo nome',
  pratos: 'Buscar prato pelo nome',
} as const;

/**
 * Tela inicial: busca de restaurantes e de pratos no serviço de Busca. O estado dos filtros
 * fica na URL (link compartilhável e "voltar" do navegador); a digitação troca a entrada do
 * histórico, os cliques criam uma nova.
 */
export function Home() {
  useTituloPagina();
  const [params, setParams] = useSearchParams();
  const filtros = useMemo(() => lerFiltros(params), [params]);

  const aplicar = useCallback(
    (novos: FiltrosBusca, { substituir = false }: OpcoesNavegacao = {}) =>
      setParams(escreverFiltros(novos), { replace: substituir }),
    [setParams],
  );

  const mudarPagina = useCallback(
    (pagina: number, opcoes?: OpcoesNavegacao) => aplicar({ ...filtros, pagina }, opcoes),
    [aplicar, filtros],
  );

  const buscarPorNome = (nome: string) => {
    if (nome !== filtros.nome) {
      aplicar(comFiltro(filtros, { nome }), { substituir: true });
    }
  };

  return (
    <section className="home" aria-labelledby="home-titulo">
      <div className="home__faixa">
        <h1 id="home-titulo">Fome de quê hoje?</h1>
        <CampoBusca
          nome={filtros.nome}
          placeholder={PLACEHOLDER[filtros.aba]}
          aoBuscar={buscarPorNome}
        />
      </div>

      <AbasBusca aba={filtros.aba} aoTrocar={(aba) => aplicar(trocarAba(filtros, aba))}>
        <div className="home__filtros">
          <BarraFiltros
            categoria={filtros.categoria}
            abertoAgora={filtros.abertoAgora}
            podeLimpar={temFiltros(filtros)}
            aoEscolherCategoria={(categoria) => aplicar(comFiltro(filtros, { categoria }))}
            aoAlternarAbertoAgora={() =>
              aplicar(comFiltro(filtros, { abertoAgora: !filtros.abertoAgora }))
            }
            aoLimpar={() => aplicar(limparFiltros(filtros))}
          />
          {filtros.aba === 'pratos' ? (
            <FaixaPreco
              precoMin={filtros.precoMin}
              precoMax={filtros.precoMax}
              aoAplicar={(faixa) => aplicar(comFiltro(filtros, faixa), { substituir: true })}
            />
          ) : null}
        </div>

        {filtros.aba === 'restaurantes' ? (
          <ResultadosRestaurantes filtros={filtros} aoMudarPagina={mudarPagina} />
        ) : (
          <ResultadosPratos filtros={filtros} aoMudarPagina={mudarPagina} />
        )}
      </AbasBusca>
    </section>
  );
}
