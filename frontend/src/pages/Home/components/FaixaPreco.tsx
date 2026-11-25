import { useState } from 'react';
import { useDebounce } from '../../../hooks/useDebounce';
import { centavosParaReais, lerFaixaDigitada } from '../../../utils/precoEntrada';

interface Faixa {
  precoMin: number | null;
  precoMax: number | null;
}

interface FaixaPrecoProps extends Faixa {
  aoAplicar: (faixa: Faixa) => void;
}

const paraTexto = (centavos: number | null) =>
  centavos === null ? '' : centavosParaReais(centavos);

/**
 * Campos "De" e "Até" em reais. Aplica a faixa (em centavos) depois de uma pausa na digitação;
 * enquanto houver erro, nada é enviado e a mensagem aparece junto ao campo.
 */
export function FaixaPreco({ precoMin, precoMax, aoAplicar }: FaixaPrecoProps) {
  const [textos, setTextos] = useState({ min: paraTexto(precoMin), max: paraTexto(precoMax) });
  const [aplicada, setAplicada] = useState<Faixa>({ precoMin, precoMax });
  const agendador = useDebounce(aoAplicar);

  // Faixa mudou por fora (voltar do navegador, "Limpar filtros"): mostra a nova.
  if (precoMin !== aplicada.precoMin || precoMax !== aplicada.precoMax) {
    setAplicada({ precoMin, precoMax });
    const atual = lerFaixaDigitada(textos.min, textos.max);
    if (!atual.valida || atual.precoMin !== precoMin || atual.precoMax !== precoMax) {
      setTextos({ min: paraTexto(precoMin), max: paraTexto(precoMax) });
    }
  }

  const resultado = lerFaixaDigitada(textos.min, textos.max);
  const erroMin = resultado.valida ? null : resultado.erroMin;
  const erroMax = resultado.valida ? null : resultado.erroMax;
  const erroFaixa = resultado.valida ? null : resultado.erroFaixa;

  const mudar = (campo: 'min' | 'max', valor: string) => {
    const novos = { ...textos, [campo]: valor };
    setTextos(novos);
    const faixa = lerFaixaDigitada(novos.min, novos.max);
    if (faixa.valida) {
      agendador.agendar({ precoMin: faixa.precoMin, precoMax: faixa.precoMax });
    } else {
      agendador.cancelar();
    }
  };

  return (
    <fieldset
      className="faixa-preco"
      aria-describedby={erroFaixa ? 'faixa-preco-erro' : undefined}
      onKeyDown={(evento) => {
        if (evento.key === 'Enter') {
          agendador.executarAgora();
        }
      }}
    >
      <legend>Faixa de preço</legend>
      <div className="faixa-preco__campos">
        <CampoPreco
          id="preco-min"
          rotulo="De (R$)"
          valor={textos.min}
          erro={erroMin}
          invalido={Boolean(erroMin || erroFaixa)}
          aoMudar={(valor) => mudar('min', valor)}
        />
        <CampoPreco
          id="preco-max"
          rotulo="Até (R$)"
          valor={textos.max}
          erro={erroMax}
          invalido={Boolean(erroMax || erroFaixa)}
          aoMudar={(valor) => mudar('max', valor)}
        />
      </div>
      {erroFaixa ? (
        <p id="faixa-preco-erro" className="faixa-preco__erro" role="alert">
          {erroFaixa}
        </p>
      ) : null}
    </fieldset>
  );
}

interface CampoPrecoProps {
  id: string;
  rotulo: string;
  valor: string;
  erro: string | null;
  invalido: boolean;
  aoMudar: (valor: string) => void;
}

function CampoPreco({ id, rotulo, valor, erro, invalido, aoMudar }: CampoPrecoProps) {
  const idErro = `${id}-erro`;
  return (
    <div className={`campo${invalido ? ' campo--erro' : ''}`}>
      <label htmlFor={id}>{rotulo}</label>
      <input
        id={id}
        type="text"
        inputMode="decimal"
        autoComplete="off"
        placeholder="0,00"
        value={valor}
        aria-invalid={invalido}
        aria-describedby={erro ? idErro : undefined}
        onChange={(evento) => aoMudar(evento.target.value)}
      />
      {erro ? (
        <small id={idErro} role="alert">
          {erro}
        </small>
      ) : null}
    </div>
  );
}
