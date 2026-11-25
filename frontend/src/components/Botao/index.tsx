import type { ButtonHTMLAttributes } from 'react';
import './styles.css';

type VarianteBotao = 'principal' | 'contorno';

interface BotaoProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: VarianteBotao;
  larguraTotal?: boolean;
}

export function Botao({
  variante = 'principal',
  larguraTotal = false,
  className,
  type = 'button',
  ...props
}: BotaoProps) {
  const classes = ['botao', `botao--${variante}`, larguraTotal && 'botao--largura-total', className]
    .filter(Boolean)
    .join(' ');
  return <button type={type} className={classes} {...props} />;
}
