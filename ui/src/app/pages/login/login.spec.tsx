import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { render, screen } from '@testing-library/react';

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
  });

  it('should render', () => {
    const { container } = component(loginProps);
    expect(container).toBeInTheDocument();
  });
});
