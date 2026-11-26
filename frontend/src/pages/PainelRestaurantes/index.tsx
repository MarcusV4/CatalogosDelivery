import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Botao } from '../../components/Botao';
import { Carregando } from '../../components/Carregando';
import { EstadoMensagem } from '../../components/EstadoMensagem';
import { useAutenticacao } from '../../hooks/useAutenticacao';
import { useNotificacao } from '../../hooks/useNotificacao';
import { useRestaurantesAdmin } from '../../hooks/useRestaurantesAdmin';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import {
  type Restaurante,
  type StatusRestaurante,
  type HorarioFuncionamento,
  chavesRestaurantes,
  listarHorariosGerenciamento,
  substituirHorarios,
} from '../../services/restaurantes';
import { mensagemDoErro } from '../../services/consultas';
import { ROTULOS_CATEGORIA_RESTAURANTE, ROTULOS_STATUS_RESTAURANTE } from '../../utils/rotulos';
import { FormularioHorarios } from './FormularioHorarios';
import { FormularioRestaurante } from './FormularioRestaurante';
import './styles.css';

/** Painel da conta LOJA para cadastrar e administrar seus restaurantes. */
export function PainelRestaurantes() {
  useTituloPagina('Meus restaurantes');
  const { sessao } = useAutenticacao();
  const usuarioId = lerUsuarioId(sessao?.usuarioId);
  const [mostrarCadastro, setMostrarCadastro] = useState(false);
  const [restauranteEditando, setRestauranteEditando] = useState<Restaurante | null>(null);
  const [horariosDoRestaurante, setHorariosDoRestaurante] = useState<Restaurante | null>(null);
  const [errosStatus, setErrosStatus] = useState<Record<number, string>>({});

  if (usuarioId === null) {
    return (
      <section className="painel-restaurantes">
        <EstadoMensagem
          tipo="erro"
          titulo="Não foi possível carregar seus restaurantes."
          descricao="O serviço precisa fornecer o identificador numérico da conta para relacioná-la aos restaurantes."
        />
      </section>
    );
  }

  return (
    <ConteudoPainel
      usuarioId={usuarioId}
      mostrarCadastro={mostrarCadastro}
      setMostrarCadastro={setMostrarCadastro}
      restauranteEditando={restauranteEditando}
      setRestauranteEditando={setRestauranteEditando}
      horariosDoRestaurante={horariosDoRestaurante}
      setHorariosDoRestaurante={setHorariosDoRestaurante}
      errosStatus={errosStatus}
      setErrosStatus={setErrosStatus}
    />
  );
}

function ConteudoPainel({
  usuarioId,
  mostrarCadastro,
  setMostrarCadastro,
  restauranteEditando,
  setRestauranteEditando,
  horariosDoRestaurante,
  setHorariosDoRestaurante,
  errosStatus,
  setErrosStatus,
}: {
  usuarioId: number;
  mostrarCadastro: boolean;
  setMostrarCadastro: (mostrar: boolean) => void;
  restauranteEditando: Restaurante | null;
  setRestauranteEditando: (restaurante: Restaurante | null) => void;
  horariosDoRestaurante: Restaurante | null;
  setHorariosDoRestaurante: (restaurante: Restaurante | null) => void;
  errosStatus: Record<number, string>;
  setErrosStatus: (erros: Record<number, string>) => void;
}) {
  const { restaurantes, criar, atualizar, status } = useRestaurantesAdmin(usuarioId);
  const salvandoDados = criar.isPending || atualizar.isPending;

  if (restaurantes.isPending) return <Carregando mensagem="Carregando seus restaurantes…" />;
  if (restaurantes.error) {
    return (
      <section className="painel-restaurantes">
        <EstadoMensagem
          tipo="erro"
          titulo="Não foi possível carregar seus restaurantes."
          descricao={mensagemDoErro(restaurantes.error)}
          onTentarDeNovo={() => void restaurantes.refetch()}
        />
      </section>
    );
  }

  const itens = restaurantes.data ?? [];
  const salvarCadastro = (dados: Parameters<typeof criar.mutate>[0]) => {
    criar.mutate(dados, {
      onSuccess: () => {
        setMostrarCadastro(false);
        setRestauranteEditando(null);
      },
    });
  };
  const salvarEdicao = (dados: Parameters<typeof atualizar.mutate>[0]['dados']) => {
    if (!restauranteEditando) return;
    atualizar.mutate(
      { id: restauranteEditando.restauranteId, dados },
      { onSuccess: () => setRestauranteEditando(null) },
    );
  };

  return (
    <section className="painel-restaurantes">
      <header className="painel-cardapio__cabecalho">
        <div>
          <p className="painel-cardapio__sobretitulo">Área da loja</p>
          <h1>Meus restaurantes</h1>
          <p>Atualize os dados, o status e os horários dos seus restaurantes.</p>
        </div>
        <Botao
          disabled={mostrarCadastro || salvandoDados}
          onClick={() => {
            setRestauranteEditando(null);
            setMostrarCadastro(true);
          }}
        >
          Novo restaurante
        </Botao>
      </header>

      {mostrarCadastro ? (
        <FormularioRestaurante
          salvando={criar.isPending}
          erro={criar.error ? mensagemDoErro(criar.error) : null}
          aoSalvar={salvarCadastro}
          aoCancelar={() => setMostrarCadastro(false)}
        />
      ) : null}
      {restauranteEditando ? (
        <FormularioRestaurante
          key={restauranteEditando.restauranteId}
          restaurante={restauranteEditando}
          salvando={atualizar.isPending}
          erro={atualizar.error ? mensagemDoErro(atualizar.error) : null}
          aoSalvar={salvarEdicao}
          aoCancelar={() => setRestauranteEditando(null)}
        />
      ) : null}

      {itens.length === 0 && !mostrarCadastro ? (
        <div className="painel-cardapio__vazio cartao">
          <EstadoMensagem
            tipo="vazio"
            titulo="Você ainda não tem restaurantes cadastrados."
            descricao="Cadastre seu primeiro restaurante para começar a gerenciá-lo."
            acoes={<Botao onClick={() => setMostrarCadastro(true)}>Cadastrar restaurante</Botao>}
          />
        </div>
      ) : null}

      {itens.length > 0 ? (
        <ul className="painel-restaurantes__lista">
          {itens.map((restaurante) => (
            <li key={restaurante.restauranteId}>
              <article className="painel-restaurantes__cartao">
                <div className="painel-restaurantes__dados">
                  <p className="painel-cardapio__sobretitulo">
                    {ROTULOS_CATEGORIA_RESTAURANTE[restaurante.categoriaRestaurante]}
                  </p>
                  <h2>{restaurante.restauranteNome}</h2>
                  <p>{restaurante.restauranteDesc}</p>
                  <p
                    className={`painel-restaurantes__status painel-restaurantes__status--${restaurante.statusRestaurante.toLowerCase()}`}
                  >
                    {ROTULOS_STATUS_RESTAURANTE[restaurante.statusRestaurante]}
                  </p>
                </div>
                <div className="painel-restaurantes__acoes">
                  <Botao
                    variante="contorno"
                    onClick={() => {
                      setMostrarCadastro(false);
                      setHorariosDoRestaurante(null);
                      setRestauranteEditando(restaurante);
                    }}
                  >
                    Editar dados
                  </Botao>
                  <Botao
                    variante="contorno"
                    onClick={() => {
                      setMostrarCadastro(false);
                      setRestauranteEditando(null);
                      setHorariosDoRestaurante(
                        horariosDoRestaurante?.restauranteId === restaurante.restauranteId
                          ? null
                          : restaurante,
                      );
                    }}
                  >
                    Editar horários
                  </Botao>
                  <Link
                    className="botao botao--contorno"
                    to={`/painel/restaurantes/${restaurante.restauranteId}/cardapio`}
                  >
                    Gerenciar cardápio
                  </Link>
                </div>
                <div className="painel-restaurantes__status-form">
                  <label htmlFor={`status-${restaurante.restauranteId}`}>Status</label>
                  <select
                    id={`status-${restaurante.restauranteId}`}
                    value={restaurante.statusRestaurante}
                    disabled={status.isPending}
                    onChange={(evento) => {
                      const valor = evento.target.value as StatusRestaurante;
                      setErrosStatus({ ...errosStatus, [restaurante.restauranteId]: '' });
                      status.mutate(
                        { id: restaurante.restauranteId, valor },
                        {
                          onError: (erro) =>
                            setErrosStatus({
                              ...errosStatus,
                              [restaurante.restauranteId]: mensagemDoErro(erro),
                            }),
                        },
                      );
                    }}
                  >
                    {Object.entries(ROTULOS_STATUS_RESTAURANTE).map(([valor, rotulo]) => (
                      <option key={valor} value={valor}>
                        {rotulo}
                      </option>
                    ))}
                  </select>
                  {errosStatus[restaurante.restauranteId] ? (
                    <p role="alert" className="painel-restaurantes__erro">
                      {errosStatus[restaurante.restauranteId]}
                    </p>
                  ) : null}
                  {status.isPending ? (
                    <span className="mudo" role="status">
                      Salvando status…
                    </span>
                  ) : null}
                </div>
                {horariosDoRestaurante?.restauranteId === restaurante.restauranteId ? (
                  <EditorHorarios restaurante={restaurante} />
                ) : null}
              </article>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function EditorHorarios({ restaurante }: { restaurante: Restaurante }) {
  return <PainelHorarios restaurante={restaurante} />;
}

function PainelHorarios({ restaurante }: { restaurante: Restaurante }) {
  const cliente = useQueryClient();
  const { notificar } = useNotificacao();
  const consulta = useQuery({
    queryKey: chavesRestaurantes.horarios(restaurante.restauranteId),
    queryFn: ({ signal }) => listarHorariosGerenciamento(restaurante.restauranteId, signal),
    meta: { silenciarErro: true },
  });
  const mutacao = useMutation({
    mutationFn: (horarios: HorarioFuncionamento[]) =>
      substituirHorarios(restaurante.restauranteId, horarios),
    meta: { silenciarErro: true },
    onSuccess: async () => {
      await cliente.invalidateQueries({
        queryKey: chavesRestaurantes.horarios(restaurante.restauranteId),
      });
      notificar('Horários atualizados.', 'sucesso');
    },
  });

  if (consulta.isPending) return <Carregando mensagem="Carregando horários…" />;
  if (consulta.error) {
    return (
      <EstadoMensagem
        tipo="erro"
        titulo="Não foi possível carregar os horários."
        descricao={mensagemDoErro(consulta.error)}
        onTentarDeNovo={() => void consulta.refetch()}
      />
    );
  }
  return (
    <FormularioHorarios
      key={`${restaurante.restauranteId}-${consulta.dataUpdatedAt}`}
      horarios={consulta.data ?? []}
      salvando={mutacao.isPending}
      erro={mutacao.error ? mensagemDoErro(mutacao.error) : null}
      aoSalvar={(horarios) => mutacao.mutate(horarios)}
    />
  );
}

function lerUsuarioId(usuarioId?: string): number | null {
  if (!usuarioId || !/^\d+$/.test(usuarioId)) return null;
  const numero = Number(usuarioId);
  return Number.isSafeInteger(numero) && numero > 0 ? numero : null;
}
