import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withUrlParams} from 'app/utils';
import {WorkspaceNavBarReact} from 'app/views/workspace-nav-bar';

export const WorkspaceWrapper = withUrlParams()(class extends React.Component<{urlParams: any},
  {displayNavBar: boolean}> {
  constructor(props) {
    super(props);
    this.state = {
      displayNavBar: false
    };
  }

  componentDidMount() {
    this.getWorkspace();
  }

  async getWorkspace() {
    const {urlParams: {ns, wsid}} = this.props;

  }

  getTabPath(): string {

  }

  render() {
    const {displayNavBar} = this.state;
    return <React.Fragment>
      {displayNavBar && <WorkspaceNavBarReact/>}
    </React.Fragment>;
  }
)};

@Component({
  template: '<div #root></div>'
})
export class WorkspaceWrapperComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceWrapper, []);
  }
}
