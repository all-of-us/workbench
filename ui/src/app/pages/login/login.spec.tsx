import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { render, screen } from '@testing-library/react';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';

import LoginReactComponent from './login';

describe('LoginComponent', () => {
  let loginProps: { signIn: Function; onCreateAccount: Function };

  const component = (props) =>
    render(
      <MemoryRouter>
        <LoginReactComponent {...props} />
      </MemoryRouter>
    );

  beforeEach(() => {
    loginProps = {
      signIn: () => {},
      onCreateAccount: () => {},
    };
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableLoginIssueBanner: false,
      },
    });
  });

  it('should render', () => {
    component(loginProps);
    expect(
      screen.getByText('Already have a Researcher Workbench account?')
    ).toBeInTheDocument();
  });

  // it('should show banner if flag is on', () => {
  //   serverConfigStore.get().config.enableLoginIssueBanner = true;
  //   component(loginProps);
  //   expect(
  //     screen.getByText(
  //       /Researcher Workbench is currently experiencing an outage\./i
  //     )
  //   ).toBeInTheDocument();
  // });
});
