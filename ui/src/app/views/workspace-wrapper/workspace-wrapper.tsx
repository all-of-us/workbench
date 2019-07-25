import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase} from 'app/utils';
import {WorkspaceNavBarReact} from 'app/views/workspace-nav-bar';

export const WorkspaceWrapper = class extends React.Component<{},
  {displayNavBar: boolean}> {
  constructor(props) {
    super(props);
    this.state = {
      displayNavBar: false
    };
  }

  componentDidMount() {

  }

  getTabPath(): string {

  }

  render() {
    const {displayNavBar} = this.state;
    return <React.Fragment>
      {displayNavBar && <WorkspaceNavBarReact/>}
    </React.Fragment>;
  }
};

@Component({
  template: '<div #root></div>'
})
export class WorkspaceWrapperComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceWrapper, []);
  }
}
