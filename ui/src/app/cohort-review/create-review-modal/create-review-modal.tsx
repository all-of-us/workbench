import {Component, Input} from '@angular/core';
import * as React from 'react';
import {validate} from 'validate.js';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {Button} from 'app/components/buttons';
import {ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, summarizeErrors, withCurrentWorkspace} from 'app/utils';
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
  },
  disabled: {
    background: '#9b9b9b',
    color: '#c9c8c8',
    border: '1px soilid #9b9b9b',
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
  numberOfParticipants: string;
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
        numberOfParticipants: '',
      };
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
      const {cohort, numberOfParticipants, review} = this.state;
      const max = Math.min(this.state.review.matchedParticipantCount, 10000);
      const errors = validate({numberOfParticipants}, {
        numberOfParticipants: {
          numericality: {
            onlyInteger: true,
            greaterThan: 0,
            lessThanOrEqualTo: max,
            message: 'must be a whole number 1 - ' + max.toLocaleString()
          }
        }
      });
      const disabled = !numberOfParticipants || errors;
      return <Modal onRequestClose={() => this.cancelReview()}>
        <ModalTitle style={styles.title}>Create Review Set</ModalTitle>
        <ModalBody style={styles.body}>
          <div style={{marginBottom: '0.5rem'}}>
            Cohort {cohort.name} has {review.matchedParticipantCount.toLocaleString() + ' '}
            participants for possible review.  How many would you like to review?
            {review.matchedParticipantCount > 10000 && <span> (max 10,000)</span>}
          </div>
          <ValidationError>
            {summarizeErrors(numberOfParticipants && errors && errors.numberOfParticipants)}
          </ValidationError>
          <input type='number'
            value={numberOfParticipants}
            style={styles.input}
            placeholder='NUMBER OF PARTICIPANTS'
            onChange={e => this.setState(
              {numberOfParticipants: e.target.value}
            )}/>
        </ModalBody>
        <ModalFooter>
          <Button style={styles.cancel} type='link' onClick={() => this.cancelReview()}>
            CANCEL
          </Button>
          <Button style={disabled ? styles.disabled : styles.create}
            type='primary'
            disabled={disabled}
            onClick={() => this.createReview()}>
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
