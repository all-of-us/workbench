import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  CohortReview,
  ParticipantCohortStatus,
  SortOrder,
} from 'generated/fetch';

import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { DetailHeader } from 'app/pages/data/cohort-review/detail-header.component';
import { DetailTabs } from 'app/pages/data/cohort-review/detail-tabs.component';
import {
  getVocabOptions,
  participantStore,
  vocabOptions,
} from 'app/services/review-state.service';
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import {
  hasNewValidProps,
  withCurrentCohortReview,
  withCurrentWorkspace,
} from 'app/utils';
import { currentCohortReviewStore } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  cohortReview: CohortReview;
  workspace: WorkspaceData;
}

interface State {
  participant: ParticipantCohortStatus;
}

export const DetailPage = fp.flow(
  withCurrentCohortReview(),
  withCurrentWorkspace(),
  withRouter
)(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = { participant: null };
    }

    async componentDidMount() {
      const {
        workspace: { id, namespace },
        hideSpinner,
      } = this.props;
      hideSpinner();
      let { cohortReview } = this.props;
      const { ns, wsid, crid } = this.props.match.params;
      if (!cohortReview) {
        const request = {
          page: 0,
          pageSize: 25,
          sortOrder: SortOrder.ASC,
          filters: { items: [] },
        };
        const getCohortReview = cohortReviewApi().getParticipantCohortStatuses(
          ns,
          wsid,
          +crid,
          request
        );
        await getCohortReview.then((response) => {
          cohortReview = response.cohortReview;
          currentCohortReviewStore.next(cohortReview);
        });
      }
      if (!vocabOptions.getValue()) {
        getVocabOptions(namespace, id);
      }
      this.updateParticipantStore();
      this.subscription = participantStore.subscribe((participant) =>
        this.setState({ participant })
      );
    }

    componentDidUpdate(prevProps) {
      if (
        hasNewValidProps(this.props, prevProps, [(p) => p.match.params.pid])
      ) {
        this.updateParticipantStore();
      }
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    async updateParticipantStore() {
      const { ns, wsid, pid, crid } = this.props.match.params;
      const participantCohortStatus =
        await cohortReviewApi().getParticipantCohortStatus(
          ns,
          wsid,
          +crid,
          +pid
        );
      participantStore.next(participantCohortStatus);
    }

    render() {
      const { participant } = this.state;
      return (
        <div
          style={{
            minHeight: 'calc(100vh - calc(6rem + 60px))',
            padding: '1.5rem',
            position: 'relative',
            marginRight: '45px',
          }}
        >
          {!!participant ? (
            <React.Fragment>
              <DetailHeader participant={participant} />
              <DetailTabs />
            </React.Fragment>
          ) : (
            <SpinnerOverlay />
          )}
        </div>
      );
    }
  }
);
