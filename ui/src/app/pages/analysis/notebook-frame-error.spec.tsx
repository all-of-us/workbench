import { mount } from 'enzyme';

import { waitForFakeTimersAndUpdate } from 'testing/react-test-helpers';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import { SecuritySuspendedMessage } from './notebook-frame-error';

describe('SecuritySuspendedMessage', () => {
  const now = new Date('2000-01-01 03:00:00');
  const nowMinus10Minutes = new Date('2000-01-01 02:55:00');
  const nowPlusFiveMinutes = new Date('2000-01-01 03:05:00');

  const component = (suspendedUntil: Date) => {
    return mount(
      <SecuritySuspendedMessage
        error={
          new ComputeSecuritySuspendedError({
            suspendedUntil: suspendedUntil.toISOString(),
          })
        }
      />
    );
  };

  beforeEach(() => {
    jest.useFakeTimers('modern').setSystemTime(now.getTime());
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should show pending suspension', async () => {
    const wrapper = component(nowPlusFiveMinutes);
    expect(wrapper).toBeTruthy();

    expect(wrapper.text()).toContain('in 5 minutes');
  });

  it('should indicate reload for past suspension', async () => {
    const wrapper = component(nowMinus10Minutes);
    expect(wrapper).toBeTruthy();

    expect(wrapper.text()).toContain('Reload the page to continue');
  });

  it('should update estimate over time', async () => {
    const wrapper = component(nowPlusFiveMinutes);
    expect(wrapper).toBeTruthy();

    await waitForFakeTimersAndUpdate(wrapper);
    expect(wrapper.text()).toContain('in 5 minutes');

    jest.setSystemTime(nowPlusFiveMinutes);
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.text()).toContain('Reload the page to continue');
  });
});
