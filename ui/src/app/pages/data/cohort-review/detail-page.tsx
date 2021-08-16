import * as fp from 'lodash/fp';
import * as React from 'react';

import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {DetailHeader} from 'app/pages/data/cohort-review/detail-header.component';
import {DetailTabs} from 'app/pages/data/cohort-review/detail-tabs.component';
import {getVocabOptions, participantStore, vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {withCurrentCohortReview, withCurrentWorkspace} from 'app/utils';
import {currentCohortReviewStore} from 'app/utils/navigation';
import {MatchParams} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CohortReview, ParticipantCohortStatus, SortOrder} from 'generated/fetch';
import {RouteComponentProps, withRouter} from 'react-router';

interface Props extends WithSpinnerOverlayProps, RouteComponentProps<MatchParams> {
  cohortReview: CohortReview;
  workspace: WorkspaceData;
}

interface State {
  participant: ParticipantCohortStatus;
}

export const DetailPage = fp.flow(withCurrentCohortReview(), withCurrentWorkspace(), withRouter)(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {participant: null};
    }

    async componentDidMount() {
      const {workspace: {cdrVersionId, id, namespace}, hideSpinner} = this.props;
      hideSpinner();
      let {cohortReview} = this.props;
      const {ns, wsid, cid} = this.props.match.params;
      if (!cohortReview) {
        await cohortReviewApi().getParticipantCohortStatuses(ns, wsid, +cid, +cdrVersionId, {
          page: 0,
          pageSize: 25,
          sortOrder: SortOrder.Asc,
          filters: {items: []}
        }).then(response => {
          cohortReview = response.cohortReview;
          currentCohortReviewStore.next(cohortReview);
        });
      }
      if (!vocabOptions.getValue()) {
        getVocabOptions(namespace, id, cohortReview.cohortReviewId);
      }
      this.subscription = participantStore.subscribe(participant => this.setState({participant}));
      this.updateParticipantStore();
    }

    componentDidUpdate(prevProps) {
      if (
          this.props.match.params !== prevProps.match.params
          && !!this.props.match.params.pid
          && this.props.cohortReview
      ) {
        this.updateParticipantStore();
      }
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    async updateParticipantStore() {
      const {ns, wsid, pid} = this.props.match.params;
      const participantCohortStatus = await cohortReviewApi()
        .getParticipantCohortStatus(ns, wsid, this.props.cohortReview.cohortReviewId, +pid);
      participantStore.next(participantCohortStatus);
    }

    render() {
      const {participant} = this.state;
      return <div style={{
        minHeight: 'calc(100vh - calc(4rem + 60px))',
        padding: '1rem',
        position: 'relative',
        marginRight: '45px'
      }}>
        {!!participant
          ? <React.Fragment>
            <DetailHeader participant={participant} />
            <DetailTabs />
          </React.Fragment>
          : <SpinnerOverlay />
        }
      </div>;
    }
  }
);
