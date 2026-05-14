/**
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { hexToRgb, rgbToHex, interpolateColor, colorToHex } from '../../src/utils/common';

describe('common utils', () => {
  describe('hexToRgb', () => {
    it('should convert valid hex to rgb', () => {
      expect(hexToRgb('#ffffff')).toEqual({ r: 255, g: 255, b: 255 });
      expect(hexToRgb('#000000')).toEqual({ r: 0, g: 0, b: 0 });
      expect(hexToRgb('#ff0000')).toEqual({ r: 255, g: 0, b: 0 });
    });

    it('should handle hex without hash', () => {
      expect(hexToRgb('ffffff')).toEqual({ r: 255, g: 255, b: 255 });
    });

    it('should return null for invalid hex', () => {
      expect(hexToRgb('invalid')).toBeNull();
    });
  });

  describe('rgbToHex', () => {
    it('should convert rgb to valid hex', () => {
      expect(rgbToHex(255, 255, 255)).toBe('#ffffff');
      expect(rgbToHex(0, 0, 0)).toBe('#000000');
      expect(rgbToHex(255, 0, 0)).toBe('#ff0000');
    });
  });

  describe('interpolateColor', () => {
    it('should interpolate between two colors', () => {
      expect(interpolateColor('#000000', '#ffffff', 0.5)).toBe('#808080');
      expect(interpolateColor('#000000', '#ffffff', 0)).toBe('#000000');
      expect(interpolateColor('#000000', '#ffffff', 1)).toBe('#ffffff');
    });
  });

  describe('colorToHex', () => {
      it('should convert 0-1 rgb object to hex', () => {
          expect(colorToHex({r: 1, g: 1, b: 1})).toBe('#ffffff');
          expect(colorToHex({r: 0, g: 0, b: 0})).toBe('#000000');
          expect(colorToHex({r: 1, g: 0, b: 0})).toBe('#ff0000');
      });
  });
});
