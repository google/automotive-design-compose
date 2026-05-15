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

import { serializeKeyframes, deserializeKeyframes } from '../../src/timeline/serialization';

describe('serialization', () => {
    it('serializes and deserializes keyframes correctly', () => {
        const keyframes = [
            { fraction: 0, value: 100, easing: 'Linear' as const },
            { fraction: 0.5, value: { x: 10, y: 20 }, easing: 'EaseIn' as const },
            { fraction: 1, value: 'test', easing: 'EaseOut' as const }
        ];
        const targetEasing = 'EaseInOut';

        const serialized = serializeKeyframes(keyframes, targetEasing);

        // Parse as JSON to assert format is standard JSON
        const parsedNode = JSON.parse(serialized);
        expect(parsedNode.targetEasing).toBe(targetEasing);
        expect(parsedNode.keyframes.length).toBe(3);

        const deserialized = deserializeKeyframes(serialized);
        expect(deserialized.targetEasing).toBe(targetEasing);
        expect(deserialized.keyframes).toEqual(keyframes);
    });
    
    it('deserializes legacy format correctly', () => {
        const legacyString = "EaseInOut|0,MTAw,Linear;0.5,eyJ4IjoxMCwieSI6MjB9,EaseIn;1,InRlc3Qi,EaseOut";
        const deserialized = deserializeKeyframes(legacyString);
        expect(deserialized.targetEasing).toBe('EaseInOut');
        expect(deserialized.keyframes.length).toBe(3);
        expect(deserialized.keyframes[0].value).toBe(100);
        expect(deserialized.keyframes[1].value).toEqual({x: 10, y: 20});
        expect(deserialized.keyframes[2].value).toBe('test');
    });
});
