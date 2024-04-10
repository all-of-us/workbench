import '@testing-library/jest-dom';

import * as React from 'react';

import { ProfileApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

import {
  AccountCreationResendModal,
  AccountCreationUpdateModal,
} from './account-creation-modals';

beforeEach(() => {
  registerApiClient(ProfileApi, new ProfileApiStub());
});

describe('AccountCreationResendModal', () => {
  it('should render', () => {
    render(
      <AccountCreationResendModal
        username='a'
        creationNonce='b'
        onClose={() => {}}
      />
    );
    expect(screen.getByText('Resend Instructions')).toBeInTheDocument();
  });
});

describe('AccountCreationUpdateModal', () => {
  it('should render', () => {
    render(
      <AccountCreationUpdateModal
        username='a'
        creationNonce='b'
        onDone={() => {}}
        onClose={() => {}}
      />
    );
    expect(screen.getByText('Change contact email')).toBeInTheDocument();
  });
});
