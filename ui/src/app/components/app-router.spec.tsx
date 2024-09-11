import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { Switch } from 'react-router-dom';
import { Navigate } from 'react-router-dom-v5-compat';

import { render, screen } from '@testing-library/react';
import { AppRoute, AppRouter, Guard } from 'app/components/app-router';

jest.mock('react-router-dom', () => {
  const originalModule = jest.requireActual('react-router-dom');

  return {
    __esModule: true,
    ...originalModule,
    BrowserRouter: ({ children }) => <div>{children}</div>,
  };
});

// unsure if this is needed
jest.mock('react-router-dom-v5-compat', () => {
  const originalModule = jest.requireActual('react-router-dom-v5-compat');

  return {
    __esModule: true,
    ...originalModule,
    BrowserRouter: ({ children }) => <div>{children}</div>,
  };
});

class TestComponent extends React.Component<{ text: String }> {
  render() {
    return <span>{this.props.text}</span>;
  }
}

const alwaysFalseGuard: Guard = {
  allowed: (): boolean => false,
  redirectPath: '/punting',
};

const alwaysTrueGuard: Guard = {
  allowed: (): boolean => true,
  redirectPath: '/punting',
};

const otherAlwaysTrueGuard: Guard = {
  allowed: (): boolean => true,
  redirectPath: '/punting',
};

const renderingFalseGuard: Guard = {
  allowed: (): boolean => false,
  renderBlocked: () => <span>guard component</span>,
};

const TestAppRouter = () => (
  <AppRouter>
    <Switch>
      <AppRoute exact path='/unprotected-route'>
        <TestComponent text={'Unprotected Route'} />
      </AppRoute>
      <AppRoute exact path='/punting'>
        <TestComponent text={'Punting'} />
      </AppRoute>
      <AppRoute exact path='/unreachable-path' guards={[alwaysFalseGuard]}>
        <TestComponent text={'Unreachable Path'} />
      </AppRoute>
      <AppRoute exact path='/protected-route' guards={[alwaysTrueGuard]}>
        <TestComponent text={'Protected Route'} />
      </AppRoute>
      <AppRoute exact path='/block-render-route' guards={[renderingFalseGuard]}>
        <TestComponent text={'Rendering Route'} />
      </AppRoute>
      <AppRoute exact path='/other-protected-route' guards={[alwaysTrueGuard]}>
        <TestComponent text={'Other Protected Route'} />
      </AppRoute>
      <AppRoute
        exact
        path='/nested-protected-route'
        guards={[alwaysTrueGuard, otherAlwaysTrueGuard]}
      >
        <TestComponent text={'Nested Protected Route'} />
      </AppRoute>
      <AppRoute
        exact
        path='/nested-unreachable-path'
        guards={[alwaysTrueGuard, alwaysFalseGuard]}
      >
        <TestComponent text={'Unreachable Path'} />
      </AppRoute>
      <AppRoute exact path='/not-found'>
        <TestComponent text={'Not Found'} />
      </AppRoute>
      <AppRoute exact path='*'>
        <Navigate to='/not-found' />
      </AppRoute>
    </Switch>
  </AppRouter>
);

describe(AppRouter.name, () => {
  const component = (initialEntries: string[]) => {
    return render(
      <MemoryRouter initialEntries={initialEntries}>
        <TestAppRouter />
      </MemoryRouter>
    );
  };

  it('allows anyone into unprotected route', () => {
    component(['/unprotected-route']);
    expect(screen.getByText('Unprotected Route')).toBeInTheDocument();
  });

  it('punts when user fails guard', () => {
    component(['/unreachable-path']);
    expect(screen.getByText('Punting')).toBeInTheDocument();
  });

  it('renders when user passes guard', () => {
    component(['/protected-route']);
    expect(screen.getByText('Protected Route')).toBeInTheDocument();
  });

  it('renders for sibling routes under a guard', () => {
    component(['/other-protected-route']);
    expect(screen.getByText('Other Protected Route')).toBeInTheDocument();
  });

  it('renders for nested protectedRoutes', () => {
    component(['/nested-protected-route']);
    expect(screen.getByText('Nested Protected Route')).toBeInTheDocument();
  });

  it('punts on failed nested protected route', () => {
    component(['/nested-unreachable-path']);
    expect(screen.getByText('Punting')).toBeInTheDocument();
  });

  it('redirects to not found page when no route is matched', async () => {
    component(['/wharrgarbl']);
    expect(await screen.findByText('Not Found')).toBeInTheDocument();
  });

  it('renders content when guard fails', () => {
    component(['/block-render-route']);
    expect(screen.getByText('guard component')).toBeInTheDocument();
  });
});
