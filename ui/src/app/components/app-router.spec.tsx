import * as React from "react";
import {AppRoute, AppRouter, Guard, ProtectedRoutes} from "app/components/app-router";
import {MemoryRouter} from "react-router";
import {mount} from "enzyme";

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
    expect(wrapper.find(NavRedirect).first().props().path).toEqual('/punting');
  });

  it('renders when user passes guard', () => {
    const wrapper = component(['/protected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Protected Route')
  });

  it('renders for sibling routes under a guard', () => {
    const wrapper = component(['/other-protected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Other Protected Route')
  });

  it('renders for nested protectedRoutes', () => {
    const wrapper = component(['/nested-protected-route'], 0);
    expect(wrapper.find('span').first().text()).toEqual('Nested Protected Route')
  });

  it('punts on failed nested protected route', () => {
    const wrapper = component(['/nested-unreachable-path'], 0);
    // don't want to pull all of angular into a test of react router to test that a redirect is
    // happening, so we'll check that the router is telling it to redirect to the right place
    expect(wrapper.find(NavRedirect).first().props().path).toEqual('/punting');
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
    <AppRoute path='/unprotected-route' component={() => <TestComponent text={'Unprotected Route'}/>}/>
    <AppRoute path='/punting' component={() => <TestComponent text={'Punting'}/>}/>
    <ProtectedRoutes guards={[alwaysFalseGuard]}>
      <AppRoute path='/unreachable-path' component={() => <TestComponent text={'Unreachable Path'}/>}/>
    </ProtectedRoutes>
    <ProtectedRoutes guards={[alwaysTrueGuard]}>
      <AppRoute path='/protected-route' component={() => <TestComponent text={'Protected Route'}/>}/>
      <AppRoute path='/other-protected-route' component={() => <TestComponent text={'Other Protected Route'}/>}/>
      <ProtectedRoutes guards={[otherAlwaysTrueGuard]}>
        <AppRoute path='/nested-protected-route' component={() => <TestComponent text={'Nested Protected Route'}/>}/>
      </ProtectedRoutes>
      <ProtectedRoutes guards={[alwaysFalseGuard]}>
        <AppRoute path='/nested-unreachable-path' component={() => <TestComponent text={'Unreachable Path'}/>}/>
      </ProtectedRoutes>
    </ProtectedRoutes>
  </AppRouter>
}

class TestComponent extends React.Component<{text: String}> {
  render() {
    return <span>{this.props.text}</span>
  }
}
