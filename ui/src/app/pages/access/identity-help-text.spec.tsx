import '@testing-library/jest-dom';

import * as React from 'react';

import { AccessModule } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import * as accessUtils from 'app/utils/access-utils';

import { IdentityHelpText } from './identity-help-text';
import { TwoFactorAuthModal } from './two-factor-auth-modal';

describe('IdentityHelpText', () => {
  let profile;
  let afterInitialClick;
  beforeEach(() => {
    profile = {};
    jest
      .spyOn(accessUtils, 'getAccessModuleStatusByName')
      .mockReturnValue({ moduleName: AccessModule.IDENTITY });
  });
  it('should not render when Identity module is compliant', () => {
    jest.spyOn(accessUtils, 'isCompliant').mockReturnValue(true);
    const { container } = render(
      <IdentityHelpText {...{ profile, afterInitialClick }} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('should render when Identity module is incompliant and user has not clicked on component', () => {
    jest.spyOn(accessUtils, 'isCompliant').mockReturnValue(false);
    const wrapper = render(
      <IdentityHelpText {...{ profile }} afterInitialClick={false} />
    );

    expect(
      screen.getByText('to review the verification steps.')
    ).toBeInTheDocument();
  });

  it('should render when Identity module is incompliant and user has clicked on component', () => {
    jest.spyOn(accessUtils, 'isCompliant').mockReturnValue(false);
    const wrapper = render(
      <IdentityHelpText {...{ profile }} afterInitialClick={true} />
    );

    expect(
      screen.getByText(
        'Looks like you still need to complete this action, please try again.'
      )
    ).toBeInTheDocument();
  });
});
