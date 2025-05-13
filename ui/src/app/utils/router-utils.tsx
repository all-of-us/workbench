import {
  RouteComponentProps,
  useHistory,
  useLocation,
  useRouteMatch,
} from 'react-router-dom';

// implement the react-router 5 withRouter HOC using hooks which are present in react-router 6+
// thanks to https://stackoverflow.com/questions/72735944/is-there-an-alternative-of-withrouter-from-react-router

export const withRouter = (WrappedComponent) => (componentProps) => {
  const routerProps: RouteComponentProps = {
    match: useRouteMatch(),
    location: useLocation(),
    history: useHistory(),
  };

  return <WrappedComponent {...componentProps} {...routerProps} />;
};
