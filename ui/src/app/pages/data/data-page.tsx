import {Component} from '@angular/core';

import * as React from 'react';

import {CardButton, TabButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {ResourceCard} from 'app/components/resource-card';
import {SpinnerOverlay} from 'app/components/spinners';
import {CohortResourceCard} from 'app/pages/data/cohort/cohort-resource-card';
import {DatasetResourceCard} from 'app/pages/data/data-set/dataset-resource-card';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {navigate} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {BillingStatus, WorkspaceAccessLevel, WorkspaceResource} from 'generated/fetch';

const styles = {
  cardButtonArea: {
    display: 'flex',
    alignItems: 'center',
    width: '100%'
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'baseline'
  },
  resourceTypeButton: {
    width: '33%',
    justifyContent: 'flex-start',
    maxWidth: 'none',
    margin: '1.9rem 1rem 0 0',
    minHeight: '325px',
    maxHeight: '325px'
  },
  resourceTypeButtonLast: {
    marginRight: '0rem'
  },
  cardHeaderText: (disabled) => {
    return {
      color: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent,
      fontSize: '20px',
      marginRight: '0.5rem',
      marginTop: '0.5rem'
    };
  },
  cardText: {
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '22px'
  },
  tabContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    width: '100%',
    marginBottom: '0.5rem'
  }
};

enum Tabs {
  SHOWALL = 'SHOW ALL',
  DATASETS = 'DATASETS',
  COHORTS = 'COHORTS',
  COHORTREVIEWS = 'COHORT REVIEWS',
  CONCEPTSETS = 'CONCEPT SETS'
}

const descriptions = {
  datasets: `A dataset is a table containing data about a Cohort that can
  be exported for analysis. `,
  cohorts: `A cohort is a group of participants based on specific criteria.`,
};

const cohortImg = '/assets/images/cohort-diagram.svg';

const dataSetImg = '/assets/images/dataset-diagram.svg';

export const DataPage = withCurrentWorkspace()(class extends React.Component<
  {workspace: WorkspaceData},
  {activeTab: Tabs, resourceList: WorkspaceResource[], isLoading: boolean,
    creatingConceptSet: boolean, existingDataSetName: string[], existingCohortName: string[],
    existingCohortReviewName: string[], existingConceptSetName: string[]}> {

  constructor(props) {
    super(props);
    this.state = {
      activeTab: Tabs.SHOWALL,
      resourceList: [],
      isLoading: true,
      creatingConceptSet: false,
      existingCohortName: [],
      existingCohortReviewName: [],
      existingConceptSetName: [],
      existingDataSetName: []
    };
  }

  componentDidMount() {
    this.loadResources();
  }

  async loadResources() {
    try {
      const {namespace, id} = this.props.workspace;

      this.setState({
        isLoading: true
      });
      const results = await workspacesApi().getCdrSelectors(namespace, id);

      results.map(result => result.modifiedTime ? result.modifiedTime = new Date(Number(result.modifiedTime)).toDateString() : null);
      this.setState({
        existingCohortName: results.filter(resource => resource.cohort !== null && resource.cohort !== undefined)
          .map(resource => resource.cohort.name),
        existingCohortReviewName: results.filter(resource => resource.cohortReview !== null  && resource.cohortReview !== undefined)
          .map(resource => resource.cohortReview.cohortName),
        existingConceptSetName: results.filter(resource => resource.conceptSet !== null && resource.conceptSet !== undefined)
          .map(resource => resource.conceptSet.name),
        existingDataSetName: results.filter(resource => resource.dataSet !== null && resource.dataSet !== undefined)
          .map(resource => resource.dataSet.name),
        resourceList: results
      });
    } catch (error) {
      console.log(error);
    } finally {
      this.setState({
        isLoading: false
      });
    }
  }

  getExistingNameList(resource) {
    if (resource.dataSet) {
      return this.state.existingDataSetName;
    } else if (resource.conceptSet) {
      return this.state.existingConceptSetName;
    } else if (resource.cohort) {
      return this.state.existingCohortName;
    } else if (resource.cohortReview) {
      return this.state.existingCohortReviewName;
    } else {
      return [];
    }
  }

  createResourceCard(resource: WorkspaceResource) {
    if (resource.cohort) {
      return <CohortResourceCard resource={resource}
                                 existingNameList={this.getExistingNameList(resource)}
                                 onUpdate={() => this.loadResources()}/>;
    } if (resource.dataSet) {
      return <DatasetResourceCard resource={resource}
                                  existingNameList={this.getExistingNameList(resource)}
                                  disableExportToNotebook={this.props.workspace.billingStatus === BillingStatus.INACTIVE}
                                  onUpdate={() => this.loadResources()}/>;
    } else {
      return <ResourceCard resourceCard={resource}
                           onDuplicateResource={(duplicating) =>
                             this.setState({isLoading: duplicating})}
                           onUpdate={() => this.loadResources()}
                           existingNameList={this.getExistingNameList(resource)}
      />;
    }
  }

  render() {
    const {accessLevel, namespace, id} = this.props.workspace;
    const {activeTab, isLoading, resourceList} = this.state;

    const writePermission = accessLevel === WorkspaceAccessLevel.OWNER ||
      accessLevel === WorkspaceAccessLevel.WRITER;

    const filteredList = resourceList.filter((resource) => {
      if (activeTab === Tabs.SHOWALL) {
        return true;
      } else if (activeTab === Tabs.COHORTS) {
        return resource.cohort;
      } else if (activeTab === Tabs.COHORTREVIEWS) {
        return resource.cohortReview;
      } else if (activeTab === Tabs.CONCEPTSETS) {
        return resource.conceptSet;
      } else if (activeTab === Tabs.DATASETS) {
        return resource.dataSet;
      }
    });
    return <React.Fragment>
      <div style={{paddingLeft: '1.5rem'}}>
        <div style={styles.cardButtonArea}>
          <TooltipTrigger content={!writePermission &&
          `Write permission required to create cohorts`} side='top'>
            <CardButton style={styles.resourceTypeButton} disabled={!writePermission}
                        onClick={() => {
                          navigate(['workspaces', namespace, id, 'data', 'cohorts', 'build']);
                        }}>
              <div style={styles.cardHeader}>
                <h2 style={styles.cardHeaderText(!writePermission)}>Cohorts</h2>
                <ClrIcon shape='plus-circle' class='is-solid' size={18} style={{marginTop: 5}}/>
              </div>
              <div style={styles.cardText}>
                {descriptions.cohorts}
              </div>
              {/*Because the container can stretch based on window size, but the height
              can't we set a max width to cap the height based on aspect ratio*/}
              <div style={{width: '100%', maxWidth: '425px', paddingTop: '1rem'}}>
                <img src={cohortImg}/>
              </div>
            </CardButton>
          </TooltipTrigger>
          <TooltipTrigger content={!writePermission &&
          `Write permission required to create datasets`} side='top'>
            <CardButton
              style={{...styles.resourceTypeButton, ...styles.resourceTypeButtonLast}}
              disabled={!writePermission}
              onClick={() => {
                AnalyticsTracker.DatasetBuilder.OpenCreatePage();
                navigate(['workspaces', namespace, id, 'data', 'data-sets']);
              }}>
              <div style={styles.cardHeader}>
                <h2 style={styles.cardHeaderText(!writePermission)}>Datasets</h2>
                <ClrIcon shape='plus-circle' class='is-solid' size={18} style={{marginTop: 5}}/>
              </div>
              <div style={styles.cardText}>
                {descriptions.datasets}
              </div>
              {/*Because the container can stretch based on window size, but the height
               can't we set a max width to cap the height based on aspect ratio*/}
              <div style={{width: '100%', maxWidth: '425px', paddingTop: '1.5rem'}}>
                <img src={dataSetImg}/>
              </div>
            </CardButton>
          </TooltipTrigger>
        </div>
      </div>
      <FadeBox style={{marginTop: '1rem'}}>
        <div style={styles.tabContainer}>
          <h2 style={{margin: 0,
            color: colors.primary,
            fontSize: '16px',
            fontWeight: 600}}>Show:</h2>
          <TabButton active={activeTab === Tabs.SHOWALL} onClick={() => {
            this.setState({
              activeTab: Tabs.SHOWALL
            });
          }}>Show All</TabButton>
          <TabButton active={activeTab === Tabs.COHORTS} onClick={() => {
            this.setState({
              activeTab: Tabs.COHORTS
            });
          }} data-test-id='view-only-cohorts'>Cohorts</TabButton>
          <TabButton active={activeTab === Tabs.COHORTREVIEWS} onClick={() => {
            this.setState({
              activeTab: Tabs.COHORTREVIEWS
            });
          }} data-test-id='view-only-cohort-reviews'>Cohort Reviews</TabButton>
          <TabButton active={activeTab === Tabs.CONCEPTSETS} onClick={() => {
            this.setState({
              activeTab: Tabs.CONCEPTSETS
            });
          }} data-test-id='view-only-concept-sets'>Concept Sets</TabButton>
          <TabButton active={activeTab === Tabs.DATASETS} onClick={() => {
            this.setState({
              activeTab: Tabs.DATASETS
            });
          }} data-test-id='view-only-data-sets'>Datasets</TabButton>
        </div>
        <div style={{
          borderBottom: `1px solid ${colors.dark}`,
          marginLeft: '-1rem',
          marginRight: '-1rem',
          opacity: 0.24
        }}>
        </div>
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          position: 'relative',
          minHeight: 247,
          padding: '0 0.5rem 1rem'
        }}>
          {filteredList.map((resource: WorkspaceResource, index: number) => {
            return <div key={index}> {this.createResourceCard(resource)} </div>;
          })}

          {isLoading && <SpinnerOverlay></SpinnerOverlay>}
        </div>
      </FadeBox>
    </React.Fragment>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class DataPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataPage, []);
  }
}
