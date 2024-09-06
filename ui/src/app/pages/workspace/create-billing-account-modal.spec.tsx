import '@testing-library/jest-dom';

import * as React from 'react';

import { fireEvent, render, screen } from '@testing-library/react';
import { profileStore } from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  getHTMLInputElementValue,
} from 'testing/react-test-helpers';
import { BROAD } from 'testing/stubs/institution-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { CreateBillingAccountModal } from './create-billing-account-modal';

describe('CreateBillingAccountModal', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();
  const profile = ProfileStubVariables.PROFILE_STUB;

  const component = () => {
    return render(<CreateBillingAccountModal onClose={() => {}} />);
  };

  beforeEach(() => {
    profileStore.set({ profile, load, reload, updateCache });
  });

  it('simulate creating billing account with all steps correctly', async () => {
    const { getByTestId } = component();
    // Show step 0 at start.
    expect(screen.getByTestId('step-0-modal')).toBeInTheDocument();
    expect(screen.queryByTestId('step-1-modal')).not.toBeInTheDocument();
    expect(screen.queryByTestId('step-2-modal')).not.toBeInTheDocument();
    expect(screen.queryByTestId('step-3-modal')).not.toBeInTheDocument();

    // Show step 1 when click use-billing-partner-button button, verify value populated.
    fireEvent.click(screen.getByText('USE A BILLING PARTNER'));
    expect(screen.queryByTestId('step-0-modal')).not.toBeInTheDocument();
    expect(screen.getByTestId('step-1-modal')).toBeInTheDocument();

    // The only enabled input is user phone number. Others are populated from profile API.
    expect(getByTestId('user-first-name')).toBeDisabled();
    expect(getByTestId('user-last-name')).toBeDisabled();
    expect(getByTestId('user-contact-email')).toBeDisabled();
    expect(getByTestId('user-workbench-id')).toBeDisabled();
    expect(getByTestId('user-institution')).toBeDisabled();
    expect(getByTestId('user-phone-number')).not.toBeDisabled();

    expect(getHTMLInputElementValue(getByTestId('user-first-name'))).toEqual(
      'Tester!@#$%^&*()><script>alert("hello");</script>'
    );
    expect((getByTestId('user-last-name') as HTMLInputElement).value).toEqual(
      'MacTesterson!@#$%^&*()><script>alert("hello");</script>'
    );
    expect(getHTMLInputElementValue(getByTestId('user-phone-number'))).toEqual(
      ''
    );
    expect(getHTMLInputElementValue(getByTestId('user-contact-email'))).toEqual(
      'tester@mactesterson.edu><script>alert("hello");</script>'
    );
    expect(getHTMLInputElementValue(getByTestId('user-workbench-id'))).toEqual(
      ProfileStubVariables.PROFILE_STUB.username
    );
    expect(getHTMLInputElementValue(getByTestId('user-institution'))).toEqual(
      BROAD.displayName
    );

    // Type invalid phone number then valid phone number
    fireEvent.change(getByTestId('user-phone-number'), {
      target: { value: '1234x' },
    });
    expectButtonElementDisabled(screen.getByRole('button', { name: /next/i }));
    fireEvent.change(getByTestId('user-phone-number'), {
      target: { value: '1234567890' },
    });
    expect(screen.getByRole('button', { name: /next/i })).not.toBeDisabled();

    // Go to step 2 by clicking next button
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.queryByTestId('step-1-modal')).not.toBeInTheDocument();
    expect(screen.getByTestId('step-2-modal')).toBeInTheDocument();

    expect(screen.getByTestId('user-first-name-text')).toHaveTextContent(
      'Tester!@#$%^&*()><script>alert("hello");</script>'
    );
    expect(screen.getByTestId('user-last-name-text')).toHaveTextContent(
      'MacTesterson!@#$%^&*()><script>alert("hello");</script>'
    );
    expect(screen.getByTestId('user-phone-number-text')).toHaveTextContent(
      '1234567890'
    );
    expect(screen.getByTestId('user-contact-email-text')).toHaveTextContent(
      'tester@mactesterson.edu><script>alert("hello");</script>'
    );
    expect(screen.getByTestId('user-workbench-id-text')).toHaveTextContent(
      ProfileStubVariables.PROFILE_STUB.username
    );
    expect(screen.getByTestId('user-institution-text')).toHaveTextContent(
      BROAD.displayName
    );
    expect(screen.getByTestId('nih-funded-text')).toHaveTextContent('No');
  });
});
