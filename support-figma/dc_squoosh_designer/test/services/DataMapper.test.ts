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

import { DataMapper } from "../../src/services/DataMapper";
import { Variant } from "../../src/timeline/types";
import { resolveVariantCustomKeyframeData } from "../../src/timeline/utils";

describe("DataMapper", () => {
  describe("calculateKeyframeData", () => {
    it("should calculate keyframe times correctly for multiple variants with delays and durations", () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const variants: Variant[] = [
        {
          name: "State 1",
          animation: {
            spec: {
              initial_delay: { secs: 1, nanos: 0 }, // 1s
              animation: {
                Smooth: {
                  duration: { secs: 2, nanos: 0 }, // 2s
                  easing: "Linear",
                },
              },
            },
          } as any,
        },
        {
          name: "State 2",
          animation: {
            spec: {
              initial_delay: { secs: 0, nanos: 500000000 }, // 0.5s
              animation: {
                Smooth: {
                  duration: { secs: 1, nanos: 0 }, // 1s
                  easing: "EaseIn",
                },
              },
            },
          } as any,
        },
        {
          name: "State 3",
          animation: null,
        },
      ];

      const result = DataMapper.calculateKeyframeData(variants);

      // Loop 1: Index 0 -> 1. Delay 1s, Duration 2s. Time increases by 3.
      // Loop 2: Index 1 -> 2. Delay 0.5s, Duration 1s. Time increases by 1.5.
      // Total time should be 4.5.

      expect(result.keyframeTimes).toHaveLength(4); // 3 variants + loop back
      expect(result.keyframeTimes[0].time).toBe(0);
      expect(result.keyframeTimes[1].time).toBe(1.5);
      expect(result.keyframeTimes[2].time).toBe(1.5);
      expect(result.totalTime).toBe(4.5);
      expect(result.keyframeTimes[3].time).toBe(4.5);
      expect(result.keyframeTimes[3].isLoop).toBe(true);
    });

    it("should assign default 0.3s duration for each variant in a sequence without animation specs", () => {
      const variants: Variant[] = [
        { name: "P", animation: null },
        { name: "R", animation: null },
        { name: "N", animation: null },
        { name: "D", animation: null },
      ];
      const result = DataMapper.calculateKeyframeData(variants);
      expect(result.keyframeTimes).toHaveLength(5);
      expect(result.keyframeTimes[0].time).toBeCloseTo(0);
      expect(result.keyframeTimes[1].time).toBeCloseTo(0.3);
      expect(result.keyframeTimes[2].time).toBeCloseTo(0.6);
      expect(result.keyframeTimes[3].time).toBeCloseTo(0.9);
      expect(result.totalTime).toBeCloseTo(1.2);
    });

    it("should handle single variant", () => {
      const variants: Variant[] = [{ name: "Single", animation: null }];
      const result = DataMapper.calculateKeyframeData(variants);
      expect(result.keyframeTimes).toHaveLength(2);
      expect(result.keyframeTimes[0].name).toBe("Single");
      expect(result.keyframeTimes[0].time).toBe(0);
      expect(result.keyframeTimes[0].index).toBe(0);
      expect(result.keyframeTimes[1].name).toBe("Single");
      expect(result.keyframeTimes[1].time).toBe(1); // Defaults to 1
      expect(result.keyframeTimes[1].index).toBe(0);
      expect(result.keyframeTimes[1].isLoop).toBe(true);
      expect(result.totalTime).toBe(1); // Defaults to 1
    });

    it("should respect activeTransitionsMap when selecting custom transition spec", () => {
      const variants: Variant[] = [
        {
          name: "State A",
          animation: {
            transitions: [
              {
                from: "*",
                to: "State A",
                name: "Default",
                spec: {
                  animation: {
                    Smooth: {
                      duration: { secs: 0, nanos: 300000000 },
                      easing: "Linear",
                    },
                  },
                },
              },
              {
                from: "*",
                to: "State A",
                name: "SportMode",
                spec: {
                  animation: {
                    Smooth: {
                      duration: { secs: 1, nanos: 500000000 },
                      easing: "EaseOut",
                    },
                  },
                },
              },
            ],
          } as any,
        },
        {
          name: "State B",
          animation: null,
        },
      ];

      const defaultResult = DataMapper.calculateKeyframeData(variants, {
        "State A": 0,
      });
      const customResult = DataMapper.calculateKeyframeData(variants, {
        "State A": 1,
      });

      expect(defaultResult.totalTime).toBeCloseTo(0.3); // 0.3s default
      expect(customResult.totalTime).toBeCloseTo(1.5); // 1.5s custom
    });

    it("should override anim.spec when anim.transitions is present and custom transition is selected", () => {
      const variants: Variant[] = [
        {
          name: "State A",
          animation: {
            spec: {
              animation: {
                Smooth: {
                  duration: { secs: 0, nanos: 300000000 },
                  easing: "Linear",
                },
              },
            },
            transitions: [
              {
                from: "*",
                to: "State A",
                name: "Default",
                spec: {
                  animation: {
                    Smooth: {
                      duration: { secs: 0, nanos: 300000000 },
                      easing: "Linear",
                    },
                  },
                },
              },
              {
                from: "*",
                to: "State A",
                name: "SportMode",
                spec: {
                  animation: {
                    Smooth: {
                      duration: { secs: 1, nanos: 500000000 },
                      easing: "EaseOut",
                    },
                  },
                },
              },
            ],
          } as any,
        },
        {
          name: "State B",
          animation: null,
        },
      ];

      const customResult = DataMapper.calculateKeyframeData(variants, {
        "State A": 1,
      });
      expect(customResult.totalTime).toBeCloseTo(1.5);
    });

    it("should resolve per-transition customKeyframeData when specified on transition", () => {
      const variants: Variant[] = [
        {
          name: "State A",
          animation: {
            customKeyframeData: { "node1-x": "0:0;1:100" },
            transitions: [
              {
                from: "*",
                to: "State A",
                name: "Default",
                customKeyframeData: { "node1-x": "0:0;1:100" },
              },
              {
                from: "*",
                to: "State A",
                name: "SportMode",
                customKeyframeData: { "node1-y": "0:50;1:200" },
              },
            ],
          } as any,
        },
        { name: "State B", animation: null },
      ];

      const kfDefault = resolveVariantCustomKeyframeData(
        variants[0].animation,
        "State A",
        { "State A": 0 },
      );
      const kfSport = resolveVariantCustomKeyframeData(
        variants[0].animation,
        "State A",
        { "State A": 1 },
      );

      expect(kfDefault).toEqual({ "node1-x": "0:0;1:100" });
      expect(kfSport).toEqual({ "node1-y": "0:50;1:200" });
    });
  });
});
