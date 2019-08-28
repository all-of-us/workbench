import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';

import {AddAnnotationDefinitionModal, EditAnnotationDefinitionsModal} from 'app/cohort-review/annotation-definition-modals/annotation-definition-modals.component';
import {DetailHeader} from 'app/cohort-review/detail-header/detail-header.component';
import {DetailTabs} from 'app/cohort-review/detail-tabs/detail-tabs.component';
import {Participant} from 'app/cohort-review/participant.model';
import {SidebarContent} from 'app/cohort-review/sidebar-content/sidebar-content.component';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortAnnotationDefinitionApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CohortAnnotationDefinition, ParticipantCohortAnnotation} from 'generated/fetch';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';

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
  creatingDefinition: boolean;
  editingDefinitions: boolean;
  participant: Participant;
  annotations: ParticipantCohortAnnotation[];
  annotationDefinitions: CohortAnnotationDefinition[];
}

export const DetailPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {
        sidebarOpen: true,
        creatingDefinition: false,
        editingDefinitions: false,
        participant: null,
        annotations: null,
        annotationDefinitions: null
      };
      this.setAnnotations = this.setAnnotations.bind(this);
      this.openCreateDefinitionModal = this.openCreateDefinitionModal.bind(this);
      this.closeCreateDefinitionModal = this.closeCreateDefinitionModal.bind(this);
      this.definitionCreated = this.definitionCreated.bind(this);
      this.openEditDefinitionsModal = this.openEditDefinitionsModal.bind(this);
      this.closeEditDefinitionsModal = this.closeEditDefinitionsModal.bind(this);
      this.setAnnotationDefinitions = this.setAnnotationDefinitions.bind(this);
      this.setParticipant = this.setParticipant.bind(this);
    }

    componentDidMount() {
      const {cdrVersionId} = this.props.workspace;
      this.subscription = urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(params => !!params.pid)
        .switchMap(({ns, wsid, cid, pid}) => {
          return Observable.forkJoin(
            from(cohortReviewApi()
              .getParticipantCohortStatus(ns, wsid,
                cohortReviewStore.getValue().cohortReviewId, +pid))
              .do(ps => {
                this.setState({participant: Participant.fromStatus(ps)});
              }),
            from(cohortReviewApi()
              .getParticipantCohortAnnotations(ns, wsid,
                cohortReviewStore.getValue().cohortReviewId, +pid))
              .do(({items}) => {
                this.setState({annotations: items});
              }),
            this.loadAnnotationDefinitions()
          );
        })
        .subscribe();
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    loadAnnotationDefinitions() {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      return from(cohortAnnotationDefinitionApi().getCohortAnnotationDefinitions(ns, wsid, +cid))
        .do(({items}) => {
          this.setState({annotationDefinitions: items});
        });
    }

    setAnnotations(v) {
      this.setState({annotations: v});
    }

    get angleDir() {
      return this.state.sidebarOpen ? 'right' : 'left';
    }

    toggleSidebar() {
      const sidebarOpen = !this.state.sidebarOpen;
      this.setState({sidebarOpen});
    }

    openCreateDefinitionModal() {
      this.setState({creatingDefinition: true});
    }

    closeCreateDefinitionModal() {
      this.setState({creatingDefinition: false});
    }

    definitionCreated(ad) {
      const annotationDefinitions = this.state.annotationDefinitions.concat([ad]);
      this.setState({annotationDefinitions, creatingDefinition: false});
    }

    openEditDefinitionsModal() {
      this.setState({editingDefinitions: true});
    }

    closeEditDefinitionsModal() {
      this.setState({editingDefinitions: false});
    }

    setAnnotationDefinitions(v) {
      this.setState({annotationDefinitions: v});
    }

    setParticipant(v) {
      this.setState({participant: v});
    }

    render() {
      const {annotations, annotationDefinitions, creatingDefinition, editingDefinitions,
        participant, sidebarOpen} = this.state;
      return <React.Fragment>
        {!(participant && annotations && annotationDefinitions) && <SpinnerOverlay />}
        {participant && annotations && annotationDefinitions && <React.Fragment>
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
                setParticipant={this.setParticipant}
                annotations={annotations}
                annotationDefinitions={annotationDefinitions}
                setAnnotations={this.setAnnotations}
                openCreateDefinitionModal={this.openCreateDefinitionModal}
                openEditDefinitionsModal={this.openEditDefinitionsModal}>
              </SidebarContent>
            </div>
          </div>

          {editingDefinitions && <EditAnnotationDefinitionsModal
            onClose={this.closeEditDefinitionsModal}
            annotationDefinitions={annotationDefinitions}
            setAnnotationDefinitions={this.setAnnotationDefinitions}>
          </EditAnnotationDefinitionsModal>}
          {creatingDefinition && <AddAnnotationDefinitionModal
            annotationDefinitions={annotationDefinitions}
            onCancel={this.closeCreateDefinitionModal}
            onCreate={this.definitionCreated}>
          </AddAnnotationDefinitionModal>}
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
