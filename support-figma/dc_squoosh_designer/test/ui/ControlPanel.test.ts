import { ControlPanel } from '../../src/ui/ControlPanel';
import { PlaybackController } from '../../src/timeline/PlaybackController';

// Mock PlaybackController
jest.mock('../../src/timeline/PlaybackController');

describe('ControlPanel', () => {
  let controlPanel: ControlPanel;
  let playbackController: PlaybackController;
  let mockElements: { [key: string]: HTMLElement };

  beforeEach(() => {
    // Setup DOM mocks
    mockElements = {
      'play-button': document.createElement('button'),
      'save-button': document.createElement('button'),
      'discard-button': document.createElement('button'),
      'ping-button': document.createElement('button'),
      'clear-preview-button': document.createElement('button'),
      'export-button': document.createElement('button'),
      'reset-button': document.createElement('button'),
      'select-preview-frame-button': document.createElement('button'),
      'keyframe-select': document.createElement('select'),
      'initial-delay': document.createElement('input'),
      'duration': document.createElement('input'),
      'easing': document.createElement('select'),
      'interrupt-type': document.createElement('select'),
      'continue-checkbox': document.createElement('input'),
      'variant-name-display': document.createElement('div'),
      'throttle-updates-checkbox': document.createElement('input'),
    };

    // Setup specific checkbox
    (mockElements['throttle-updates-checkbox'] as HTMLInputElement).type = 'checkbox';

    // Populate select options for jsdom to handle value property
    ['Linear', 'EaseIn', 'EaseOut'].forEach(val => {
        const opt = document.createElement('option');
        opt.value = val;
        mockElements['easing'].appendChild(opt);
    });
    ['None', 'Immediate', 'Finish'].forEach(val => {
        const opt = document.createElement('option');
        opt.value = val;
        mockElements['interrupt-type'].appendChild(opt);
    });

    jest.spyOn(document, 'getElementById').mockImplementation((id: string) => {
      return mockElements[id] || null;
    });

    // Manually mock methods needed in constructor
    const MockPlaybackController = PlaybackController as jest.MockedClass<typeof PlaybackController>;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    playbackController = new MockPlaybackController({} as any);
    
    // Ensure 'on' method is mockable
    playbackController.on = jest.fn();
    
    controlPanel = new ControlPanel(playbackController);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should initialize properly', () => {
    expect(controlPanel).toBeDefined();
    expect(playbackController.on).toHaveBeenCalledWith('play', expect.any(Function));
    expect(playbackController.on).toHaveBeenCalledWith('pause', expect.any(Function));
  });

  it('should toggle play/pause on button click', () => {
    const playBtn = mockElements['play-button'] as HTMLButtonElement;
    
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (playbackController as any).isPlaying = false;
    playBtn.click();
    expect(playbackController.play).toHaveBeenCalled();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (playbackController as any).isPlaying = true;
    playBtn.click();
    expect(playbackController.pause).toHaveBeenCalled();
  });

  it('should emit save event with settings', () => {
    const saveBtn = mockElements['save-button'] as HTMLButtonElement;
    const saveSpy = jest.fn();
    controlPanel.on('save', saveSpy);

    // Set inputs
    (mockElements['initial-delay'] as HTMLInputElement).value = '1.5';
    (mockElements['duration'] as HTMLInputElement).value = '2.0';
    (mockElements['easing'] as HTMLSelectElement).value = 'EaseIn';
    (mockElements['interrupt-type'] as HTMLSelectElement).value = 'Immediate';

    saveBtn.click();

    expect(saveSpy).toHaveBeenCalledWith({
      initialDelay: 1.5,
      duration: 2.0,
      easing: 'EaseIn',
      interruptType: 'Immediate'
    });
  });

  it('should update inputs when setVariant is called', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const variant: any = {
          name: 'Test Variant',
          animation: {
              spec: {
                  initial_delay: { secs: 1, nanos: 500000000 }, // 1.5s
                  animation: {
                      Smooth: {
                          duration: { secs: 0, nanos: 500000000 }, // 0.5s
                          easing: 'EaseOut'
                      }
                  },
                  interrupt_type: 'Finish'
              }
          }
      };

      controlPanel.setVariant(variant);

      expect((mockElements['initial-delay'] as HTMLInputElement).value).toBe('1.50');
      expect((mockElements['duration'] as HTMLInputElement).value).toBe('0.50');
      expect((mockElements['easing'] as HTMLSelectElement).value).toBe('EaseOut');
      expect((mockElements['interrupt-type'] as HTMLSelectElement).value).toBe('Finish');
      expect(mockElements['variant-name-display'].textContent).toBe('Test Variant');
  });

  it('should disable save button initially when variant set', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const variant: any = {
        name: 'Test Variant',
        animation: { spec: {} }
      };
      controlPanel.setVariant(variant);
      expect((mockElements['save-button'] as HTMLButtonElement).disabled).toBe(true);
  });

  it('should enable save button when properties change', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const variant: any = {
        name: 'Test Variant',
        animation: { spec: {
            initial_delay: { secs: 0, nanos: 0 },
            animation: { Smooth: { duration: { secs: 0, nanos: 300000000 }, easing: 'Linear' } }
        } }
      };
      controlPanel.setVariant(variant);
      
      // Change duration
      (mockElements['duration'] as HTMLInputElement).value = '0.5';
      // Trigger input event
      (mockElements['duration'] as HTMLInputElement).dispatchEvent(new Event('input'));

      expect((mockElements['save-button'] as HTMLButtonElement).disabled).toBe(false);
  });
});
