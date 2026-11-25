import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { Botao } from '../../components/Botao';
import { Carregando } from '../../components/Carregando';
import { EstadoMensagem } from '../../components/EstadoMensagem';
import { ImagemComFallback } from '../../components/ImagemComFallback';
import { useCardapioAdmin } from '../../hooks/useCardapioAdmin';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { type CategoriaProduto, type Produto, type ProdutoCadastro } from '../../services/cardapio';
import { mensagemDoErro } from '../../services/consultas';
import { lerIdDaRota } from '../../utils/identificador';
import {
  centavosParaReais,
  MENSAGEM_PRECO_INVALIDO,
  reaisParaCentavos,
} from '../../utils/precoEntrada';
import { formatarPreco } from '../../utils/preco';
import { ROTULOS_CATEGORIA_PRODUTO } from '../../utils/rotulos';
import { NaoEncontrada } from '../NaoEncontrada';
import './styles.css';

const TITULO_PAGINA = 'Gerenciar cardápio';

/** Painel de produtos para `/painel/restaurantes/:restauranteId/cardapio`. */
export function PainelCardapio() {
  const restauranteId = lerIdDaRota(useParams().restauranteId);
  if (restauranteId === null) {
    return (
      <NaoEncontrada
        titulo="Restaurante não encontrado"
        descricao="O identificador do restaurante no endereço é inválido."
      />
    );
  }
  return <ConteudoPainel key={restauranteId} restauranteId={restauranteId} />;
}

function ConteudoPainel({ restauranteId }: { restauranteId: number }) {
  const { cardapio, categorias, criar, atualizar, disponibilidade, excluir } =
    useCardapioAdmin(restauranteId);
  const [produtoEmEdicao, setProdutoEmEdicao] = useState<Produto | null>(null);
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [erroAcao, setErroAcao] = useState<string | null>(null);
  const carregando = cardapio.isPending || categorias.isPending;
  const erroConsulta = cardapio.error ?? categorias.error;
  const mutacaoPendente =
    criar.isPending || atualizar.isPending || disponibilidade.isPending || excluir.isPending;

  useTituloPagina(TITULO_PAGINA);

  if (carregando) {
    return <Carregando mensagem="Carregando itens do cardápio…" />;
  }
  if (erroConsulta || cardapio.data === undefined || categorias.data === undefined) {
    return (
      <EstadoMensagem
        tipo="erro"
        titulo="Não foi possível carregar o painel do cardápio."
        descricao={mensagemDoErro(erroConsulta)}
        onTentarDeNovo={() => {
          void cardapio.refetch();
          void categorias.refetch();
        }}
      />
    );
  }

  const iniciarCadastro = () => {
    setProdutoEmEdicao(null);
    setMostrarFormulario(true);
    setErroAcao(null);
  };

  const iniciarEdicao = (produto: Produto) => {
    setProdutoEmEdicao(produto);
    setMostrarFormulario(true);
    setErroAcao(null);
  };

  const fecharFormulario = () => {
    setMostrarFormulario(false);
    setProdutoEmEdicao(null);
    criar.reset();
    atualizar.reset();
  };

  const alternarDisponibilidade = async (produto: Produto) => {
    setErroAcao(null);
    try {
      await disponibilidade.mutateAsync({ id: produto.id, disponivel: !produto.disponivel });
    } catch (erro) {
      setErroAcao(mensagemDoErro(erro));
    }
  };

  const excluirProduto = async (produto: Produto) => {
    const confirmar = window.confirm(`Excluir “${produto.nome}” do cardápio?`);
    if (!confirmar) return;
    setErroAcao(null);
    try {
      await excluir.mutateAsync(produto.id);
    } catch (erro) {
      setErroAcao(mensagemDoErro(erro));
    }
  };

  const categoriasVazias = categorias.data.length === 0;
  const erroFormulario = mensagemDoErro(produtoEmEdicao ? atualizar.error : criar.error);

  return (
    <section className="painel-cardapio" aria-labelledby="painel-cardapio-titulo">
      <header className="painel-cardapio__cabecalho">
        <div>
          <p className="painel-cardapio__sobretitulo">Restaurante {restauranteId}</p>
          <h1 id="painel-cardapio-titulo">{TITULO_PAGINA}</h1>
          <p className="mudo">Cadastre e mantenha os itens deste cardápio.</p>
        </div>
        <Botao onClick={iniciarCadastro} disabled={mutacaoPendente || categoriasVazias}>
          Novo item
        </Botao>
      </header>

      <p className="aviso" role="note">
        O controle de acesso do proprietário ainda não está ativo neste painel.
      </p>

      {categoriasVazias ? (
        <p className="painel-cardapio__erro" role="alert">
          O serviço não retornou categorias de produto; não é possível cadastrar ou editar itens.
        </p>
      ) : null}

      {erroAcao ? (
        <p className="painel-cardapio__erro" role="alert">
          {erroAcao}
        </p>
      ) : null}

      {mostrarFormulario ? (
        <FormularioProduto
          produto={produtoEmEdicao}
          categorias={categorias.data}
          salvando={criar.isPending || atualizar.isPending}
          erro={erroFormulario}
          aoCancelar={fecharFormulario}
          aoSalvar={async (dados) => {
            try {
              if (produtoEmEdicao) {
                await atualizar.mutateAsync({ id: produtoEmEdicao.id, produto: dados });
              } else {
                await criar.mutateAsync(dados);
              }
              fecharFormulario();
            } catch {
              // A mensagem da API fica no formulário, que permanece aberto para correção.
            }
          }}
        />
      ) : null}

      {cardapio.data.length === 0 ? (
        <div className="painel-cardapio__vazio">
          <EstadoMensagem tipo="vazio" titulo="Este cardápio ainda não tem itens." />
          <Botao onClick={iniciarCadastro} disabled={categoriasVazias}>
            Cadastrar primeiro item
          </Botao>
        </div>
      ) : (
        <ul className="painel-cardapio__lista" aria-label="Itens do cardápio">
          {cardapio.data.map((produto) => (
            <li className="painel-produto" key={produto.id}>
              <ImagemComFallback
                src={produto.urlImagem}
                alt={`Imagem de ${produto.nome}`}
                className="painel-produto__imagem"
                fallback={
                  <span className="painel-produto__imagem-fallback" aria-hidden="true">
                    {produto.nome.slice(0, 1)}
                  </span>
                }
              />
              <div className="painel-produto__dados">
                <h2>{produto.nome}</h2>
                {produto.descricao ? <p className="mudo">{produto.descricao}</p> : null}
                <p className="painel-produto__detalhes">
                  <span>{ROTULOS_CATEGORIA_PRODUTO[produto.categoria]}</span>
                  <span>{formatarPreco(produto.preco)}</span>
                  <span
                    className={
                      produto.disponivel
                        ? 'painel-produto__status'
                        : 'painel-produto__status painel-produto__status--inativo'
                    }
                  >
                    {produto.disponivel ? 'Disponível' : 'Indisponível'}
                  </span>
                </p>
              </div>
              <div className="painel-produto__acoes" aria-label={`Ações de ${produto.nome}`}>
                <Botao
                  variante="contorno"
                  disabled={mutacaoPendente || categoriasVazias}
                  onClick={() => iniciarEdicao(produto)}
                  aria-label={`Editar ${produto.nome}`}
                >
                  Editar
                </Botao>
                <Botao
                  variante="contorno"
                  disabled={mutacaoPendente}
                  onClick={() => void alternarDisponibilidade(produto)}
                  aria-label={`${produto.disponivel ? 'Tornar indisponível' : 'Tornar disponível'}: ${produto.nome}`}
                >
                  {produto.disponivel ? 'Desativar' : 'Ativar'}
                </Botao>
                <Botao
                  variante="contorno"
                  disabled={mutacaoPendente}
                  onClick={() => void excluirProduto(produto)}
                  aria-label={`Excluir ${produto.nome}`}
                >
                  Excluir
                </Botao>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

interface FormularioProdutoProps {
  produto: Produto | null;
  categorias: CategoriaProduto[];
  salvando: boolean;
  erro: string;
  aoCancelar: () => void;
  aoSalvar: (produto: ProdutoCadastro) => Promise<void>;
}

function FormularioProduto({
  produto,
  categorias,
  salvando,
  erro,
  aoCancelar,
  aoSalvar,
}: FormularioProdutoProps) {
  const [nome, setNome] = useState(produto?.nome ?? '');
  const [descricao, setDescricao] = useState(produto?.descricao ?? '');
  const [preco, setPreco] = useState(produto ? centavosParaReais(produto.preco) : '');
  const [categoria, setCategoria] = useState<CategoriaProduto>(
    produto?.categoria ?? categorias[0] ?? 'LANCHE',
  );
  const [urlImagem, setUrlImagem] = useState(produto?.urlImagem ?? '');
  const [disponivel, setDisponivel] = useState(produto?.disponivel ?? true);
  const [erroLocal, setErroLocal] = useState<string | null>(null);

  const enviar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    setErroLocal(null);
    const nomeLimpo = nome.trim();
    const precoEmCentavos = reaisParaCentavos(preco);
    if (!nomeLimpo) {
      setErroLocal('Informe o nome do item.');
      return;
    }
    if (precoEmCentavos === null) {
      setErroLocal(MENSAGEM_PRECO_INVALIDO);
      return;
    }
    if (!categorias.includes(categoria)) {
      setErroLocal('Selecione uma categoria.');
      return;
    }
    void aoSalvar({
      nome: nomeLimpo,
      descricao: descricao.trim() || null,
      preco: precoEmCentavos,
      disponivel,
      urlImagem: urlImagem.trim() || null,
      categoria,
    });
  };

  return (
    <form
      className="painel-formulario"
      onSubmit={enviar}
      noValidate
      aria-labelledby="formulario-produto-titulo"
    >
      <h2 id="formulario-produto-titulo">{produto ? 'Editar item' : 'Novo item'}</h2>
      <div className="painel-formulario__campos">
        <div className="campo">
          <label htmlFor="produto-nome">Nome</label>
          <input
            id="produto-nome"
            value={nome}
            onChange={(evento) => setNome(evento.target.value)}
            maxLength={120}
            required
            disabled={salvando}
          />
        </div>
        <div className="campo">
          <label htmlFor="produto-preco">Preço (R$)</label>
          <input
            id="produto-preco"
            inputMode="decimal"
            autoComplete="off"
            placeholder="25,90"
            value={preco}
            onChange={(evento) => setPreco(evento.target.value)}
            required
            disabled={salvando}
            aria-describedby={erroLocal ? 'produto-erro-local' : undefined}
          />
        </div>
        <div className="campo">
          <label htmlFor="produto-categoria">Categoria</label>
          <select
            id="produto-categoria"
            value={categoria}
            onChange={(evento) => setCategoria(evento.target.value as CategoriaProduto)}
            required
            disabled={salvando}
          >
            {categorias.map((item) => (
              <option key={item} value={item}>
                {ROTULOS_CATEGORIA_PRODUTO[item]}
              </option>
            ))}
          </select>
        </div>
        <div className="campo painel-formulario__campo-largo">
          <label htmlFor="produto-descricao">Descrição</label>
          <textarea
            id="produto-descricao"
            rows={3}
            value={descricao}
            onChange={(evento) => setDescricao(evento.target.value)}
            maxLength={1000}
            disabled={salvando}
          />
        </div>
        <div className="campo painel-formulario__campo-largo">
          <label htmlFor="produto-imagem">URL da imagem</label>
          <input
            id="produto-imagem"
            type="url"
            value={urlImagem}
            onChange={(evento) => setUrlImagem(evento.target.value)}
            disabled={salvando}
          />
        </div>
        <label className="painel-formulario__disponivel" htmlFor="produto-disponivel">
          <input
            id="produto-disponivel"
            type="checkbox"
            checked={disponivel}
            onChange={(evento) => setDisponivel(evento.target.checked)}
            disabled={salvando}
          />
          Disponível para venda
        </label>
      </div>
      {erroLocal ? (
        <p id="produto-erro-local" className="painel-cardapio__erro" role="alert">
          {erroLocal}
        </p>
      ) : null}
      {erro ? (
        <p className="painel-cardapio__erro" role="alert">
          {erro}
        </p>
      ) : null}
      <div className="painel-formulario__acoes">
        <Botao type="submit" disabled={salvando}>
          {salvando ? 'Salvando…' : produto ? 'Salvar alterações' : 'Cadastrar item'}
        </Botao>
        <Botao variante="contorno" disabled={salvando} onClick={aoCancelar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}
