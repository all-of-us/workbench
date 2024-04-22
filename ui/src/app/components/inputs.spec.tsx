import * as React from 'react';

import { fireEvent, render } from '@testing-library/react';

import { CheckBox, TextAreaWithLengthValidationMessage } from './inputs';

const initialText = 'Hey';

const expectChecked = (checkbox, checked: boolean) => {
  expect(checkbox.checked).toEqual(checked);
};

describe('inputs', () => {
  it('click causes DOM checked state to change', () => {
    const { getByRole } = render(<CheckBox label='asdf' />);
    const checkbox = getByRole('checkbox');

    expectChecked(checkbox, false);
    fireEvent.click(checkbox);
    expectChecked(checkbox, true);
  });

  it('uses "checked" to set initial state', () => {
    const { getByRole } = render(<CheckBox checked={true} />);
    const checkbox = getByRole('checkbox');

    expectChecked(checkbox, true);
  });

  it('uses props only when manageOwnState=false', () => {
    const { getByRole, rerender } = render(
      <CheckBox checked={true} manageOwnState={false} />
    );
    const checkbox = getByRole('checkbox');

    expectChecked(checkbox, true);
    fireEvent.click(checkbox);
    expectChecked(checkbox, true);

    rerender(<CheckBox checked={false} manageOwnState={false} />);
    expectChecked(checkbox, false);
  });

  it('calls onChange with target checked state', () => {
    let checked = null;
    const { getByRole } = render(
      <CheckBox onChange={(value) => (checked = value)} />
    );
    const checkbox = getByRole('checkbox');

    fireEvent.click(checkbox);
    expect(checked).toEqual(true);
  });

  it('renders with plain-text label', () => {
    const { getByText } = render(<CheckBox label='hello' />);
    expect(getByText('hello')).toBeTruthy();
  });

  it('renders with HTML label', () => {
    const { getByText } = render(
      <CheckBox
        label={
          <span>
            Hello, <i>world</i>
          </span>
        }
      />
    );
    expect(getByText('Hello, world')).toBeTruthy();
  });

  it('Shows characters remaining warning ', () => {
    const { getByTestId } = render(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={5}
        onChange={() => {}}
      />
    );
    expect(getByTestId('characterLimit').textContent).toEqual(
      `2 characters remaining`
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

  it('Shows too short warning if text input is less the short characters', () => {
    const { getByTestId, queryByTestId, rerender } = render(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
      />
    );

    fireEvent.blur(getByTestId('test'));
    expect(queryByTestId('warning')).toBeNull();

    rerender(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
        tooShortWarning={'Testing too short'}
        tooShortWarningCharacters={5}
      />
    );

    fireEvent.blur(getByTestId('test'));
    expect(getByTestId('warning').textContent).toBe('Testing too short');

    rerender(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
        tooShortWarning={'Testing too short'}
        tooShortWarningCharacters={2}
      />
    );

    fireEvent.blur(getByTestId('test'));
    expect(queryByTestId('warning')).toBeNull();
  });
});
