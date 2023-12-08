import '@testing-library/jest-dom';

import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { MemoryRouter } from 'react-router';
import * as fp from 'lodash/fp';
import { mount, MountRendererProps, ReactWrapper } from 'enzyme';

import { render, RenderResult } from '@testing-library/react';
import { setImmediate } from 'timers';

export async function waitOneTickAndUpdate(wrapper: ReactWrapper) {
  const waitImmediate = () =>
    new Promise<void>((resolve) => setImmediate(resolve));
  await act(waitImmediate);
  wrapper.update();
}

// Combining a setTimeout with a delay of 0 (used in UI) and setImmediate can result in a non-deterministic order of events
// If you are testing code that uses setTimeout this may be a safer choice.
export async function waitOnTimersAndUpdate(wrapper: ReactWrapper) {
  const waitForTimeout = () =>
    new Promise<void>((resolve) => setTimeout(resolve, 0));
  await act(waitForTimeout);
  wrapper.update();
}

// Use in combination with jest fake timers only; runs all pending jest timers.
// maxRetries may be specified to retry on timer exception.
export async function waitForFakeTimersAndUpdate(
  wrapper: ReactWrapper,
  maxRetries = 0
) {
  for (let attempt = 0; ; attempt++) {
    try {
      await act(() => {
        jest.runOnlyPendingTimers();
        return Promise.resolve();
      });
    } catch (e) {
      if (attempt >= maxRetries) {
        throw e;
      }
      console.log(
        `retrying timer exception (try ${attempt}/${maxRetries}): ${e}`
      );
      continue;
    }
    wrapper.update();
    return;
  }
}

export async function waitAndExecute(timeMs?: number) {
  await new Promise((resolve) => setTimeout(resolve, timeMs || 100));
}

export async function simulateSelection(
  selectElement: ReactWrapper,
  selection: string
) {
  const domNode = selectElement.getDOMNode() as HTMLSelectElement;
  domNode.value = selection;
  selectElement.simulate('change', { target: domNode });
  await waitOneTickAndUpdate(selectElement);
}

export function mountWithRouter(
  children: string | React.ReactNode,
  options?: MountRendererProps
) {
  // It would be ideal to unwrap down to the child wrapper here, but unfortunately
  // wrapper.update() only works on the root node - and most tests need to call that.
  return mount(<MemoryRouter>{children}</MemoryRouter>, options);
}

// We only want to check against the actual text node
// Capturing other nodes in this search will return the parent nodes as well as the text,
// The "nodes" that exclusively have text do not have a node name and return null
export const findNodesByExactText = fp.curry((wrapper: ReactWrapper, text) =>
  wrapper.findWhere((node) => {
    return node.name() === null && node.text() === text;
  })
);

export const findNodesContainingText = fp.curry((wrapper: ReactWrapper, text) =>
  wrapper.findWhere((node) => {
    return node.name() === null && fp.includes(text, node.text());
  })
);

interface PrimeReactChangeEvent {
  originalEvent: Event;
  value: any;
  target: { name: string; id: string; value: any };
}
export const simulateComponentChange = async (
  wrapper: ReactWrapper,
  component: any,
  value: any
) => {
  const primeReactComponent = component as React.Component<
    { onChange: (e: PrimeReactChangeEvent) => void },
    any
  >;
  // @ts-ignore
  primeReactComponent.props().onChange({
    originalEvent: undefined,
    value,
    target: { name: 'name', id: '', value },
  });
  return await waitOneTickAndUpdate(wrapper);
};

export const simulateTextInputChange = async (
  wrapper: ReactWrapper,
  value: string
) => {
  wrapper.simulate('change', { target: { value } });
  return await waitOneTickAndUpdate(wrapper);
};

export const expectButtonElementEnabled = (buttonElement: HTMLElement) =>
  expect(buttonElement).not.toHaveStyle('cursor: not-allowed');
export const expectButtonElementDisabled = (buttonElement: HTMLElement) =>
  expect(buttonElement).toHaveStyle('cursor: not-allowed');

// TODO: is there a common way to describe "elements where we disable by setting the cursor to not-allowed" ?
export const expectMenuItemElementEnabled = expectButtonElementEnabled;
export const expectMenuItemElementDisabled = expectButtonElementDisabled;

// When simulate is used with an input element of type checkbox, it will negate its current state.
export const toggleCheckbox = (checkBoxWrapper: ReactWrapper) =>
  checkBoxWrapper.simulate('change');

// TODO: this is a nasty hack. Can we do better?

// it appears that we need to have a 'popup-root' ID defined when a Modal is rendered, or it will throw an error

export const renderModal = <C,>(
  component: React.ReactElement<C, string | React.JSXElementConstructor<C>>
): RenderResult => render(<div id='popup-root'>{component}</div>);

// seems to be the best we can do with Primereact Dropdown
// based on runtime-configuration-panel.spec pickDropdownOption

export const expectDropdown = (
  container: HTMLElement,
  dropDownId: string
): HTMLElement => {
  const dropdown: HTMLElement = container.querySelector(`#${dropDownId}`);
  expect(dropdown).toBeInTheDocument();
  return dropdown;
};

export const getDropdownOption = (
  container: HTMLElement,
  dropDownId: string,
  optionText: string,
  expectedCount?: number
): Element => {
  const dropdown: HTMLElement = expectDropdown(container, dropDownId);
  dropdown.click();

  const allOptions = container.querySelectorAll(
    `#${dropDownId} .p-dropdown-item`
  );
  if (expectedCount) {
    expect(allOptions).toHaveLength(expectedCount);
  }

  // TODO: can we include this check in the querySelector step?
  const option = Array.from(allOptions).find(
    (di) => di.textContent === optionText
  );
  expect(option).toBeInTheDocument();
  expect(option).toHaveTextContent(optionText);
  return option;
};

export const expectDropdownDisabled = (
  container: HTMLElement,
  dropDownId: string
) => {
  const dropdown: HTMLElement = expectDropdown(container, dropDownId);
  dropdown.click();

  expect(
    container.querySelector(`#${dropDownId} .p-dropdown-item`)
  ).not.toBeInTheDocument();
};
