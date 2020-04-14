import * as fp from 'lodash/fp';
import * as React from 'react';
import { BrowserRouter, Route, Link} from "react-router-dom";
import { ReactWrapperBase } from 'app/utils';
import {Component} from '@angular/core';
import {WorkspaceList} from '../pages/workspace/workspace-list';

const {useState} = React

type nothing = {}

export const AppRouterComponent: React.FunctionComponent<nothing> = () => {
  console.log("App Router Rendered")
  return <BrowserRouter>
    <Route path="/route">
      <Link to="/test">Link To Test</Link>
    </Route>
    <Route path="/test">
      <div style={{display: 'flex', flexDirection: 'column'}}>
        <Link to="/workspaces">React Workspace Link</Link>
        <Link to="/route">Route Link</Link>
      </div>
    </Route>
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
