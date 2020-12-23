import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {CriteriaSearch} from 'app/pages/data/criteria-search';
import {
  ReactWrapperBase,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {
  conceptSetUpdating,
  currentConceptSetStore,
  currentConceptStore,
  NavStore,
  queryParamsStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Criteria, WorkspaceAccessLevel} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

interface Props {
  cohortContext: any;
  concept: Array<Criteria>;
  setConceptSetUpdating: (conceptSetUpdating: boolean) => void;
  setShowUnsavedModal: (showUnsavedModal: () => Promise<boolean>) => void;
  setUnsavedConceptChanges: (unsavedConceptChanges: boolean) => void;
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  // Show if trying to navigate away with unsaved changes
  showUnsavedModal: boolean;
}

const ConceptSearch = fp.flow(withCurrentCohortSearchContext(), withCurrentConcept(), withCurrentWorkspace(), withUrlParams())
  (class extends React.Component<Props, State> {
    resolveUnsavedModal: Function;
    subscription: Subscription;
    constructor(props: any) {
      super(props);
      this.state = {showUnsavedModal: false};
      this.showUnsavedModal = this.showUnsavedModal.bind(this);
    }

    componentDidMount() {
      currentConceptStore.next([]);
      this.subscription = currentConceptStore.subscribe(currentConcepts => {
        if (![null, undefined].includes(currentConcepts)) {
          const currentConceptSet = currentConceptSetStore.getValue();
          const unsavedChanges = (!currentConceptSet && currentConcepts.length > 0)
            || (!!currentConceptSet && JSON.stringify(currentConceptSet.criteriums.sort()) !== JSON.stringify(currentConcepts.sort()));
          this.props.setUnsavedConceptChanges(unsavedChanges);
        }
      });
      this.subscription.add(conceptSetUpdating.subscribe(updating => this.props.setConceptSetUpdating(updating)));
      this.props.setShowUnsavedModal(this.showUnsavedModal);
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    async showUnsavedModal() {
      this.setState({showUnsavedModal: true});
      return await new Promise<boolean>((resolve => this.resolveUnsavedModal = resolve));
    }

    getModalResponse(res: boolean) {
      this.setState({showUnsavedModal: false});
      this.resolveUnsavedModal(res);
    }

    get disableFinishButton() {
      const {concept, workspace: {accessLevel}} = this.props;
      return !concept || concept.length === 0 || ![WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER].includes(accessLevel);
    }

    render() {
      const {cohortContext, urlParams: {domain}, workspace: {id, namespace}} = this.props;
      const {showUnsavedModal} = this.state;
      const selectedSurvey = queryParamsStore.getValue().survey;
      return <React.Fragment>
        <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
          <CriteriaSearch backFn={() => NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts'])}
                          cohortContext={{domain, selectedSurvey, source: 'concept'}}
                          conceptSearchTerms={!!cohortContext ? cohortContext.searchTerms : ''}/>
          <Button style={{float: 'right', marginBottom: '2rem'}}
                  disabled={this.disableFinishButton}
                  onClick={() => setSidebarActiveIconStore.next('concept')}>Finish & Review</Button>
        </FadeBox>
        {showUnsavedModal && <Modal>
          <ModalTitle>Warning! </ModalTitle>
          <ModalBody>
            Your concept set has not been saved. If you’d like to save your concepts, please click CANCEL
            and save your changes in the right sidebar.
          </ModalBody>
          <ModalFooter>
            <Button type='link' onClick={() => this.getModalResponse(false)}>Cancel</Button>
            <Button type='primary' onClick={() => this.getModalResponse(true)}>Discard Changes</Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>;
    }
  });

@Component({
  template: '<div #root></div>'
})

export class ConceptSearchComponent extends ReactWrapperBase {
  conceptSetUpdating: boolean;
  showUnsavedModal: () => Promise<boolean>;
  unsavedConceptChanges: boolean;
  constructor() {
    super(ConceptSearch, ['setConceptSetUpdating', 'setShowUnsavedModal', 'setUnsavedConceptChanges']);
    this.setConceptSetUpdating = this.setConceptSetUpdating.bind(this);
    this.setShowUnsavedModal = this.setShowUnsavedModal.bind(this);
    this.setUnsavedConceptChanges = this.setUnsavedConceptChanges.bind(this);
  }

  setShowUnsavedModal(showUnsavedModal: () => Promise<boolean>): void {
    this.showUnsavedModal = showUnsavedModal;
  }

  setConceptSetUpdating(csUpdating: boolean): void {
    this.conceptSetUpdating = csUpdating;
  }

  setUnsavedConceptChanges(unsavedConceptChanges: boolean): void {
    this.unsavedConceptChanges = unsavedConceptChanges;
  }

  canDeactivate(): Promise<boolean> | boolean {
    return !this.unsavedConceptChanges || this.conceptSetUpdating || this.showUnsavedModal();
  }
}
