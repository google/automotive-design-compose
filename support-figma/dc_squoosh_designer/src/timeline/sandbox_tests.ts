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

import { TestResult } from "../utils/SimpleTestRunner";

export async function runSandboxTests(): Promise<TestResult[]> {
  const results: TestResult[] = [];

  async function test(name: string, fn: () => Promise<void> | void) {
    try {
      await fn();
      results.push({ name, status: "passed" });
    } catch (e) {
      results.push({ name, status: "failed", error: e as Error });
    }
  }

  function expect(actual: unknown) {
    return {
      toBe: (expected: unknown) => {
        if (actual !== expected) {
          throw new Error(`Expected ${expected} but got ${actual}`);
        }
      },
      toBeTruthy: () => {
        if (!actual) throw new Error(`Expected ${actual} to be truthy`);
      },
      toBeFalsy: () => {
        if (actual) throw new Error(`Expected ${actual} to be falsy`);
      },
    };
  }

  await test("Create and remove rectangle", () => {
    const rect = figma.createRectangle();
    expect(rect).toBeTruthy();
    expect(rect.type).toBe("RECTANGLE");
    rect.remove();
    expect(rect.removed).toBe(true);
  });

  await test("Set and read plugin data", () => {
      const node = figma.createFrame();
      node.setPluginData("test-key", "test-value");
      expect(node.getPluginData("test-key")).toBe("test-value");
      node.remove();
  });

  return results;
}
