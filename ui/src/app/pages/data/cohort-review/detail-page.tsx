import * as fp from 'lodash/fp';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

import {SpinnerOverlay} from 'app/components/spinners';
import {DetailHeader} from 'app/pages/data/cohort-review/detail-header.component';
import {DetailTabs} from 'app/pages/data/cohort-review/detail-tabs.component';
import {getVocabOptions, participantStore, vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {withCurrentCohortReview, withCurrentWorkspace} from 'app/utils';
import {currentCohortReviewStore, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CohortReview, ParticipantCohortStatus, SortOrder} from 'generated/fetch';

interface Props {
  cohortReview: CohortReview;
  workspace: WorkspaceData;
}

interface State {
  participant: ParticipantCohortStatus;
}

export const DetailPage = fp.flow(withCurrentCohortReview(), withCurrentWorkspace())(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {participant: null};
    }

    async componentDidMount() {
      const {workspace: {cdrVersionId, id, namespace}} = this.props;
      let {cohortReview} = this.props;
      const {ns, wsid, cid} = urlParamsStore.getValue();
      if (!cohortReview) {
        await cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
          page: 0,
          pageSize: 25,
          sortOrder: SortOrder.Asc,
          filters: {items: []}
        }).then(response => {
          cohortReview = response.cohortReview;
          currentCohortReviewStore.next(cohortReview);
        });
      }
      this.subscription = urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(params => !!params.pid)
        .switchMap(({pid}) => {
          return from(cohortReviewApi()
            .getParticipantCohortStatus(ns, wsid, cohortReview.cohortReviewId, +pid))
            .do(ps => participantStore.next(ps));
        })
        .subscribe();
      if (!vocabOptions.getValue()) {
        getVocabOptions(namespace, id, cohortReview.cohortReviewId);
      }
      this.subscription.add(participantStore.subscribe(participant => this.setState({participant})));
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
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
