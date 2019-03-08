import * as React from 'react';

import {withRoute} from 'app/routes';
import {history} from 'app/services/history';

export const Router = withRoute()(({route}) => {
  if (!route) {
    return <div>Not found</div>;
  }
  return route.render();
});

export class Redirect extends React.Component<{to: any}> {
  componentDidMount() {
    const {to} = this.props;
    history.replace(to);
  }

  render() {
    return null;
  }
}

// Example of how to enforce login on particular pages
/*
export const AuthWrapper = fp.flow(
  withRoute(),
  withAuthState(),
)(({route, authState, children}) => {
  if (!route.public && !authState.loggedIn) {
    return <Redirect to={getPath('login')} />;
  }
  return children;
});
*/
