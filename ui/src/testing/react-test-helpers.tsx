import {mount, ReactWrapper} from 'enzyme';
import * as fp from 'lodash/fp';
import {Dropdown} from "primereact/dropdown";
import {InputSwitch} from "primereact/inputswitch";
import * as React from 'react';
import {act} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router';

export async function waitOneTickAndUpdate(wrapper: ReactWrapper) {
  const waitImmediate = () => new Promise<void>(resolve => setImmediate(resolve))
  await act(waitImmediate);
  wrapper.update();
}

// Combining a setTimeout with a delay of 0 (used in UI) and setImmediate can result in a non-deterministic order of events
// If you are testing code that uses setTimeout this may be a safer choice.
export async function waitOnTimersAndUpdate(wrapper: ReactWrapper){
  const waitForTimeout = () => new Promise<void>(resolve => setTimeout(resolve, 0));
  await act(waitForTimeout);
  wrapper.update();
}

export async function waitForFakeTimersAndUpdate(wrapper: ReactWrapper) {
  await act(() => {
    jest.runOnlyPendingTimers();
    return Promise.resolve();
  });
  wrapper.update();
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


interface PrimeReactChangeEvent {originalEvent: Event, value: any, target: {name: string, id: string, value: any}};
export const simulateComponentChange = async(wrapper: ReactWrapper, component: any, value: any) => {
  const primeReactComponent = component as React.Component<{onChange: (PrimeReactChangeEvent) => void}, any>;
  primeReactComponent.props.onChange({
    originalEvent: undefined,
    value,
    target: {name: 'name', id: '', value}
  });
  return await waitOneTickAndUpdate(wrapper);
};
