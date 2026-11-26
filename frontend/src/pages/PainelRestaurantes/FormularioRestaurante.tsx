import { useState, type FormEvent } from 'react';
import { Botao } from '../../components/Botao';
import {
  type CategoriaRestaurante,
  type DadosRestaurante,
  type Restaurante,
} from '../../services/restaurantes';
import { centavosParaReais, reaisParaCentavos } from '../../utils/precoEntrada';
import { ROTULOS_CATEGORIA_RESTAURANTE } from '../../utils/rotulos';

interface FormularioRestauranteProps {
  restaurante?: Restaurante | null;
  salvando: boolean;
  erro?: string | null;
  aoSalvar: (dados: DadosRestaurante) => void;
  aoCancelar: () => void;
}

interface CamposFormulario {
  restauranteNome: string;
  restauranteDesc: string;
  logradouro: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  estado: string;
  cep: string;
  restauranteTelefone: string;
  taxaEntrega: string;
  tempoEstimadoMin: string;
  tempoEstimadoMax: string;
  urlImagem: string;
  categoriaRestaurante: CategoriaRestaurante;
}

const CATEGORIAS = Object.keys(ROTULOS_CATEGORIA_RESTAURANTE) as CategoriaRestaurante[];

function camposIniciais(restaurante?: Restaurante | null): CamposFormulario {
  return {
    restauranteNome: restaurante?.restauranteNome ?? '',
    restauranteDesc: restaurante?.restauranteDesc ?? '',
    logradouro: restaurante?.logradouro ?? '',
    numero: restaurante?.numero ?? '',
    complemento: restaurante?.complemento ?? '',
    bairro: restaurante?.bairro ?? '',
    cidade: restaurante?.cidade ?? '',
    estado: restaurante?.estado ?? '',
    cep: restaurante?.cep ?? '',
    restauranteTelefone: restaurante?.restauranteTelefone ?? '',
    taxaEntrega: restaurante ? centavosParaReais(restaurante.taxaEntrega) : '',
    tempoEstimadoMin: restaurante?.tempoEstimadoMin?.toString() ?? '',
    tempoEstimadoMax: restaurante?.tempoEstimadoMax?.toString() ?? '',
    urlImagem: restaurante?.urlImagem ?? '',
    categoriaRestaurante: restaurante?.categoriaRestaurante ?? 'OUTROS',
  };
}

/** Formulário compartilhado de cadastro e edição, com validação alinhada ao contrato REST. */
export function FormularioRestaurante({
  restaurante,
  salvando,
  erro,
  aoSalvar,
  aoCancelar,
}: FormularioRestauranteProps) {
  const [campos, setCampos] = useState(() => camposIniciais(restaurante));
  const [erroLocal, setErroLocal] = useState<string | null>(null);
  const alterar = (chave: keyof CamposFormulario, valor: string) =>
    setCampos((atuais) => ({ ...atuais, [chave]: valor }));

  const enviar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    setErroLocal(null);
    const taxaEntrega = reaisParaCentavos(campos.taxaEntrega);
    const tempoEstimadoMin = Number(campos.tempoEstimadoMin);
    const tempoEstimadoMax = Number(campos.tempoEstimadoMax);

    if (
      Object.entries(campos).some(
        ([chave, valor]) =>
          chave !== 'complemento' && chave !== 'urlImagem' && !String(valor).trim(),
      )
    ) {
      setErroLocal('Preencha todos os campos obrigatórios.');
      return;
    }
    if (!/^[A-Za-z]{2}$/.test(campos.estado.trim())) {
      setErroLocal('Informe a sigla do estado com duas letras, como PE.');
      return;
    }
    if (!/^\d{5}-?\d{3}$/.test(campos.cep.trim())) {
      setErroLocal('Informe um CEP válido, como 50000-000.');
      return;
    }
    if (!/^\d{10,11}$/.test(campos.restauranteTelefone.trim())) {
      setErroLocal('Informe telefone com DDD, usando 10 ou 11 números sem espaços.');
      return;
    }
    if (taxaEntrega === null) {
      setErroLocal('Informe uma taxa válida, em reais, como 5,50.');
      return;
    }
    if (!Number.isInteger(tempoEstimadoMin) || tempoEstimadoMin <= 0) {
      setErroLocal('O tempo mínimo deve ser um número maior que zero.');
      return;
    }
    if (!Number.isInteger(tempoEstimadoMax) || tempoEstimadoMax <= 0) {
      setErroLocal('O tempo máximo deve ser um número maior que zero.');
      return;
    }
    if (tempoEstimadoMax < tempoEstimadoMin) {
      setErroLocal('O tempo máximo não pode ser menor que o tempo mínimo.');
      return;
    }

    aoSalvar({
      restauranteNome: campos.restauranteNome.trim(),
      restauranteDesc: campos.restauranteDesc.trim(),
      logradouro: campos.logradouro.trim(),
      numero: campos.numero.trim(),
      complemento: campos.complemento.trim() || null,
      bairro: campos.bairro.trim(),
      cidade: campos.cidade.trim(),
      estado: campos.estado.trim().toUpperCase(),
      cep: campos.cep.trim(),
      restauranteTelefone: campos.restauranteTelefone.trim(),
      taxaEntrega,
      tempoEstimadoMin,
      tempoEstimadoMax,
      urlImagem: campos.urlImagem.trim() || null,
      categoriaRestaurante: campos.categoriaRestaurante,
    });
  };

  return (
    <form
      className="painel-formulario painel-restaurantes__formulario"
      onSubmit={enviar}
      noValidate
    >
      <h2>{restaurante ? 'Editar restaurante' : 'Cadastrar restaurante'}</h2>
      <div className="painel-formulario__campos">
        <div className="campo">
          <label htmlFor="restaurante-nome">Nome</label>
          <input
            id="restaurante-nome"
            value={campos.restauranteNome}
            maxLength={120}
            required
            disabled={salvando}
            onChange={(e) => alterar('restauranteNome', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-categoria">Categoria</label>
          <select
            id="restaurante-categoria"
            value={campos.categoriaRestaurante}
            disabled={salvando}
            onChange={(e) => alterar('categoriaRestaurante', e.target.value)}
          >
            {CATEGORIAS.map((categoria) => (
              <option key={categoria} value={categoria}>
                {ROTULOS_CATEGORIA_RESTAURANTE[categoria]}
              </option>
            ))}
          </select>
        </div>
        <div className="campo painel-formulario__campo-largo">
          <label htmlFor="restaurante-descricao">Descrição</label>
          <textarea
            id="restaurante-descricao"
            rows={3}
            value={campos.restauranteDesc}
            required
            disabled={salvando}
            onChange={(e) => alterar('restauranteDesc', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-telefone">Telefone (DDD + número)</label>
          <input
            id="restaurante-telefone"
            inputMode="numeric"
            placeholder="81999990000"
            value={campos.restauranteTelefone}
            required
            disabled={salvando}
            onChange={(e) => alterar('restauranteTelefone', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-imagem">URL da imagem (opcional)</label>
          <input
            id="restaurante-imagem"
            type="url"
            value={campos.urlImagem}
            disabled={salvando}
            onChange={(e) => alterar('urlImagem', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-logradouro">Logradouro</label>
          <input
            id="restaurante-logradouro"
            value={campos.logradouro}
            required
            disabled={salvando}
            onChange={(e) => alterar('logradouro', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-numero">Número</label>
          <input
            id="restaurante-numero"
            value={campos.numero}
            required
            disabled={salvando}
            onChange={(e) => alterar('numero', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-complemento">Complemento (opcional)</label>
          <input
            id="restaurante-complemento"
            value={campos.complemento}
            disabled={salvando}
            onChange={(e) => alterar('complemento', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-bairro">Bairro</label>
          <input
            id="restaurante-bairro"
            value={campos.bairro}
            required
            disabled={salvando}
            onChange={(e) => alterar('bairro', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-cidade">Cidade</label>
          <input
            id="restaurante-cidade"
            value={campos.cidade}
            required
            disabled={salvando}
            onChange={(e) => alterar('cidade', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-estado">Estado (UF)</label>
          <input
            id="restaurante-estado"
            maxLength={2}
            value={campos.estado}
            required
            disabled={salvando}
            onChange={(e) => alterar('estado', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-cep">CEP</label>
          <input
            id="restaurante-cep"
            placeholder="50000-000"
            value={campos.cep}
            required
            disabled={salvando}
            onChange={(e) => alterar('cep', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-taxa">Taxa de entrega (R$)</label>
          <input
            id="restaurante-taxa"
            inputMode="decimal"
            placeholder="5,50"
            value={campos.taxaEntrega}
            required
            disabled={salvando}
            onChange={(e) => alterar('taxaEntrega', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-tempo-min">Tempo mínimo estimado (min)</label>
          <input
            id="restaurante-tempo-min"
            type="number"
            min="1"
            value={campos.tempoEstimadoMin}
            required
            disabled={salvando}
            onChange={(e) => alterar('tempoEstimadoMin', e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="restaurante-tempo-max">Tempo máximo estimado (min)</label>
          <input
            id="restaurante-tempo-max"
            type="number"
            min="1"
            value={campos.tempoEstimadoMax}
            required
            disabled={salvando}
            onChange={(e) => alterar('tempoEstimadoMax', e.target.value)}
          />
        </div>
      </div>
      {erroLocal ? (
        <p className="painel-restaurantes__erro" role="alert">
          {erroLocal}
        </p>
      ) : null}
      {erro ? (
        <p className="painel-restaurantes__erro" role="alert">
          {erro}
        </p>
      ) : null}
      <div className="painel-formulario__acoes">
        <Botao type="submit" disabled={salvando}>
          {salvando ? 'Salvando…' : restaurante ? 'Salvar alterações' : 'Cadastrar restaurante'}
        </Botao>
        <Botao variante="contorno" disabled={salvando} onClick={aoCancelar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}
