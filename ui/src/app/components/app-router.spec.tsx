import * as React from "react";
import {AppRoute, AppRouter, Guard} from "app/components/app-router";
import {MemoryRouter} from "react-router";
import {mount} from "enzyme";
import {Redirect, Switch} from "react-router-dom";
import {waitOneTickAndUpdate} from "../../testing/react-test-helpers";

jest.mock('react-router-dom', () => {
  const originalModule = jest.requireActual('react-router-dom');

  return {
    __esModule: true,
    ...originalModule,
    BrowserRouter: ({ children }) => <div>{children}</div>,
  }
});

describe('AppRouter', () => {
  const component = (initialEntries: string[], initialIndex: number) => {
    return mount(<MemoryRouter initialEntries={initialEntries} initialIndex={initialIndex}>
      {makeAppRouter()}
    </MemoryRouter>);
  }

  it('allows anyone into unprotected route', () => {
    const wrapper = component(['/unprotected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Unprotected Route');
  });

  it('punts when user fails guard', () => {
    const wrapper = component(['/unreachable-path'], 0);
    // don't want to pull all of angular into a test of react routerto test that a redirect is
    // happening, so we'll check that the router is telling it to redirect to the right place
    expect(wrapper.find('span').first().text()).toEqual('Punting');
  });

  it('renders when user passes guard', () => {
    const wrapper = component(['/protected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Protected Route');
  });

  it('renders for sibling routes under a guard', () => {
    const wrapper = component(['/other-protected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Other Protected Route');
  });

  it('renders for nested protectedRoutes', () => {
    const wrapper = component(['/nested-protected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Nested Protected Route');
  });

  it('punts on failed nested protected route', () => {
    const wrapper = component(['/nested-unreachable-path'], 0);
    // don't want to pull all of angular into a test of react router to test that a redirect is
    // happening, so we'll check that the router is telling it to redirect to the right place
    expect(wrapper.find('span').first().text()).toEqual('Punting');
  });

  it('redirects to not found page on nonsense route', () => {
    const wrapper = component(['/wharrgarbl'], 0);
    waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('span').first().text()).toEqual('Not Found');
  });
});

const alwaysFalseGuard: Guard = {
  allowed: (): boolean => false,
  redirectPath: '/punting'
}

const alwaysTrueGuard: Guard = {
  allowed: (): boolean => true,
  redirectPath: '/punting'
}

const otherAlwaysTrueGuard: Guard = {
  allowed: (): boolean => true,
  redirectPath: '/punting'
}

const makeAppRouter = () => {
  return <AppRouter>
    <Switch>
      <AppRoute exact path='/unprotected-route'><TestComponent text={'Unprotected Route'}/></AppRoute>
      <AppRoute exact path='/punting'><TestComponent text={'Punting'}/></AppRoute>
      <AppRoute exact path='/unreachable-path' guards={[alwaysFalseGuard]}><TestComponent text={'Unreachable Path'}/></AppRoute>
      <AppRoute exact path='/protected-route' guards={[alwaysTrueGuard]}><TestComponent text={'Protected Route'}/></AppRoute>
      <AppRoute exact path='/other-protected-route' guards={[alwaysTrueGuard]}><TestComponent text={'Other Protected Route'}/></AppRoute>
      <AppRoute exact path='/nested-protected-route' guards={[alwaysTrueGuard, otherAlwaysTrueGuard]}><TestComponent text={'Nested Protected Route'}/></AppRoute>
      <AppRoute exact path='/nested-unreachable-path' guards={[alwaysTrueGuard, alwaysFalseGuard]}><TestComponent text={'Unreachable Path'}/></AppRoute>
      <AppRoute exact path='/not-found'><TestComponent text={'Not Found'}/></AppRoute>
      <AppRoute exact={false} path='*'><Redirect to='/not-found'/></AppRoute>
    </Switch>
  </AppRouter>
}

class TestComponent extends React.Component<{text: String}> {
  render() {
    return <span>{this.props.text}</span>
  }
}
