import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {CriteriaSearch} from 'app/pages/data/criteria-search';
import {
  ReactWrapperBase,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {currentConceptStore, NavStore, setSidebarActiveIconStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Criteria, WorkspaceAccessLevel} from 'generated/fetch';

interface Props {
  cohortContext: any;
  concept: Array<Criteria>;
  workspace: WorkspaceData;
  urlParams: any;
}

const ConceptSearch = fp.flow(withCurrentCohortSearchContext(), withCurrentConcept(), withCurrentWorkspace(), withUrlParams())
  (class extends React.Component<Props> {
    constructor(props: any) {
      super(props);
      this.state = {};
    }

    componentDidMount() {
      currentConceptStore.next([]);
    }

    get disableFinishButton() {
      const {concept, workspace: {accessLevel}} = this.props;
      return !concept || concept.length === 0 || ![WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER].includes(accessLevel);
    }

    render() {
      const {cohortContext, urlParams: {domain}, workspace: {id, namespace}} = this.props;
      return <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
        <CriteriaSearch backFn={() => NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts'])}
                        cohortContext={{domain, source: 'concept'}}
                        conceptSearchTerms={!!cohortContext ? cohortContext.searchTerms : ''}/>
        <Button style={{float: 'right', marginBottom: '2rem'}}
                disabled={this.disableFinishButton}
                onClick={() => setSidebarActiveIconStore.next('concept')}>Finish & Review</Button>
      </FadeBox>;
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
