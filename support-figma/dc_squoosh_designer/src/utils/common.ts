/**
 * Common utility functions and constants.
 */

export const EPSILON = 0.0001;

/**
 * Converts a hex color string to an RGB object.
 * @param hex The hex color string (e.g., "#RRGGBB" or "RRGGBB").
 * @returns An object with r, g, b values (0-255) or null if invalid.
 */
export function hexToRgb(hex: string): { r: number; g: number; b: number } | null {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16),
      }
    : null;
}

/**
 * Converts RGB values to a hex color string.
 * @param r Red value (0-255).
 * @param g Green value (0-255).
 * @param b Blue value (0-255).
 * @returns The hex color string (e.g., "#RRGGBB").
 */
export function rgbToHex(r: number, g: number, b: number): string {
  return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
}

/**
 * Interpolates between two hex colors.
 * @param color1 The start hex color.
 * @param color2 The end hex color.
 * @param factor The interpolation factor (0-1).
 * @returns The interpolated hex color.
 */
export function interpolateColor(
  color1: string,
  color2: string,
  factor: number,
): string {
  const rgb1 = hexToRgb(color1);
  const rgb2 = hexToRgb(color2);
  if (!rgb1 || !rgb2) return color1; // Fallback

  const r = Math.round(rgb1.r + factor * (rgb2.r - rgb1.r));
  const g = Math.round(rgb1.g + factor * (rgb2.g - rgb1.g));
  const b = Math.round(rgb1.b + factor * (rgb2.b - rgb1.b));

  return rgbToHex(r, g, b);
}

/**
 * Converts a color object with 0-1 range to a hex string.
 * @param color The color object with r, g, b values between 0 and 1.
 * @returns The hex color string (e.g., "#RRGGBB").
 */
export function colorToHex(color: { r: number; g: number; b: number }): string {
    const toHex = (c: number) => {
      const i = Math.round(c * 255);
      const hex = i.toString(16);
      return hex.length == 1 ? "0" + hex : hex;
    };
    return `#${toHex(color.r)}${toHex(color.g)}${toHex(color.b)}`;
  }

/**
 * Multiplies two 2D affine transform matrices.
 * Matrix A * Matrix B.
 * @param m1 The first matrix [[a1, c1, tx1], [b1, d1, ty1]].
 * @param m2 The second matrix [[a2, c2, tx2], [b2, d2, ty2]].
 * @returns The result matrix.
 */
export function multiplyMatrix(m1: number[][], m2: number[][]): number[][] {
  const [a1, c1, tx1] = m1[0];
  const [b1, d1, ty1] = m1[1];
  const [a2, c2, tx2] = m2[0];
  const [b2, d2, ty2] = m2[1];

  return [
    [
      a1 * a2 + c1 * b2,
      a1 * c2 + c1 * d2,
      a1 * tx2 + c1 * ty2 + tx1,
    ],
    [
      b1 * a2 + d1 * b2,
      b1 * c2 + d1 * d2,
      b1 * tx2 + d1 * ty2 + ty1,
    ],
  ];
}

/**
 * Inverts a 2D affine transform matrix.
 * @param m The matrix to invert [[a, c, tx], [b, d, ty]].
 * @returns The inverted matrix, or null if not invertible.
 */
export function invertMatrix(m: number[][]): number[][] | null {
  const [a, c, tx] = m[0];
  const [b, d, ty] = m[1];

  const det = a * d - b * c;
  if (Math.abs(det) < 1e-6) return null;

  const invDet = 1 / det;

  return [
    [
      d * invDet,
      -c * invDet,
      (c * ty - d * tx) * invDet,
    ],
    [
      -b * invDet,
      a * invDet,
      (b * tx - a * ty) * invDet,
    ],
  ];
}

/**
 * Decomposes a 2D affine transform matrix into translation, rotation, and scale.
 * @param matrix The 2D matrix as [[a, c, e], [b, d, f]].
 * @returns The decomposed transform.
 */
export function decomposeMatrix(matrix: number[][]) {
  const [a, c, e] = matrix[0];
  const [b, d, f] = matrix[1];
  const delta = a * d - b * c;
  const translateX = e,
    translateY = f;
  let angle = 0,
    scaleX = 1,
    scaleY = 1;
  if (a !== 0 || b !== 0) {
    const r = Math.sqrt(a * a + b * b);
    angle = b > 0 ? Math.acos(a / r) : -Math.acos(a / r);
    scaleX = r;
    scaleY = delta / r;
  } else if (c !== 0 || d !== 0) {
    const s = Math.sqrt(c * c + d * d);
    angle = Math.PI / 2 - (d > 0 ? Math.acos(-c / s) : -Math.acos(c / s));
    scaleX = delta / s;
    scaleY = s;
  }
  return {
    translateX,
    translateY,
    angle: angle * (180 / Math.PI),
    scaleX,
    scaleY,
  };
}
