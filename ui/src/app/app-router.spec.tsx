import * as React from "react";
import {AppRoute, AppRouter, Guard, ProtectedRoutes} from "./components/app-router";
import {MemoryRouter} from "react-router";
import {mount} from "enzyme";

// const rrd = require('react-router-dom');
// // Just render plain div with its children
// rrd.BrowserRouter = ({children}) => <div>{children}</div>
// module.exports = rrd;

jest.mock('react-router-dom', () => {
  const originalModule = jest.requireActual('react-router-dom');

  return {
    __esModule: true,
    ...originalModule,
    BrowserRouter: ({ children }) => <div>{children}</div>,
    // useParams: jest.fn(),
    // useHistory: jest.fn(),
  }
});

describe('AppRouter', () => {
  const component = (initialEntries: string[]) => {
    return mount(<MemoryRouter initialEntries={initialEntries}>
      {makeAppRouter()}
    </MemoryRouter>);
  }

  it('allows anyone into unprotected route', () => {
    const wrapper = component(['unprotected-route']);
    expect(wrapper.find('span').first().text()).toEqual('Unprotected Route');
  });

  it('punts when user fails guard', () => {
    const wrapper = component(['/unreachable-path']);
    expect(wrapper.find('span').first().text()).toEqual('Punting');
  });
});

const alwaysFalseGuard: Guard = {
  allowed: (): boolean => false,
  redirectPath: '/unprotected-route'
}

const alwaysTrueGuard: Guard = {
  allowed: (): boolean => true,
  redirectPath: '/unprotected-route'
}

const otherAlwaysTrueGuard: Guard = {
  allowed: (): boolean => true,
  redirectPath: '/unprotected-route'
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
        <AppRoute path='/nested-protected-route' component={() => <TestComponent text={'Nested Protective Route'}/>}/>
      </ProtectedRoutes>
    </ProtectedRoutes>
  </AppRouter>
}

class TestComponent extends React.Component<{text: String}> {
  render() {
    return <span>{this.props.text}</span>
  }
}
