import {Component} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';

export class CohortActions extends React.Component<{}> {
  constructor(props: any) {
    super(props);
  }

  render() {
    return <div>
      <h4>Cohort Saved Successfully</h4>
    </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class CohortActionsComponent extends ReactWrapperBase {

  constructor() {
    super(CohortActions, []);
  }
}
