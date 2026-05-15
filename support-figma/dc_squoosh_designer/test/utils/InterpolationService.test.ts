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

import { InterpolationService } from '../../src/utils/InterpolationService';
import { Timeline, Keyframe, Node, SerializedNode, Variant, KeyframeTime } from '../../src/timeline/types';

// Mocks
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockKeyframe = (id: string, position: number, value: any, locked: boolean = false, easing: any = 'Linear'): Keyframe => ({
  id,
  position,
  value,
  locked,
  easing
});

const mockTimeline = (id: string, property: string, keyframes: Keyframe[]): Timeline => ({
  id,
  property,
  keyframes
});

const mockNode: Node = {
  id: 'node-1',
  figmaId: '1:1',
  name: 'Test Node',
  timelines: [],
  children: []
};

const mockSerializedNode: SerializedNode = {
  id: '1:1',
  name: 'Test Node',
  type: 'FRAME',
  x: 0,
  y: 0
};

const mockVariant: Variant = {
    name: 'Variant 1',
    animation: null
};

const mockKeyframeTimes: KeyframeTime[] = [
    { time: 0, name: 'Variant 1', index: 0 },
    { time: 1, name: 'Variant 2', index: 1 }
];

describe('InterpolationService', () => {
  describe('getInterpolatedValue', () => {
    it('should interpolate numeric values linearly', () => {
      const timeline = mockTimeline('tl-1', 'x', [
        mockKeyframe('kf-1', 0, 0),
        mockKeyframe('kf-2', 1, 100)
      ]);
      
      const val = InterpolationService.getInterpolatedValue(
        timeline,
        0.5, // time
        1, // totalTime
        mockKeyframeTimes,
        [mockSerializedNode, mockSerializedNode],
        [mockVariant, mockVariant],
        mockNode
      );
      expect(val).toBe(50);
    });

    it('should handle instant easing', () => {
      const timeline = mockTimeline('tl-1', 'x', [
        mockKeyframe('kf-1', 0, 0),
        mockKeyframe('kf-2', 1, 100, false, 'Instant')
      ]);
      
      const valAt0_5 = InterpolationService.getInterpolatedValue(
        timeline, 0.5, 1, mockKeyframeTimes, [], [], mockNode
      );
      expect(valAt0_5).toBe(0);

       const valAt1 = InterpolationService.getInterpolatedValue(
        timeline, 1, 1, mockKeyframeTimes, [], [], mockNode
      );
      expect(valAt1).toBe(100);
    });

     it('should interpolate colors', () => {
      const timeline = mockTimeline('tl-1', 'fills.0.solid', [
        mockKeyframe('kf-1', 0, '#000000'),
        mockKeyframe('kf-2', 1, '#ffffff')
      ]);
      
      const val = InterpolationService.getInterpolatedValue(
        timeline,
        0.5, 
        1, 
        mockKeyframeTimes,
        [], [], mockNode
      );
      // #000000 to #ffffff halfway is #808080
      expect(val).toBe('#808080');
    });
    it('should interpolate gradient stops from stringified JSON', () => {
        const startStops = JSON.stringify([
            { position: 0, color: '#000000' },
            { position: 1, color: '#000000' }
        ]);
        const endStops = JSON.stringify([
             { position: 0, color: '#ffffff' },
             { position: 1, color: '#ffffff' }
        ]);

        const timeline = mockTimeline('tl-1', 'fills.0.gradient.stops', [
            mockKeyframe('kf-1', 0, startStops),
            mockKeyframe('kf-2', 1, endStops)
        ]);

        const val = InterpolationService.getInterpolatedValue(
            timeline,
            0.5,
            1,
            mockKeyframeTimes,
            [], [], mockNode
        );
        
        expect(val).toBeDefined(); // Ensure val is not undefined
        expect(Array.isArray(val)).toBe(true);
        expect((val as Array<{ position: number; color: string }>)[0].color).toBe('#808080');
    });

    it('should handle invalid JSON gracefully for gradient stops', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        const startStops = "invalid-json";
        const endStops = JSON.stringify([
             { position: 0, color: '#ffffff' },
             { position: 1, color: '#ffffff' }
        ]);

        const timeline = mockTimeline('tl-1', 'fills.0.gradient.stops', [
            mockKeyframe('kf-1', 0, startStops),
            mockKeyframe('kf-2', 1, endStops)
        ]);

        const val = InterpolationService.getInterpolatedValue(
            timeline,
            0.5,
            1,
            mockKeyframeTimes,
            [], [], mockNode
        );
        
        // Should now return undefined on invalid JSON
        expect(val).toBeUndefined();
        expect(consoleSpy).toHaveBeenCalled();
        consoleSpy.mockRestore();
    });
  });
});
