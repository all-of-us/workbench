import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import {
  Matcher,
  render,
  RenderOptions,
  RenderResult,
  screen,
  waitFor,
} from '@testing-library/react';
import { UserEvent } from '@testing-library/user-event/index';

export async function waitAndExecute(timeMs?: number) {
  await new Promise((resolve) => setTimeout(resolve, timeMs || 100));
}

export function renderWithRouter(
  children: string | React.ReactNode,
  options?: RenderOptions
) {
  // It would be ideal to unwrap down to the child wrapper here, but unfortunately
  // wrapper.update() only works on the root node - and most tests need to call that.
  return render(<MemoryRouter>{children}</MemoryRouter>, options);
}

export const expectButtonElementEnabled = (buttonElement: HTMLElement) =>
  expect(buttonElement).not.toHaveStyle('cursor: not-allowed');
export const expectButtonElementDisabled = (buttonElement: HTMLElement) =>
  expect(buttonElement).toHaveStyle('cursor: not-allowed');

// TODO: is there a common way to describe "elements where we disable by setting the cursor to not-allowed" ?
export const expectMenuItemElementEnabled = expectButtonElementEnabled;
export const expectMenuItemElementDisabled = expectButtonElementDisabled;

export const expectSpinner = () => expect(screen.getByLabelText('Please Wait'));
export const waitForNoSpinner = async () =>
  await waitFor(() =>
    expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument()
  );

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

export const getDropdownSelection = (
  container: HTMLElement,
  dropDownName: string
): string => {
  return container.querySelector(`select[name='${dropDownName}']`).textContent;
};

export const getHTMLInputElementValue = (element: HTMLElement): string => {
  return (element as HTMLInputElement).value;
};

export const pickSpinButtonValue = async (
  user: UserEvent,
  name: string,
  value: number
): Promise<void> => {
  const spinButton = screen.getByRole('spinbutton', { name });
  expect(spinButton).toBeInTheDocument();
  await user.clear(spinButton);
  await user.type(spinButton, value.toString());
};
// by default, screen.debug() cuts off after 7000 chars.  let's output more than that
export const debugAll = () => screen.debug(undefined, 1_000_000);

/* This function is used to get the value of a Select component.
This assumes that the Select component is a
react-select Select component. The function expects the
component's underlying input element as input. This is based on
a helper function that Terra-UI utilizes:
https://github.com/DataBiosphere/terra-ui/blob/dev/src/testing/test-utils.ts
*/
export const getSelectComponentValue = (
  inputElement: HTMLInputElement
): string => {
  // Searchable Select components have an additional wrapper element
  const valueContainer =
    inputElement.parentElement?.parentElement?.parentElement;
  const valueElement = valueContainer.querySelector(
    ':scope > div[class*="Value"]'
  );
  return valueElement.textContent ?? '';
};

export const expectPrimaryButton = (element: HTMLElement) => {
  expect(element).toHaveAttribute('data-button-type', 'primary');
};

export const expectSecondaryButton = (element: HTMLElement) => {
  expect(element).toHaveAttribute('data-button-type', 'secondary');
};

export const rgbToHex = (rgb) => {
  const rgbValues = rgb.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
  const hex = (x) => {
    return ('0' + parseInt(x, 10).toString(16)).slice(-2);
  };
  return '#' + hex(rgbValues[1]) + hex(rgbValues[2]) + hex(rgbValues[3]);
};

export const changeInputValue = async (
  inputElement: HTMLInputElement,
  newValue: string,
  user: UserEvent
) => {
  await user.clear(inputElement);
  await user.paste(newValue);
  await user.tab();
};

export const expectTooltip = async (
  element: HTMLElement,
  message: Matcher,
  user: UserEvent
) => {
  await user.hover(element);
  expect(screen.getByText(message)).toBeInTheDocument();
  await user.unhover(element);
};
