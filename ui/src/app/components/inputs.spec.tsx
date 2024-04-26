import * as React from 'react';

import { fireEvent, render } from '@testing-library/react';

import { CheckBox, TextAreaWithLengthValidationMessage } from './inputs';

const initialText = 'Hey';

describe('inputs', () => {
  it('click causes DOM checked state to change', () => {
    // When the checkbox manages its own state, a click should cause the input to change.
    const { getByRole } = render(<CheckBox label='asdf' />);
    const checkbox = getByRole('checkbox') as HTMLInputElement;

    expect(checkbox.checked).toEqual(false);
    fireEvent.click(checkbox);
    expect(checkbox.checked).toEqual(true);
  });

  it('uses "checked" to set initial state - true', () => {
    const { getByRole } = render(<CheckBox checked={true} />);
    const checkbox = getByRole('checkbox') as HTMLInputElement;
    expect(checkbox.checked).toEqual(true);
  });

  it('uses "checked" to set initial state - false', () => {
    const { getByRole } = render(<CheckBox checked={false} />);
    const checkbox = getByRole('checkbox') as HTMLInputElement;
    expect(checkbox.checked).toEqual(false);
  });

  it('uses props only when manageOwnState=false', () => {
    // This pattern is disfavored but still exists in the RW codebase, where
    // the caller of CheckBox provides "checked" as a prop and handles updates
    // by reacting to onChange callbacks.
    const { getByRole, rerender } = render(
      <CheckBox checked={true} manageOwnState={false} />
    );
    const checkbox = getByRole('checkbox') as HTMLInputElement;

    expect(checkbox.checked).toEqual(true);

    // A user click shouldn't directly affect the checked state.
    fireEvent.click(checkbox);
    expect(checkbox.checked).toEqual(true);

    rerender(<CheckBox checked={false} manageOwnState={false} />);
    expect(checkbox.checked).toEqual(false);
  });

  it('calls onChange with target checked state', () => {
    let externalValue = null;
    const { getByRole } = render(
      <CheckBox onChange={(value) => (externalValue = value)} />
    );
    const checkbox = getByRole('checkbox');

    fireEvent.click(checkbox);
    expect(externalValue).toEqual(true);

    fireEvent.click(checkbox);
    expect(externalValue).toEqual(false);
  });

  it('renders with plain-text label', () => {
    const { getByText } = render(<CheckBox label='hello' />);
    expect(getByText('hello')).toBeTruthy();
  });

  it('renders with HTML label', () => {
    const { getByRole } = render(
      <CheckBox
        label={
          <span>
            Hello, <i>world</i>
          </span>
        }
      />
    );
    expect(getByRole('checkbox', { name: 'Hello, world' })).toBeTruthy();
  });

  it('Shows characters remaining warning', () => {
    const { getByTestId } = render(
      <TextAreaWithLengthValidationMessage
        id='test'
        initialText='Hey'
        maxCharacters={5}
        onChange={() => {}}
      />
    );
    // Length of initialText (Hey): 3, characters remaining 5 - 3
    expect(getByTestId('characterLimit').textContent).toEqual(
      '2 characters remaining'
    );
  });

  it('Shows characters remaining warning for 0', () => {
    const { getByTestId } = render(
      <TextAreaWithLengthValidationMessage
        id='test'
        initialText='Hey'
        maxCharacters={3}
        onChange={() => {}}
      />
    );
    expect(getByTestId('characterLimit').textContent).toEqual(
      '0 characters remaining'
    );
  });

  it('Shows characters over warning', () => {
    const { getByTestId } = render(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText + ' lets test'}
        maxCharacters={5}
        onChange={() => {}}
      />
    );
    expect(getByTestId('characterLimit').textContent).toEqual(
      '8 characters over'
    );
  });

  it('Shows too short warning if text input is less than tooShortWarningCharacters', () => {
    const { getByTestId, getByRole } = render(
      <TextAreaWithLengthValidationMessage
        id='test'
        initialText='Hey'
        maxCharacters={15}
        onChange={() => {}}
        tooShortWarning='Testing too short'
        tooShortWarningCharacters={5}
      />
    );

    fireEvent.blur(getByRole('textbox', { name: 'test' }));
    expect(getByTestId('warning').textContent).toBe('Testing too short');
  });

  it('Does not show the too short warning when there are enough characters', () => {
    const { getByRole, queryByTestId } = render(
      <TextAreaWithLengthValidationMessage
        id='test'
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
        tooShortWarning='Testing too short'
        tooShortWarningCharacters={2}
      />
    );

    fireEvent.blur(getByRole('textbox', { name: 'test' }));
    expect(queryByTestId('warning')).toBeNull();
  });

  it('Does not show the too short warning when tooShortWarning is not set', () => {
    const { getByRole, queryByTestId } = render(
      <TextAreaWithLengthValidationMessage
        id='test'
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
      />
    );

    fireEvent.blur(getByRole('textbox', { name: 'test' }));
    expect(queryByTestId('warning')).toBeNull();
  });
});
