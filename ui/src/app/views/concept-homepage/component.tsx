import {Component} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';
import {ConceptSetsList} from 'app/views/concept-set-list/component';
import * as React from 'react';


export class ConceptHomepage extends React.Component<{}, {}> {
  constructor(props) {
    super(props);
  }

  render() {
    return <React.Fragment>
            <ConceptSetsList/>
        </React.Fragment>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class ConceptHomepageComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptHomepage, []);
  }
}
