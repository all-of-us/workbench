import * as fp from 'lodash/fp';
import * as React from 'react';
import { BrowserRouter, Route, Link, useParams} from "react-router-dom";
import { ReactWrapperBase } from 'app/utils';
import {Component} from '@angular/core';
import {WorkspaceList} from '../pages/workspace/workspace-list';
import {BuildWorkspace} from '../pages/workspace/workspace-edit';

const {useState, Fragment} = React

type nothing = {}

const RouteTest = () => {
  const {value} = useParams();
  console.log(value);
  return <Fragment>
    <Link to="/test">Link To Test</Link>
    <div>URL Parameter value: {value}</div>
  </Fragment>
}

export const AppRouterComponent: React.FunctionComponent<nothing> = () => {
  console.log("App Router Rendered")
  return <BrowserRouter>
    <Route path="/route/:value" component={RouteTest}/>
    <Route path="/test">
      <div style={{display: 'flex', flexDirection: 'column'}}>
        <Link to="/workspaces">React Workspace Link</Link>
        <Route path="/builder"/>
        <Link to="/route/value">Route Link</Link>
      </div>
    </Route>
    <Route path="/workspaces/build" component={BuildWorkspace}/>
    <Route exact path="/workspaces" component={WorkspaceList}/>
  </BrowserRouter>;
}

@Component({
  template: '<div #root></div>'
})
export class AppRouter extends ReactWrapperBase {
  constructor() {
    super(AppRouterComponent, [])
  }
}
