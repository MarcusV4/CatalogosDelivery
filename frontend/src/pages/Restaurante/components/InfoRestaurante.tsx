import type { UseQueryResult } from '@tanstack/react-query';
import type { HorarioFuncionamento, Restaurante } from '../../../services/restaurantes';
import { linhasEndereco } from '../../../utils/restaurante';
import { ListaHorarios } from './ListaHorarios';

interface InfoRestauranteProps {
  restaurante: Restaurante;
  horarios: UseQueryResult<HorarioFuncionamento[]>;
}

const SEM_INFORMACAO = '—';

/** Sobre, endereço, telefone e horários. Erro nos horários não bloqueia o resto. */
export function InfoRestaurante({ restaurante, horarios }: InfoRestauranteProps) {
  const endereco = linhasEndereco(restaurante);

  return (
    <section className="cartao" aria-label="Informações do restaurante">
      <dl className="info-loja">
        <div>
          <dt>Sobre</dt>
          <dd>{restaurante.restauranteDesc?.trim() || SEM_INFORMACAO}</dd>
        </div>
        <div>
          <dt>Endereço</dt>
          <dd>
            {endereco.length
              ? endereco.map((linha) => (
                  <span key={linha} className="info-loja__linha">
                    {linha}
                  </span>
                ))
              : SEM_INFORMACAO}
          </dd>
        </div>
        <div>
          <dt>Telefone</dt>
          <dd>{restaurante.restauranteTelefone?.trim() || SEM_INFORMACAO}</dd>
        </div>
        <div>
          <dt>Horários</dt>
          <dd>
            {horarios.isPending ? (
              <span className="mudo">Carregando horários…</span>
            ) : horarios.isError ? (
              <span className="mudo">Horários indisponíveis no momento.</span>
            ) : (
              <ListaHorarios horarios={horarios.data} />
            )}
          </dd>
        </div>
      </dl>
    </section>
  );
}
