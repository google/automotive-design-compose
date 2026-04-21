/**
 * Simple event emitter implementation for managing custom events.
 * Allows components to subscribe to and trigger events without direct coupling.
 */
export class EventEmitter {
  private events: { [key: string]: ((...args: unknown[]) => void)[] } = {};

  /**
   * Registers an event listener.
   * @param event The name of the event to listen for.
   * @param callback The function to call when the event is emitted.
   */
  public on(event: string, callback: (...args: unknown[]) => void): void {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(callback);
  }

  /**
   * Removes an event listener.
   * @param event The name of the event.
   * @param callback The callback function to remove.
   */
  public off(event: string, callback: (...args: unknown[]) => void): void {
    if (!this.events[event]) {
      return;
    }
    this.events[event] = this.events[event].filter((cb) => cb !== callback);
  }

  /**
   * Emits an event, triggering all registered listeners.
   * @param event The name of the event to emit.
   * @param args Arguments to pass to the listeners.
   */
  public emit(event: string, ...args: unknown[]): void {
    if (!this.events[event]) {
      return;
    }
    this.events[event].forEach((callback) => callback(...args));
  }
}
