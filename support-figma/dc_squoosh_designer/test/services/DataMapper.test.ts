import { DataMapper } from '../../src/services/DataMapper';
import { Variant } from '../../src/timeline/types';

describe('DataMapper', () => {
  describe('calculateKeyframeData', () => {
    it('should calculate keyframe times correctly for multiple variants with delays and durations', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const variants: Variant[] = [
        {
          name: 'State 1',
          animation: {
            spec: {
              initial_delay: { secs: 1, nanos: 0 }, // 1s
              animation: {
                Smooth: {
                  duration: { secs: 2, nanos: 0 }, // 2s
                  easing: 'Linear'
                }
              }
            }
          } as any
        },
        {
          name: 'State 2',
          animation: {
            spec: {
              initial_delay: { secs: 0, nanos: 500000000 }, // 0.5s
              animation: {
                Smooth: {
                  duration: { secs: 1, nanos: 0 }, // 1s
                  easing: 'EaseIn'
                }
              }
            }
          } as any
        },
        {
          name: 'State 3',
          animation: null
        }
      ];

      const result = DataMapper.calculateKeyframeData(variants);

      // Loop 1: Index 0 -> 1. Delay 1s, Duration 2s. Time increases by 3.
      // Loop 2: Index 1 -> 2. Delay 0.5s, Duration 1s. Time increases by 1.5.
      // Total time should be 4.5.
      
      expect(result.keyframeTimes).toHaveLength(4); // 3 variants + loop back
      expect(result.keyframeTimes[0].time).toBe(0);
      expect(result.keyframeTimes[1].time).toBe(1.5);
      expect(result.keyframeTimes[2].time).toBe(1.5); // Because State 3 has no animation spec to move from State 2? No, State 2 defines transition TO State 3? 
      // Actually, the logic uses `animSourceVariant` which is `variants[index+1]`.
      // The animation spec is ON the target variant usually in Figma (smart animate).
      // So to go 1 -> 2, we look at 2's spec. 
      
      expect(result.totalTime).toBe(4.5);
      expect(result.keyframeTimes[3].time).toBe(4.5);
      expect(result.keyframeTimes[3].isLoop).toBe(true);
    });

    it('should handle single variant', () => {
      const variants: Variant[] = [{ name: 'Single', animation: null }];
      const result = DataMapper.calculateKeyframeData(variants);
      expect(result.keyframeTimes).toHaveLength(2);
      expect(result.keyframeTimes[0].name).toBe('Single');
      expect(result.keyframeTimes[0].time).toBe(0);
      expect(result.keyframeTimes[0].index).toBe(0);
      expect(result.keyframeTimes[1].name).toBe('Single');
      expect(result.keyframeTimes[1].time).toBe(1); // Defaults to 1
      expect(result.keyframeTimes[1].index).toBe(0);
      expect(result.keyframeTimes[1].isLoop).toBe(true);
      expect(result.totalTime).toBe(1); // Defaults to 1
    });
  });
});
