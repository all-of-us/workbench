import {Component, Input} from '@angular/core';
import * as React from 'react';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort} from 'generated/fetch';
import {CohortReview} from 'generated/fetch';

const styles = reactStyles({
  title: {
    color: '#302973',
    fontSize: '0.9rem',
    fontWeight: 200,
  },
  body: {
    lineHeight: '1rem',
  },
  input: {
    height: '1.25rem',
    width: '10rem',
    border: '1px solid #c5c5c5',
    borderRadius: '3px',
  },
  cancel: {
    color: '#000000',
    fontWeight: 'bold',
    marginRight: '1rem',
    padding: '0.25rem 0.5rem',
    letterSpacing: '1.25px',
  },
  create: {
    background: '#302C71',
    marginLeft: '0.5rem',
  }
});

interface Props {
  created: Function;
  workspace: WorkspaceData;
}

interface State {
  cohort: Cohort;
  review: CohortReview;
  create: boolean;
  creating: boolean;
  inputValue: number;
}

export const CreateReviewModal = withCurrentWorkspace()(
  class extends React.Component<Props, State> {

    constructor(props: any) {
      super(props);
      this.state = {
        review: cohortReviewStore.getValue(),
        cohort: currentCohortStore.getValue(),
        create: true,
        creating: false,
        inputValue: null
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
      const {cohort, inputValue, review} = this.state;
      return <Modal onRequestClose={() => this.cancelReview()}>
        <ModalTitle style={styles.title}>Create Review Set</ModalTitle>
        <ModalBody style={styles.body}>
          <div style={{marginBottom: '0.5rem'}}>
            Cohort {cohort.name} has {review.matchedParticipantCount.toLocaleString() + ' '}
            participants for possible review.  How many would you like to review?
            {review.matchedParticipantCount > 10000 && <span> (max 10,000)</span>}
          </div>
          <input type='number'
            value={inputValue}
            style={styles.input}
            placeholder='NUMBER OF PARTICIPANTS'
            onChange={e => this.setState({inputValue: parseInt(e.target.value, 10)})}/>
        </ModalBody>
        <ModalFooter>
          <Button style={styles.cancel} type='link' onClick={() => this.cancelReview()}>
            CANCEL
          </Button>
          <Button style={styles.create} type='primary' onClick={() => this.createReview()}>
            CREATE SET
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
