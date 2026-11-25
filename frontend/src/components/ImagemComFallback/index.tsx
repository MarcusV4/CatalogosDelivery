import { useState, type ReactNode } from 'react';

interface ImagemComFallbackProps {
  src: string | null;
  alt: string;
  className?: string;
  /** Mostrado quando não há URL ou a imagem não carrega. */
  fallback: ReactNode;
}

/** Imagem com texto alternativo que troca pelo `fallback` se a URL faltar ou falhar. */
export function ImagemComFallback({ src, alt, className, fallback }: ImagemComFallbackProps) {
  const [urlComFalha, setUrlComFalha] = useState<string | null>(null);

  if (!src?.trim() || src === urlComFalha) {
    return fallback;
  }
  return (
    <img
      src={src}
      alt={alt}
      className={className}
      loading="lazy"
      decoding="async"
      onError={() => setUrlComFalha(src)}
    />
  );
}
