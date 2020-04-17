import {Component} from '@angular/core';
import {BuildWorkspace} from 'app/pages/workspace/workspace-edit';
import {WorkspaceList } from 'app/pages/workspace/workspace-list';
import { ReactWrapperBase } from 'app/utils';
import * as React from 'react';
import { BrowserRouter, Link, Route, useParams} from 'react-router-dom';

const {Fragment} = React;

const RouteTest = () => {
  const {value} = useParams();
  console.log(value);
  return <Fragment>
    <Link to='/test'>Link To Test</Link>
    <div>URL Parameter value: {value}</div>
  </Fragment>;
};

const AppRouter: React.FunctionComponent<any> = () => {
  console.log('App Router Rendered');
  return <BrowserRouter>
    <Route path='/route/:value' component={RouteTest}/>
    <Route path='/test'>
      <div style={{display: 'flex', flexDirection: 'column'}}>
        <Link to='/workspaces'>React Workspace Link</Link>
        <Route path='/builder'/>
        <Link to='/route/value'>Route Link</Link>
      </div>
    </Route>
    <Route path='/workspaces/build' component={BuildWorkspace}/>
    <Route exact path='/workspaces' component={WorkspaceList}/>
  </BrowserRouter>;
};

@Component({
  template: '<div #root></div>'
})
export class AppRouterComponent extends ReactWrapperBase {
  constructor() {
    super(AppRouter, []);
  }
}
