import {ReactWrapper} from 'enzyme';
import {act} from 'react-dom/test-utils';
import * as fp from 'lodash/fp';

// This file is necessary because angular imports complain if there
// is no zone, regardless of whether the imports are used.
// The error is from:
//   import {ComponentFixture, tick} from '@angular/core/testing';
// And fails with:
//   ReferenceError: Zone is not defined
export async function waitOneTickAndUpdate(wrapper: ReactWrapper) {
  const waitImmediate = () => new Promise<void>(resolve => setImmediate(resolve));
  await act(waitImmediate);
  wrapper.update();
}

export async function waitForFakeTimersAndUpdate(wrapper: ReactWrapper) {
  act(() => {
    jest.runOnlyPendingTimers();
  });
  await waitOneTickAndUpdate(wrapper);
}

export const findNodesByExactText = fp.curry((wrapper: ReactWrapper, text) => wrapper.findWhere(node => {
  return (node.name() === null && node.text() === text);
}));

export const findNodesContainingText = fp.curry((wrapper: ReactWrapper, text) => wrapper.findWhere(node => {
  return (node.name() === null && node.text().includes(text));
}));
