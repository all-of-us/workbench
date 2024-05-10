import * as React from 'react';

import { render, screen, waitFor } from '@testing-library/react';
import { TermsOfService, TosProps } from 'app/components/terms-of-service';

import { expectButtonElementEnabled } from 'testing/react-test-helpers';

let props: TosProps;
const onCompleteSpy = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  props = {
    onComplete: onCompleteSpy,
    filePath: '/assets/documents/fake-document-path.html',
    afterPrev: false,
    showReAcceptNotification: false,
  };
});

it('should render', async () => {
  const wrapper = render(<TermsOfService {...props} />);
  await screen.findByText(
    'Please read through the entire agreement to continue.'
  );
});
it('should enable NEXT button and checkbox should be selected if page is re-visited after Institution Page', async () => {
  props.afterPrev = true;
  render(<TermsOfService {...props} />);
  const acknowledgeTermsCheckbox: HTMLInputElement = await screen.findByRole(
    'checkbox',
    { name: /acknowledge terms/i }
  );
  expect(acknowledgeTermsCheckbox.checked).toBeTruthy();
  expectButtonElementEnabled(
    screen.getByRole('button', {
      name: /next/i,
    })
  );
});
