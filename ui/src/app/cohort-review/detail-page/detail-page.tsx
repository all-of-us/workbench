import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

import {DetailHeader} from 'app/cohort-review/detail-header/detail-header.component';
import {DetailTabs} from 'app/cohort-review/detail-tabs/detail-tabs.component';
import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore, getVocabOptions, vocabOptions} from 'app/cohort-review/review-state.service';
import {HelpSidebar} from 'app/components/help-sidebar';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';


interface Props {
  workspace: WorkspaceData;
}

interface State {
  participant: Participant;
}

export const DetailPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {participant: null};
    }

    componentDidMount() {
      this.subscription = urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(params => !!params.pid)
        .switchMap(({ns, wsid, pid}) => {
          return from(cohortReviewApi()
            .getParticipantCohortStatus(ns, wsid,
              cohortReviewStore.getValue().cohortReviewId, +pid))
            .do(ps => {
              this.setState({participant: Participant.fromStatus(ps)});
            });
        })
        .subscribe();
      if (!vocabOptions.getValue()) {
        const {workspace: {id, namespace}} = this.props;
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
      return <React.Fragment>
        {!!participant
          ? <React.Fragment>
            <div className='detail-page '>
              <DetailHeader participant={participant} />
              <DetailTabs />
            </div>
            <HelpSidebar location='reviewParticipantDetail' participant={participant}
              setParticipant={(p) => this.setParticipant(p)} />
          </React.Fragment>
          : <SpinnerOverlay />
        }
      </React.Fragment>;
    }
  }
);

@Component ({
  template: '<div #root></div>'
})
export class DetailPageComponent extends ReactWrapperBase {
  constructor() {
    super(DetailPage, []);
  }
}
