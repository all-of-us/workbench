import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

import {HelpSidebar} from 'app/components/help-sidebar';
import {SpinnerOverlay} from 'app/components/spinners';
import {DetailHeader} from 'app/pages/data/cohort-review/detail-header.component';
import {DetailTabs} from 'app/pages/data/cohort-review/detail-tabs.component';
import {cohortReviewStore, getVocabOptions, vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {Participant} from 'app/utils/participant.model';
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
