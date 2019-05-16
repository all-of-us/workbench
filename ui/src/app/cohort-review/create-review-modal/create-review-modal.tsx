import {Component, Input} from '@angular/core';
import * as React from 'react';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {Button} from 'app/components/buttons';
import {TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort} from 'generated/fetch';
import {CohortReview} from 'generated/fetch';

interface Props {
  created: Function;
  workspace: WorkspaceData;
}

interface State {
  cohort: Cohort;
  review: CohortReview;
  create: boolean;
  creating: boolean;
}

export const CreateReviewModal = withCurrentWorkspace()(
  class extends React.Component<Props, State> {

    constructor(props: any) {
      super(props);
      this.state = {
        review: cohortReviewStore.getValue(),
        cohort: currentCohortStore.getValue(),
        create: true,
        creating: false
      };
    }

    get numParticipants() {
      return 0;
    }

    get maxParticipants() {
      return Math.min(this.state.review.matchedParticipantCount, 10000);
    }

    cancelReview() {
      const {ns, wsid} = urlParamsStore.getValue();
      navigate(['workspaces', ns, wsid, 'cohorts']);
    }

    createReview() {
      this.setState({creating: true});
      const {ns, wsid, cid} = urlParamsStore.getValue();
      const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
      const request = {size: 0};

      cohortReviewApi().createCohortReview(ns, wsid, cid, cdrid, request)
        .then(_ => {
          this.setState({creating: false});
          this.props.created(true);
          navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
        });
    }

    render() {
      const {cohort, review} = this.state;
      return <Modal onRequestClose={() => this.cancelReview()}>
        <ModalTitle>Create Review Set</ModalTitle>
        <ModalBody>
          Cohort {cohort.name} has {review.matchedParticipantCount.toLocaleString()}
          participants for possible review.  How many would you like to review?
          {review.matchedParticipantCount > 10000 && <span>(max 10,000)</span>}
          <TextInput/>
        </ModalBody>
        <ModalFooter>
          <Button type='secondary' onClick={() => this.cancelReview()}>Cancel</Button>
          <Button style={{marginLeft: '0.5rem'}} onClick={() => this.createReview()}>
            Create set
          </Button>
        </ModalFooter>
      </Modal>;
    }
  }
);

@Component({
  selector: 'app-create-review-modal',
  template: '<div #root></div>'
})
export class CreateReviewModalComponent extends ReactWrapperBase {
  @Input('created') created: Props['created'];
  constructor() {
    super(CreateReviewModal, ['created']);
  }
}
