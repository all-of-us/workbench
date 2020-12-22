import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {CriteriaSearch} from 'app/pages/data/criteria-search';
import {
  ReactWrapperBase,
  withCurrentConcept,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {NavStore, setSidebarActiveIconStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Criteria, WorkspaceAccessLevel} from 'generated/fetch';

interface Props {
  concept: Array<Criteria>;
  urlParams: any;
  workspace: WorkspaceData;
}

const ConceptSearch = fp.flow(withCurrentConcept(), withCurrentWorkspace(), withUrlParams())(class extends React.Component<Props> {
  constructor(props: any) {
    super(props);
    this.state = {};
  }

  get disableFinishButton() {
    const {concept, workspace: {accessLevel}} = this.props;
    return !concept || concept.length === 0 || ![WorkspaceAccessLevel.READER, WorkspaceAccessLevel.WRITER].includes(accessLevel);
  }

  render() {
    const {urlParams: {domain}, workspace: {id, namespace}} = this.props;
    return <React.Fragment>
      <CriteriaSearch backFn={() => NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts'])}
                      cohortContext={{domain}}
                      conceptSearchTerms={''}
                      source='concept'/>
      <Button style={{float: 'right', marginBottom: '2rem'}}
              disabled={this.disableFinishButton}
              onClick={() => setSidebarActiveIconStore.next('concept')}>Finish & Review</Button>
    </React.Fragment>;
  }
});

@Component({
  template: '<div #root></div>'
})

export class ConceptSearchComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptSearch, []);
  }
}
