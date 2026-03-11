export type TestStatus = 'passed' | 'failed';

export interface TestResult {
  name: string;
  status: TestStatus;
  error?: Error;
}

export class SimpleTestRunner {
  private tests: { name: string; fn: () => void | Promise<void> }[] = [];
  private results: TestResult[] = [];
  private logContainer: HTMLElement | null = null;

  constructor(logContainerId?: string) {
    if (logContainerId) {
      this.logContainer = document.getElementById(logContainerId);
    }
  }

  describe(name: string, fn: () => void) {
    // Simply run the describe block to register inner 'it's
    // Ideally we'd track hierarchy but flat is fine for now.
    fn(); 
  }

  it(name: string, fn: () => void | Promise<void>) {
    this.tests.push({ name, fn });
  }

  async run() {
    this.results = [];
    this.clearLog();
    this.log('Running tests...', 'info');

    for (const test of this.tests) {
      try {
        await test.fn();
        this.results.push({ name: test.name, status: 'passed' });
        this.log(`✅ ${test.name}`, 'pass');
      } catch (e) {
        this.results.push({ name: test.name, status: 'failed', error: e as Error });
        this.log(`❌ ${test.name}`, 'fail');
        console.error(e);
        this.log(`   ${(e as Error).message}`, 'error-detail');
      }
    }
    
    const passed = this.results.filter(r => r.status === 'passed').length;
    const failed = this.results.filter(r => r.status === 'failed').length;
    this.log(`Done. Passed: ${passed}, Failed: ${failed}`, failed > 0 ? 'fail' : 'pass');
  }

  expect(actual: unknown) {
    return {
      toBe: (expected: unknown) => {
        if (actual !== expected) {
          throw new Error(`Expected ${expected} but got ${actual}`);
        }
      },
      toEqual: (expected: unknown) => {
         if (JSON.stringify(actual) !== JSON.stringify(expected)) {
            throw new Error(`Expected ${JSON.stringify(expected)} but got ${JSON.stringify(actual)}`);
         }
      },
      toBeTruthy: () => {
        if (!actual) {
          throw new Error(`Expected ${actual} to be truthy`);
        }
      },
      toBeFalsy: () => {
        if (actual) {
          throw new Error(`Expected ${actual} to be falsy`);
        }
      },
      toContain: (expected: unknown) => {
          if (Array.isArray(actual)) {
              if (!actual.includes(expected)) throw new Error(`Expected array to contain ${expected}`);
          } else if (typeof actual === 'string') {
              if (!actual.includes(expected as string)) throw new Error(`Expected string to contain ${expected}`);
          } else {
              throw new Error(`Expected ${actual} to contain ${expected}`);
          }
      }
    };
  }

  private log(message: string, type: 'info' | 'pass' | 'fail' | 'error-detail') {
    if (!this.logContainer) {
        return;
    }
    const div = document.createElement('div');
    div.textContent = message;
    div.className = `test-log-${type}`;
    div.style.fontFamily = 'monospace';
    div.style.fontSize = '12px';
    if (type === 'pass') div.style.color = 'green';
    if (type === 'fail') div.style.color = 'red';
    if (type === 'error-detail') {
        div.style.color = 'red';
        div.style.marginLeft = '20px';
    }
    this.logContainer.appendChild(div);
  }

  private clearLog() {
    if (this.logContainer) {
      this.logContainer.innerHTML = '';
    }
  }
}
