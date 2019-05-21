import {Component} from '@angular/core';
import * as React from 'react';

import {
  DomainInfo,
  RecentResource,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import {conceptsApi, conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils/index';

import {CreateConceptSetModal} from 'app/views/conceptset-create-modal/component';

import {CardButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {convertToResources, ResourceType} from 'app/utils/resourceActions';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ConceptNavigationBar} from 'app/views/concept-navigation-bar/component';
import {ResourceCard} from 'app/views/resource-card/component';

const styles = reactStyles({
  pageArea: {
    display: 'flex',
    justifyContent: 'flex-start'
  },
  resourceCardArea: {
    display: 'flex',
    flexWrap: 'wrap'
  }
});

export const ConceptSetsList = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData},
      {conceptSetsList: RecentResource[], conceptSetsLoading: boolean,
        conceptDomainList: Array<DomainInfo>, createModalOpen: boolean}> {
    constructor(props) {
      super(props);
      this.state = {
        conceptDomainList: undefined,
        conceptSetsList: undefined,
        conceptSetsLoading: true,
        createModalOpen: false
      };
    }

    componentDidMount() {
      try {
        this.getDomainInfo();
      } finally {
        this.loadConceptSets();
      }
    }

    async getDomainInfo() {
      const {namespace, id} = this.props.workspace;
      try {
        const resp = await conceptsApi().getDomainInfo(namespace, id);
        this.setState({conceptDomainList: resp.items});
      } catch (error) {
        console.log(error);
      }
    }

    async loadConceptSets() {
      const {namespace, id, accessLevel} = this.props.workspace;
      try {
        const resp = await conceptSetsApi().getConceptSetsInWorkspace(namespace, id);
        const conceptSets = convertToResources(resp.items, namespace, id,
          accessLevel as unknown as WorkspaceAccessLevel, ResourceType.CONCEPT_SET);

        this.setState({conceptSetsList: conceptSets, conceptSetsLoading: false});
      } catch (error) {
        console.log(error);
      }
    }

    render() {
      const {conceptSetsLoading, conceptSetsList, conceptDomainList, createModalOpen} = this.state;
      const {namespace, id} = this.props.workspace;
      const accessLevel = this.props.workspace.accessLevel as unknown as WorkspaceAccessLevel;
      const writePermission = accessLevel === WorkspaceAccessLevel.OWNER ||
          accessLevel === WorkspaceAccessLevel.WRITER;

      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <ConceptNavigationBar wsId={id} ns={namespace} showConcepts={false}/>
        <div style={styles.pageArea}>
          <TooltipTrigger content={!writePermission &&
          `Write permission required to create cohorts`} side='top'>
            <CardButton type='small'
                        onClick={() => this.setState({createModalOpen: true})}
                        disabled={!writePermission}>
              Create a <br/>Concept Set
              <ClrIcon shape='plus-circle' size={21} style={{marginTop: 5}}/>
            </CardButton>
          </TooltipTrigger>
          <div style={styles.resourceCardArea}>
            {conceptSetsList && conceptSetsList.map((conceptSet: RecentResource) => {
              return <ResourceCard resourceCard={conceptSet} key={conceptSet.conceptSet.name}
                                   onUpdate={() => this.loadConceptSets()}>
              </ResourceCard>;
            })}
          </div>
          {conceptSetsLoading && <SpinnerOverlay />}
        </div>
        {createModalOpen &&
        <CreateConceptSetModal onCreate={() => this.loadConceptSets()}
                               onClose={() => this.setState({createModalOpen: false})}
                               conceptDomainList={conceptDomainList}
                               existingConceptSets={conceptSetsList}/>}
      </FadeBox>;
    }

  });

@Component({
  template: '<div #root></div>'
})
export class ConceptSetListComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptSetsList, []);
  }
}
