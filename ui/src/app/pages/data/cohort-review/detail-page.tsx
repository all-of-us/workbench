import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

import {HelpSidebar} from 'app/components/help-sidebar';
import {SpinnerOverlay} from 'app/components/spinners';
import {DetailHeader} from 'app/pages/data/cohort-review/detail-header.component';
import {DetailTabs} from 'app/pages/data/cohort-review/detail-tabs.component';
import {cohortReviewStore, getVocabOptions, vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ParticipantCohortStatus, SortOrder} from 'generated/fetch';

interface Props {
  workspace: WorkspaceData;
}

interface State {
  participant: ParticipantCohortStatus;
}

export const DetailPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {participant: null};
    }

    async componentDidMount() {
      const {workspace: {cdrVersionId, id, namespace}} = this.props;
      const {ns, wsid, cid} = urlParamsStore.getValue();
      const promises = [];
      if (!cohortReviewStore.getValue()) {
        promises.push(
          cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
            page: 0,
            pageSize: 25,
            sortOrder: SortOrder.Asc,
            filters: {items: []}
          }).then(review => cohortReviewStore.next(review))
        );
      }
      if (!currentCohortStore.getValue()) {
        promises.push(cohortsApi().getCohort(ns, wsid, cid).then(cohort => currentCohortStore.next(cohort)));
      }
      if (promises.length) {
        await Promise.all(promises);
      }
      this.subscription = urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(params => !!params.pid)
        .switchMap(({pid}) => {
          return from(cohortReviewApi()
            .getParticipantCohortStatus(ns, wsid,
              cohortReviewStore.getValue().cohortReviewId, +pid))
            .do(ps => {
              this.setState({participant: ps});
            });
        })
        .subscribe();
      if (!vocabOptions.getValue()) {
        const {cohortReviewId} = cohortReviewStore.getValue();
        getVocabOptions(namespace, id, cohortReviewId);
      }
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    setParticipant(v) {
      this.setState({participant: v});
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
            <HelpSidebar location='reviewParticipantDetail' participant={participant}
              setParticipant={(p) => this.setParticipant(p)} />
          </React.Fragment>
          : <SpinnerOverlay />
        }
      </div>;
    }
  }
);

@Component ({
  template: '<div #root></div>',
})
export class DetailPageComponent extends ReactWrapperBase {
  constructor() {
    super(DetailPage, []);
  }
}
