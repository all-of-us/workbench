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
  const waitImmediate = () => new Promise<void>(resolve => setImmediate(resolve))
  await act(waitImmediate);
  wrapper.update();
}

// Combining a setTimeout with a delay of 0 (used in UI) and setImmediate can result in a non-deterministic order of events
// If you are testing code that uses setTimeout this may be a safer choice.
// If you are using fakeAsync waitOneTickAndUpdate is preferred
export async function waitOnTimersAndUpdate(wrapper: ReactWrapper){
  const waitForTimeout = () => new Promise<void>(resolve => setTimeout(resolve, 0));
  await act(waitForTimeout);
  wrapper.update();
}

export async function waitForFakeTimersAndUpdate(wrapper: ReactWrapper) {
  act(() => {
    jest.runOnlyPendingTimers();
  });
  await waitOneTickAndUpdate(wrapper);
}

export async function simulateSelection(selectElement: ReactWrapper, selection: string) {
  const domNode = selectElement.getDOMNode() as HTMLSelectElement;
  domNode.value = selection;
  selectElement.simulate('change', {target: domNode});
  await waitOneTickAndUpdate(selectElement);
}

// We only want to check against the actual text node
// Capturing other nodes in this search will return the parent nodes as well as the text,
// The "nodes" that exclusively have text do not have a node name and return null
export const findNodesByExactText = fp.curry((wrapper: ReactWrapper, text) => wrapper.findWhere(node => {
  return (node.name() === null && node.text() === text);
}));

export const findNodesContainingText = fp.curry((wrapper: ReactWrapper, text) => wrapper.findWhere(node => {
  return (node.name() === null && fp.includes(text, node.text()));
}));

export const mockUseNavigation = () => {
  const navigate = jest.fn();
  const navigateByUrl = jest.fn();

  jest.mock('app/utils/navigation', () => ({
    ...jest.requireActual('app/utils/navigation'),
    useNavigation: () => [navigate, navigateByUrl],
  }));

  return [navigate, navigateByUrl];
};
