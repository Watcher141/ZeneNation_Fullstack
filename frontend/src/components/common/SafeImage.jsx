import { useState } from 'react';
import { MdImage } from 'react-icons/md';

/**
 * SafeImage renders an <img>, but gracefully displays a sleek fallback placeholder
 * if the image URL is missing, invalid, or fails to load (e.g. 401, 404, deleted account).
 */
export default function SafeImage({
  src,
  alt = '',
  className = '',
  style = {},
  loading = 'lazy',
  fallbackIconSize = 28,
  ...props
}) {
  const [hasError, setHasError] = useState(false);

  if (!src || hasError) {
    return (
      <div
        className={`safe-img-fallback ${className}`}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'var(--bg-card, #1a1a24)',
          width: '100%',
          height: '100%',
          minHeight: '100%',
          color: 'var(--text-muted, #71717a)',
          borderRadius: style.borderRadius || undefined,
          ...style,
        }}
      >
        <MdImage size={fallbackIconSize} />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className={className}
      style={style}
      loading={loading}
      onError={() => setHasError(true)}
      {...props}
    />
  );
}

