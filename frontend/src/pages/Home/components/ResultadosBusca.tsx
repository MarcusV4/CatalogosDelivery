import type { UseQueryResult } from '@tanstack/react-query';
import { useEffect, useRef, type ReactNode } from 'react';
import { Carregando } from '../../../components/Carregando';
import { EstadoMensagem } from '../../../components/EstadoMensagem';
import type { Pagina } from '../../../services/busca';
import { mensagemDoErro } from '../../../services/consultas';
import { corrigirPagina, textoTotal } from '../../../utils/paginacao';
import { Paginacao } from './Paginacao';

export interface OpcoesNavegacao {
  /** Troca a entrada atual do histórico em vez de criar outra. */
  substituir?: boolean;
}

interface ResultadosBuscaProps<T> {
  titulo: string;
  singular: string;
  plural: string;
  consulta: UseQueryResult<Pagina<T>>;
  /** Página da URL, começando em 1. */
  pagina: number;
  aoMudarPagina: (pagina: number, opcoes?: OpcoesNavegacao) => void;
  chave: (item: T) => number;
  renderizar: (item: T) => ReactNode;
}

/**
 * Lista paginada de uma aba. Enquanto a próxima página carrega, a atual continua na tela
 * (esmaecida, `aria-busy`); o total é anunciado num `role="status"`.
 */
export function ResultadosBusca<T>({
  titulo,
  singular,
  plural,
  consulta,
  pagina,
  aoMudarPagina,
  chave,
  renderizar,
}: ResultadosBuscaProps<T>) {
  const tituloRef = useRef<HTMLHeadingElement>(null);
  const { data, error, isPending, isError, isPlaceholderData, refetch } = consulta;
  const atualizando = isPlaceholderData;
  const paginaCorrigida = data && !atualizando ? corrigirPagina(pagina, data.totalPaginas) : null;

  useEffect(() => {
    if (paginaCorrigida !== null) {
      aoMudarPagina(paginaCorrigida, { substituir: true });
    }
  }, [paginaCorrigida, aoMudarPagina]);

  const mudarPagina = (nova: number) => {
    aoMudarPagina(nova);
    tituloRef.current?.focus();
  };

  const status = isError
    ? ''
    : atualizando
      ? 'Atualizando resultados…'
      : data
        ? textoTotal(data.totalElementos, singular, plural)
        : '';

  let conteudo: ReactNode;
  if (isPending || paginaCorrigida !== null) {
    conteudo = <Carregando mensagem={`Buscando ${plural}…`} />;
  } else if (isError) {
    conteudo = (
      <EstadoMensagem
        tipo="erro"
        titulo="Não foi possível fazer a busca."
        descricao={mensagemDoErro(error)}
        onTentarDeNovo={() => void refetch()}
      />
    );
  } else if (data.conteudo.length === 0) {
    conteudo = <EstadoMensagem tipo="vazio" titulo={`Nenhum ${singular} encontrado.`} />;
  } else {
    conteudo = (
      <>
        <ul className={`resultados__grade${atualizando ? ' resultados__grade--atualizando' : ''}`}>
          {data.conteudo.map((item) => (
            <li key={chave(item)}>{renderizar(item)}</li>
          ))}
        </ul>
        <Paginacao pagina={pagina} totalPaginas={data.totalPaginas} aoMudar={mudarPagina} />
      </>
    );
  }

  return (
    <section className="resultados" aria-labelledby="resultados-titulo" aria-busy={atualizando}>
      <div className="resultados__topo">
        <h2 id="resultados-titulo" ref={tituloRef} tabIndex={-1}>
          {titulo}
        </h2>
        <p className="mudo pequeno" role="status">
          {status}
        </p>
      </div>
      {conteudo}
    </section>
  );
}
