import {Component} from '@angular/core';
import * as React from 'react';

import {
  DomainInfo,
  RecentResource,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import {conceptsApi, conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';

import {FadeBox} from 'app/components/containers';
import {ResourceCard} from 'app/components/resource-card';
import {SpinnerOverlay} from 'app/components/spinners';
import {ConceptNavigationBar} from 'app/pages/data/concept/concept-navigation-bar';
import {convertToResources, ResourceType} from 'app/utils/resourceActions';
import {WorkspaceData} from 'app/utils/workspace-data';

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
      this.getDomainInfo();
      this.loadConceptSets();
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
      const {conceptSetsLoading, conceptSetsList} = this.state;
      const {namespace, id} = this.props.workspace;

      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <ConceptNavigationBar wsId={id} ns={namespace} showConcepts={false}/>
        <div style={styles.pageArea}>
          <div style={styles.resourceCardArea}>
            {conceptSetsList && conceptSetsList.map((conceptSet: RecentResource) => {
              return <ResourceCard resourceCard={conceptSet} key={conceptSet.conceptSet.name}
                                   onDuplicateResource={(duplicating) => this.setState({
                                     conceptSetsLoading: duplicating
                                   })}
                                   onUpdate={() => this.loadConceptSets()}>
              </ResourceCard>;
            })}
          </div>
          {conceptSetsLoading && <SpinnerOverlay />}
        </div>
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
