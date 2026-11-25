import { describe, expect, it } from 'vitest';
import { caminhos } from './caminhos';

describe('caminhos', () => {
  it('monta o caminho da página do restaurante', () => {
    expect(caminhos.restaurante(10)).toBe('/restaurante/10');
  });
});
