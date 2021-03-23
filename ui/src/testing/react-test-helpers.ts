import {ReactWrapper} from 'enzyme';
import {act} from 'react-dom/test-utils';

// This file is necessary because angular imports complain if there
// is no zone, regardless of whether the imports are used.
// The error is from:
//   import {ComponentFixture, tick} from '@angular/core/testing';
// And fails with:
//   ReferenceError: Zone is not defined
export async function waitOneTickAndUpdate(wrapper: ReactWrapper) {
  const waitImmediate = () => new Promise<void>(resolve => setImmediate(resolve));
  wrapper.update();
  await act(waitImmediate);
}

export async function waitForFakeTimersAndUpdate(wrapper: ReactWrapper) {
  act(() => {
    jest.runOnlyPendingTimers();
  });
  await waitOneTickAndUpdate(wrapper);
}
