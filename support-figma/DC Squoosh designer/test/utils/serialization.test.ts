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

        // Let's print the actual serialized string to confirm its format
        console.log("Serialized string:", serialized);

        // Assert the format matches TargetEasing|Keyframe1;Keyframe2;...
        expect(serialized).toContain('|');
        expect(serialized.split('|')[0]).toBe(targetEasing);
        expect(serialized.split('|')[1].split(';').length).toBe(3);

        const deserialized = deserializeKeyframes(serialized);
        expect(deserialized.targetEasing).toBe(targetEasing);
        expect(deserialized.keyframes).toEqual(keyframes);
    });
});
