import {Component} from '@angular/core';

import * as React from 'react';

import {ReactWrapperBase} from 'app/utils/index';
import {ResetClusterButton} from "../reset-cluster-button/component";

export class SettingsReact extends React.Component<{}, {}> {

  render() {
    return <React.Fragment>
      <h2>Settings</h2>
      <h3>Notebooks</h3>
      <ResetClusterButton billingProjectId="aou-rw-local1-1933908639"/>
    </React.Fragment>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class SettingsComponent extends ReactWrapperBase {
  constructor() {
    super(SettingsReact, []);
  }
}
