function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function loadImage(dataUrl: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new window.Image();
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = dataUrl;
  });
}

/**
 * Reads an image file, downscales it to fit within maxDimension on its
 * longest side, and re-encodes as JPEG. Storage is localStorage, which has
 * a hard ~5-10MB per-origin ceiling shared across the whole app, so a
 * handful of full-resolution phone photos (each easily 3-8MB as base64)
 * would exhaust it almost immediately.
 */
export async function fileToCompressedDataUrl(
  file: File,
  maxDimension = 800,
  quality = 0.82
): Promise<string> {
  const original = await fileToDataUrl(file);
  const img = await loadImage(original);

  const scale = Math.min(1, maxDimension / Math.max(img.width, img.height));
  const width = Math.round(img.width * scale);
  const height = Math.round(img.height * scale);

  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  if (!ctx) return original;
  ctx.drawImage(img, 0, 0, width, height);

  const compressed = canvas.toDataURL("image/jpeg", quality);
  // Fall back to the original if compression somehow produced something
  // larger (can happen with already-tiny or already-compressed images).
  return compressed.length < original.length ? compressed : original;
}
