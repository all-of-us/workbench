import * as React from 'react';
import * as fp from 'lodash/fp';
import { validate } from 'validate.js';

import { Cohort } from 'generated/fetch';
import { CohortReview } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { NumberInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import { queryResultSizeStore } from 'app/services/review-state.service';
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import {
  reactStyles,
  summarizeErrors,
  withCurrentCohortReview,
  withCurrentWorkspace,
} from 'app/utils';
import { triggerEvent } from 'app/utils/analytics';
import {
  currentCohortReviewStore,
  NavigationProps,
} from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

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
  },
});

interface Props extends NavigationProps {
  canceled: Function;
  cohort: Cohort;
  cohortReview: CohortReview;
  created: Function;
  participantCount?: number;
  workspace: WorkspaceData;
}

interface State {
  create: boolean;
  creating: boolean;
  numberOfParticipants: string;
}

export const CreateReviewModal = fp.flow(
  withCurrentCohortReview(),
  withCurrentWorkspace(),
  withNavigation
)(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        create: true,
        creating: false,
        numberOfParticipants: '',
      };
    }

    componentDidMount(): void {
      triggerEvent('Create Review Set', 'Click', 'Create Set - Review');
    }

    createReview() {
      this.setState({ creating: true });
      const {
        cohort,
        workspace: { id, namespace },
      } = this.props;
      const { numberOfParticipants } = this.state;
      const request = {
        name: cohort.name,
        size: parseInt(numberOfParticipants, 10),
      };

      cohortReviewApi()
        .createCohortReview(namespace, id, cohort.id, request)
        .then((response) => {
          currentCohortReviewStore.next(response);
          queryResultSizeStore.next(parseInt(numberOfParticipants, 10));
          this.setState({ creating: false });
          if (serverConfigStore.get().config.enableMultiReview) {
            this.props.created(response);
          } else {
            this.props.created(true);
            this.props.navigate([
              'workspaces',
              namespace,
              id,
              'data',
              'cohorts',
              cohort.id,
              'review',
              'participants',
            ]);
          }
        });
    }

    render() {
      const {
        cohort,
        cohortReview: { matchedParticipantCount },
        participantCount,
      } = this.props;
      const cohortParticipantCount =
        participantCount || matchedParticipantCount;
      const { creating, numberOfParticipants } = this.state;
      const max = Math.min(cohortParticipantCount, 10000);
      const errors = validate(
        { numberOfParticipants },
        {
          numberOfParticipants: {
            numericality: {
              onlyInteger: true,
              greaterThan: 0,
              lessThanOrEqualTo: max,
              message: 'must be a whole number 1 - ' + max.toLocaleString(),
            },
          },
        }
      );
      const disabled = !numberOfParticipants || errors;
      return (
        <Modal onRequestClose={() => this.props.canceled()}>
          <ModalTitle style={styles.title}>Create Review Set</ModalTitle>
          <ModalBody style={styles.body}>
            <div style={{ marginBottom: '0.5rem' }}>
              Cohort {cohort.name} has{' '}
              {cohortParticipantCount.toLocaleString() + ' '}
              participants for possible review. How many would you like to
              review?
              {cohortParticipantCount > 10000 && <span> (max 10,000)</span>}
            </div>
            <ValidationError>
              {summarizeErrors(
                numberOfParticipants && errors && errors.numberOfParticipants
              )}
            </ValidationError>
            <NumberInput
              value={numberOfParticipants}
              style={styles.input}
              placeholder='NUMBER OF PARTICIPANTS'
              onChange={(v) => this.setState({ numberOfParticipants: v })}
            />
          </ModalBody>
          <ModalFooter>
            <Button
              style={styles.cancel}
              type='link'
              disabled={creating}
              onClick={() => this.props.canceled()}
            >
              CANCEL
            </Button>
            <Button
              style={disabled ? styles.disabled : styles.create}
              type='primary'
              disabled={disabled || creating}
              onClick={() => this.createReview()}
            >
              {creating && (
                <Spinner
                  size={16}
                  style={{ marginRight: '0.25rem', marginLeft: '-0.25rem' }}
                />
              )}
              CREATE SET
            </Button>
          </ModalFooter>
        </Modal>
      );
    }
  }
);
