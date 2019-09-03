import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

import {DetailHeader} from 'app/cohort-review/detail-header/detail-header.component';
import {DetailTabs} from 'app/cohort-review/detail-tabs/detail-tabs.component';
import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore, getVocabOptions, vocabOptions} from 'app/cohort-review/review-state.service';
import {SidebarContent} from 'app/cohort-review/sidebar-content/sidebar-content.component';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';

const styles = reactStyles({
  detailSidebar: {
    position: 'absolute',
    top: '1px',
    right: '0px',
    borderRadius: '2px',
    height: '100%',
    backgroundColor: '#E2E2EA',
    display: 'flex',
  },
  sidebarHandle: {
    backgroundColor: '#728FA3',
    padding: '0.6rem',
    position: 'absolute',
    borderRadius: '0.2rem 0 0 0.2rem',
    marginLeft: '-2.4rem',
    top: '3rem',
    cursor: 'pointer'
  },
  detailPage: {
    transition: 'margin 0.5s',
    marginRight: '1rem',
  },
  detailPageOpen: {
    marginRight: '324px',
    paddingRight: '1rem',
  },
  sidebarContent: {
    width: 0,
    transition: '0.5s',
    overflowX: 'hidden',
    flex: 1,
    paddingTop: '0.35rem',
    paddingBottom: '0.35rem',
    color: colors.primary,
  },
  sidebarContentOpen: {
    width: '370px',
    borderLeft: '1rem solid #728FA3',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  sidebarHandleOpen: {
    marginLeft: '0.25rem',
    width: '359px',
  },
});

interface Props {
  workspace: WorkspaceData;
}

interface State {
  sidebarOpen: boolean;
  participant: Participant;
}

export const DetailPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {sidebarOpen: true, participant: null};
      this.setParticipant = this.setParticipant.bind(this);
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

    get angleDir() {
      return this.state.sidebarOpen ? 'right' : 'left';
    }

    toggleSidebar() {
      const sidebarOpen = !this.state.sidebarOpen;
      this.setState({sidebarOpen});
    }

    setParticipant(v) {
      this.setState({participant: v});
    }

    render() {
      const {participant, sidebarOpen} = this.state;
      return <React.Fragment>
        {!participant ? <SpinnerOverlay /> : <React.Fragment>
          <div className={'detail-page ' + (sidebarOpen ? 'sidebar-open' : '')}
               style={{...styles.detailPage, ...(sidebarOpen ? styles.detailPageOpen : {})}}>
            <DetailHeader participant={participant}>
            </DetailHeader>
            <DetailTabs>
            </DetailTabs>
          </div>
          <div style={styles.detailSidebar}>
            <div style={styles.sidebarHandle} onClick={() => this.toggleSidebar()}>
              <ClrIcon style={{color: 'white'}} shape='angle-double' dir={this.angleDir} size='29'/>
            </div>
            <div id='review-sidebar-content'
              style={{...styles.sidebarContent, ...(sidebarOpen ? styles.sidebarContentOpen : {})}}>
              <SidebarContent
                participant={participant}
                setParticipant={this.setParticipant}>
              </SidebarContent>
            </div>
          </div>
        </React.Fragment>}
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
