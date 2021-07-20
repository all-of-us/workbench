import {navigate, routeConfigDataStore, urlParamsStore} from 'app/utils/navigation';
import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';
import {
  BrowserRouter,
  Link,
  Redirect,
  Route,
  RouteProps,
  Switch,
  useHistory,
  useLocation,
  useParams,
  useRouteMatch
} from 'react-router-dom';
import {Guard} from "app/guards/react-guards";

const {Fragment} = React;

export const usePath = () => {
  const {path} = useRouteMatch();
  return path;
};

// TODO angular2react: This isn't really the right place to be making the store updates but it's the
// best place I found while we're using both angular and react routers
export const withRouteData = WrappedComponent => ({intermediaryRoute = false, routeData, ...props}) => {
  const params = useParams();

  useEffect(() => {
    if (!intermediaryRoute) {
      routeConfigDataStore.next(routeData);
      routeDataStore.set(routeData);
    }
  }, [routeData]);

  useEffect(() => {
    if (!intermediaryRoute) {
      urlParamsStore.next(params);
    }
  }, [params]);

  return <WrappedComponent {...props}/>;
};

export const withFullHeight = WrappedComponent => ({...props}) => {
  return <div style={{height: '100%'}}><WrappedComponent {...props} /></div>;
};

export const SubRoute = ({children}): React.ReactElement => <Switch>{children}</Switch>;
export const AppRouter = ({children}): React.ReactElement => <BrowserRouter><Switch>{children}</Switch></BrowserRouter>;

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

// To compensate for Angular, while keeping true to the declarative/componentized nature of the router
// We will utilize a redirect component that uses the Angular navigation.
// Upon completing the migration this can be replaced with a react-router Redirect component.
// Exported for testing.
export const NavRedirect = ({path}) => {
  navigate([path]);
  return null;
};

interface AppRouteProps extends RouteProps {
  guards?: Array<Guard>
}

export class AppRoute extends Route<AppRouteProps> {
  static defaultProps = {exact: true}

  constructor(props) {
    super(props);
  }

  render() {
    const { redirectPath = null } = fp.find(({allowed}) => !allowed(), this.props.guards) || {};
    return redirectPath
      ? <NavRedirect path={redirectPath}/>
      : super.render();
  }
}

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};
