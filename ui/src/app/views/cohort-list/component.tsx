import {Component} from '@angular/core';

import {ResourceCard} from 'app/views/resource-card/component';

import {
  RecentResource,
  WorkspaceAccessLevel
} from 'generated/fetch';

import {convertToResources, ResourceType} from 'app/utils/resourceActionsReact';

import {cohortsApi} from 'app/services/swagger-fetch-clients';

import {ClrIcon} from 'app/components/icons';

import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';

import {CardButton} from 'app/components/buttons';
import {navigate} from 'app/utils/navigation';
import * as React from 'react';

import {FadeBox} from 'app/components/containers';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils/index';
import {WorkspaceData} from 'app/utils/workspace-data';

const styles = reactStyles({
  pageArea: {
    display: 'flex',
    justifyContent: 'flex-start'
  },

  cohortsHeader: {
    color: '#2F2E7E',
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
    paddingTop: '0.5rem'
  },

  resourceCardArea: {
    display: 'flex',
    flexWrap: 'wrap'
  },

  infoIcon: {
    color: '#2691D0',
    cursor: 'pointer',
    marginLeft: '0.2rem',
    height: '16px',
    width: '16px'
  }
});

export const CohortList = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData},
      {cohortList: RecentResource[], cohortsLoading: boolean}> {
    constructor(props) {
      super(props);
      this.state = {
        cohortsLoading: false,
        cohortList: undefined
      };
    }

    componentDidMount() {
      this.reloadCohorts();
    }

    async reloadCohorts() {
      try {
        const {namespace, id, accessLevel} = this.props.workspace;
        this.setState({
          cohortsLoading: true
        });
        const resp = await cohortsApi().getCohortsInWorkspace(namespace, id);
        this.setState({
          cohortList: convertToResources(resp.items, namespace,
            id, accessLevel as unknown as WorkspaceAccessLevel, ResourceType.COHORT),
        });
      } catch (error) {
        console.log(error);
      } finally {
        this.setState({
          cohortsLoading: false
        });
      }
    }

    navigateToCohortBuilder() {
      const {namespace, id} = this.props.workspace;
      navigate(['workspaces', namespace, id, 'cohorts', 'build']);
    }

    render() {
      const {cohortsLoading, cohortList} = this.state;
      const accessLevel = this.props.workspace.accessLevel as unknown as WorkspaceAccessLevel;

      const writePermission = accessLevel === WorkspaceAccessLevel.OWNER ||
        accessLevel === WorkspaceAccessLevel.WRITER;

      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <h3 style={styles.cohortsHeader}>
          Cohorts
          <TooltipTrigger content={`A “Cohort” is a group of participants you
              are interested in researching. The cohort builder allows you to
              create and review cohorts and annotate participants in your study group.`}>
            <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
          </TooltipTrigger>
        </h3>
        <div style={styles.pageArea}>
          <TooltipTrigger content={!writePermission &&
              `Write permission required to create cohorts`} side='top'>
            <CardButton type='small'
                        onClick={() => this.navigateToCohortBuilder()}
                        disabled={!writePermission}>
              Create a <br/>New Cohort
              <ClrIcon shape='plus-circle' size={21} style={{marginTop: 5}}/>
            </CardButton>
          </TooltipTrigger>
          <div style={styles.resourceCardArea}>
            {cohortList && cohortList.map((cohort: RecentResource) => {
              return <ResourceCard resourceCard={cohort} key={cohort.cohort.id}
                 onUpdate={() => this.reloadCohorts()}>
              </ResourceCard>;
            })}
          </div>
          {cohortsLoading && <SpinnerOverlay />}
        </div>
      </FadeBox>;
    }
  });

@Component({
  template: '<div #root></div>'
})
export class CohortListComponent extends ReactWrapperBase {
  constructor() {
    super(CohortList, []);
  }
}
