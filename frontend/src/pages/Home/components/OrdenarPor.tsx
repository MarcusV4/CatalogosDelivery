import {
  lerOrdem,
  opcoesDeOrdem,
  type AbaBusca,
  type OrdemBusca,
} from '../../../utils/filtrosBusca';

interface OrdenarPorProps {
  aba: AbaBusca;
  ordem: OrdemBusca | null;
  aoMudar: (ordem: OrdemBusca | null) => void;
}

/** Seletor "Ordenar por" com as opções da aba; a primeira opção é a ordem padrão (nome A–Z). */
export function OrdenarPor({ aba, ordem, aoMudar }: OrdenarPorProps) {
  return (
    <div className="campo ordenar-por">
      <label htmlFor="ordenar-por">Ordenar por</label>
      <select
        id="ordenar-por"
        value={ordem ?? ''}
        onChange={(evento) => aoMudar(lerOrdem(aba, evento.target.value))}
      >
        {opcoesDeOrdem(aba).map(({ valor, rotulo }) => (
          <option key={valor} value={valor}>
            {rotulo}
          </option>
        ))}
      </select>
    </div>
  );
}
