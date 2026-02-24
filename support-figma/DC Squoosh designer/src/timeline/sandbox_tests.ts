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
