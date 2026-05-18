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

import { DataMapper } from '../../src/services/DataMapper';
import { InterpolationService } from '../../src/utils/InterpolationService';
import { Variant, SerializedNode, AnimationNode, Keyframe } from '../../src/timeline/types';

const createNode = (id: string, props: Partial<SerializedNode> = {}): SerializedNode => ({
    id,
    name: id,
    type: 'FRAME',
    x: 0,
    y: 0,
    visible: true,
    opacity: 1,
    fills: [],
    strokes: [],
    strokeWeight: 0,
    cornerRadius: 0,
    absoluteTransform: [[1, 0, 0], [0, 1, 0]],
    ...props
} as SerializedNode);

describe('Animation Logic End-to-End', () => {
    const child1V1 = createNode('Child1', { x: 0, opacity: 0, absoluteTransform: [[1, 0, 0], [0, 1, 0]] });
    const child1V2 = createNode('Child1', { x: 100, opacity: 1, absoluteTransform: [[1, 0, 100], [0, 1, 0]] });

    const root1 = createNode('State1', { children: [child1V1] });
    const root2 = createNode('State2', { children: [child1V2] });

    const variants: Variant[] = [
        {
            name: 'State1',
            animation: null // Loop back or initial state properties
        },
        {
            name: 'State2',
            animation: { // Defines transition FROM State1 TO State2
                spec: {
                    initial_delay: { secs: 0, nanos: 0 },
                    animation: {
                        Smooth: {
                            duration: { secs: 1, nanos: 0 },
                            easing: 'EaseIn' 
                        }
                    },
                    interrupt_type: 'None'
                }
            }
        }
    ];

    const serializedVariants = [root1, root2];

    const animationData = DataMapper.transformDataToAnimationData(variants, serializedVariants);
    const { keyframeTimes, totalTime } = DataMapper.calculateKeyframeData(variants);

    const rootNode = animationData.nodes[0];
    const childNode = rootNode.children.find(n => n.name === 'Child1');
    
    const xTimeline = childNode?.timelines.find(t => t.property === 'x');
    const opacityTimeline = childNode?.timelines.find(t => t.property === 'opacity');

    test('Base Variant Easing is applied', () => {
        // State1 -> State2 takes 1s.
        // State2 -> State1 takes 0s (default).
        // Total time = 1s.
        // Test at 0.5s (50%).
        
        expect(totalTime).toBe(1);
        
        const val = InterpolationService.getInterpolatedValue(
            xTimeline!,
            0.5,
            totalTime,
            keyframeTimes,
            serializedVariants,
            variants,
            { name: 'Child1' } as any
        );
        
        // EaseIn(0.5) = 0.25. 
        // 0 -> 100. Expected 25.
        expect(val).toBeCloseTo(25); 
    });

    test('Property Override: Linear', () => {
        // State1 -> State2 segment ends at index 1.
        const kf2 = opacityTimeline!.keyframes.find(k => Math.abs(k.position - (keyframeTimes[1].time / totalTime)) < 0.001);
        expect(kf2).toBeDefined();
        
        // Override to Linear
        kf2!.easing = 'Linear';

        const val = InterpolationService.getInterpolatedValue(
            opacityTimeline!,
            0.5,
            totalTime,
            keyframeTimes,
            serializedVariants,
            variants,
            { name: 'Child1' } as any
        );
        
        // Linear(0.5) = 0.5.
        // 0 -> 1. Expected 0.5.
        expect(val).toBeCloseTo(0.5);
    });

    test('Custom Keyframe Insertion', () => {
        // Add custom KF at 0.5 (50% time, so 0.5s).
        // Value 80. Easing Linear (for next segment).
        // Segment 1: 0 -> 0.5 (easing defined by the custom keyframe at 0.5).
        
        xTimeline!.keyframes.push({
            id: 'custom-1',
            position: 0.5,
            value: 80,
            locked: false,
            easing: 'Linear' 
        });
        xTimeline!.keyframes.sort((a, b) => a.position - b.position);
        
        // 0 -> 0.5 segment. End KF is custom (0.5). Easing 'Linear'.
        // Query at 0.25 (halfway of 0.5 duration). Progress 0.5.
        // Linear(0.5) = 0.5.
        // 0 -> 80. 
        // Expected 40.
        const val1 = InterpolationService.getInterpolatedValue(
            xTimeline!,
            0.25,
            totalTime,
            keyframeTimes,
            serializedVariants,
            variants,
            { name: 'Child1' } as any
        );
        expect(val1).toBeCloseTo(40);

        // 0.5 -> 1.0 segment. End KF is State2 (1.0).
        // Easing Inherit -> EaseIn (from Variant).
        // 80 -> 100. Delta 20.
        // Query at 0.75 (halfway). Progress 0.5.
        // EaseIn(0.5) = 0.25.
        // 80 + 20 * 0.25 = 85.
        
        const val2 = InterpolationService.getInterpolatedValue(
            xTimeline!,
            0.75,
            totalTime,
            keyframeTimes,
            serializedVariants,
            variants,
            { name: 'Child1' } as any
        );
        expect(val2).toBeCloseTo(85);
    });
});