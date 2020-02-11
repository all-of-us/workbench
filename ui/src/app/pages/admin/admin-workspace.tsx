import * as React from 'react';

import {Component} from '@angular/core';

import {Button, Clickable} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {
  ResearchPurposeItems,
  SpecificPopulationItems
} from 'app/pages/workspace/workspace-edit';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import colors from "app/styles/colors";
import {ReactWrapperBase} from 'app/utils';

import {
  AdminWorkspaceResources, BillingAccountType,
  UserRole,
  Workspace
} from 'generated/fetch';
import {ClrIcon} from "../../components/icons";
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems
} from "../../utils/research-purpose";

const styles = {
  columnWithRightMargin: {
    width: '10rem',
    marginRight: '1rem'
  },
  dropdownIcon: {
    marginRight: '1rem',
    transform: 'rotate(180deg)',
    transition: 'transform 0.5s',
    color: colors.primary
  },
  dropdownIconOpen: {
    transform: 'rotate(0deg)',
  }
};

interface Props {}
interface State {
  googleProject: string;
  workspace: Workspace;
  collaborators: Array<UserRole>;
  resources: AdminWorkspaceResources;
  detailsOpen: boolean;
}

const FlexColumnWithRightMargin = ({style={}, children}) => {
  return <FlexColumn style={{...styles.columnWithRightMargin, ...style}}>
    {...children}
  </FlexColumn>
};

const LabelWithPrimaryColor = ({style={}, children}) => {
  return <label style={{color: colors.primary, ...style}}>
    {...children}
  </label>
}

export class AdminWorkspace extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      googleProject: '',
      workspace: null,
      collaborators: [],
      resources: {
        workspaceObjects: {
          cohortCount: 0,
          conceptSetCount: 0,
          datasetCount: 0
        },
        cloudStorage: {
          notebookFileCount: 0,
          nonNotebookFileCount: 0,
          storageBytesUsed: 0
        },
        clusters: []
      },
      detailsOpen: false
    };
  }

  updateProject(newProject: string) {
    this.setState({googleProject: newProject});
  }

  getFederatedWorkspaceInformation() {
    workspaceAdminApi().getFederatedWorkspaceDetails(this.state.googleProject).then(
      response => {
        this.setState({
          workspace: response.workspace,
          collaborators: response.collaborators,
          resources: response.resources
        });
      }
    ).catch(error => {
      console.error(error);
    });
  }

  maybeGetFederatedWorkspaceInformation(event) {
    if (event.key === 'Enter') {
      return this.getFederatedWorkspaceInformation();
    }
  }

  render() {
    const {workspace, collaborators, resources, detailsOpen} = this.state;
    return <div>
      <h2>Manage Workspaces</h2>
      <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center'}}>
        <label style={{marginRight: '1rem'}}>Google Project ID</label>
        <TextInput
            style={styles.columnWithRightMargin}
            onChange={value => this.updateProject(value)}
            onKeyDown={event => this.maybeGetFederatedWorkspaceInformation(event)}
        />
        <Button
            style={{height: '1.5rem'}}
            onClick={() => this.getFederatedWorkspaceInformation()}
        >
          Search
        </Button>
      </FlexRow>
      {
        workspace && <FlexColumn>
          <h3>Workspace Admin Actions</h3>
          <FlexRow style={{justifyContent: 'space-between'}}>
            <Button>Shut down all VMs</Button>
            <Button>Disable workspace</Button>
            <Button>Disable all collaborators</Button>
            <Button>Exclude from public directory</Button>
            <Button>Log administrative comment</Button>
            <Button>Publish workspace</Button>
          </FlexRow>
        </FlexColumn>
      }
      {workspace && (resources.workspaceObjects || resources.cloudStorage) &&
        <FlexColumn style={{flex: '1 0 auto'}}>
          <h3>Basic Information</h3>
          <FlexRow style={{width: '100%'}}>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Workspace Name</LabelWithPrimaryColor>
              <div>{workspace.name}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Google Project ID</LabelWithPrimaryColor>
              <div>{workspace.namespace}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Billing Status</LabelWithPrimaryColor>
              <div>{workspace.billingStatus}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Billing Account Type</LabelWithPrimaryColor>
              <div>{workspace.billingAccountType}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Created Time</LabelWithPrimaryColor>
              <div>{new Date(workspace.creationTime).toDateString()}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Last Modified Time</LabelWithPrimaryColor>
              <div>{new Date(workspace.lastModifiedTime).toDateString()}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor>Workspace Published</LabelWithPrimaryColor>
              <div>{workspace.published ? 'Yes' : 'No'}</div>
            </FlexColumnWithRightMargin>
          </FlexRow>
        </FlexColumn>
      }
      {workspace && <Clickable
          onClick={() => {
            this.setState(previousState => ({
              detailsOpen: !previousState.detailsOpen
            }));
          }}
        >
          <FlexRow style={{alignItems: 'flex-end'}}>
            <h3>Details</h3>
            <ClrIcon
                shape='angle'
                style={
                  this.state.detailsOpen
                      ? {...styles.dropdownIcon, ...styles.dropdownIconOpen}
                      : styles.dropdownIcon
                }
                size={21}
            />
          </FlexRow>
        </Clickable>
      }
      {detailsOpen && <FlexColumn>
          <h3>Research Purpose</h3>
          <FlexRow>
            <FlexColumnWithRightMargin style={{width: '21rem'}}>
              <LabelWithPrimaryColor>Primary purpose of project</LabelWithPrimaryColor>
              {getSelectedResearchPurposeItems(workspace).map((researchPurposeItem, i) => <div key={i}>{researchPurposeItem}</div>)}
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin style={{width: '21rem'}}>
              <LabelWithPrimaryColor>Reason for choosing <i>All of Us</i></LabelWithPrimaryColor>
              <div style={{wordWrap: 'break-word'}}>{workspace.researchPurpose.reasonForAllOfUs}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin style={{width: '21rem'}}>
              <LabelWithPrimaryColor>Area of intended study</LabelWithPrimaryColor>
              <div style={{wordWrap: 'break-word'}}>{workspace.researchPurpose.intendedStudy}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin style={{width: '21rem'}}>
              <LabelWithPrimaryColor>Anticipated findings</LabelWithPrimaryColor>
              <div style={{wordWrap: 'break-word'}}>{workspace.researchPurpose.anticipatedFindings}</div>
            </FlexColumnWithRightMargin>
            {
              workspace.researchPurpose.population && <FlexColumnWithRightMargin style={{width: '21rem'}}>
                <LabelWithPrimaryColor>Population area(s) of focus</LabelWithPrimaryColor>
                {getSelectedPopulations(workspace).map((selectedPopulation, i) => <div key={i}>{selectedPopulation}</div>)}
              </FlexColumnWithRightMargin>
            }
          </FlexRow>
        </FlexColumn>
      }
      {detailsOpen && resources.workspaceObjects && <FlexColumn>
          <h3>Workspace Objects</h3>
          <FlexRow style={{width: '100%'}}>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor># of Cohorts</LabelWithPrimaryColor>
              <div>{resources.workspaceObjects.cohortCount}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor># of Concept Sets</LabelWithPrimaryColor>
              <div>{resources.workspaceObjects.conceptSetCount}</div>
            </FlexColumnWithRightMargin>
            <FlexColumnWithRightMargin>
              <LabelWithPrimaryColor># of Data Sets</LabelWithPrimaryColor>
              <div>{resources.workspaceObjects.datasetCount}</div>
            </FlexColumnWithRightMargin>
          </FlexRow>
        </FlexColumn>
      }
      {detailsOpen && resources.cloudStorage && <FlexColumn>
        <h3>Cloud Storage</h3>
        <FlexRow style={{width: '100%'}}>
          <FlexColumnWithRightMargin>
            <LabelWithPrimaryColor># of Notebook Files</LabelWithPrimaryColor>
            <div>{resources.cloudStorage.notebookFileCount}</div>
          </FlexColumnWithRightMargin>
          <FlexColumnWithRightMargin>
            <LabelWithPrimaryColor># of Non-Notebook Files</LabelWithPrimaryColor>
            <div>{resources.cloudStorage.nonNotebookFileCount}</div>
          </FlexColumnWithRightMargin>
          <FlexColumnWithRightMargin>
            <LabelWithPrimaryColor>Storage used (bytes)</LabelWithPrimaryColor>
            <div>{resources.cloudStorage.storageBytesUsed}</div>
          </FlexColumnWithRightMargin>
        </FlexRow>
      </FlexColumn>
      }
      {detailsOpen && workspace && collaborators && <FlexColumn>
          <h3>Workspace Collaborators</h3>
          {collaborators.map((userRole, i) =>
              <div key={i}>
                {userRole.email + ': ' + userRole.role}
              </div>
          )}
        </FlexColumn>
      }
      {detailsOpen && workspace && resources.clusters.length > 0 && <FlexColumn>
          <h3>Clusters</h3>
          <FlexRow>
            <label style={{color: colors.primary, ...styles.columnWithRightMargin}}>Cluster Name</label>
            <label style={{color: colors.primary, ...styles.columnWithRightMargin}}>Google Project</label>
            <label style={{color: colors.primary, ...styles.columnWithRightMargin}}>Created Time</label>
            <label style={{color: colors.primary, ...styles.columnWithRightMargin}}>Last Accessed Time</label>
            <label style={{color: colors.primary, ...styles.columnWithRightMargin}}>Status</label>
          </FlexRow>
          {resources.clusters.map((cluster, i) =>
              <FlexRow key={i}>
                <div style={styles.columnWithRightMargin}>{cluster.clusterName}</div>
                <div style={styles.columnWithRightMargin}>{cluster.googleProject}</div>
                <div style={styles.columnWithRightMargin}>{new Date(cluster.createdDate).toDateString()}</div>
                <div style={styles.columnWithRightMargin}>{new Date(cluster.dateAccessed).toDateString()}</div>
                <div style={styles.columnWithRightMargin}>{cluster.status}</div>
                <Button>Disable</Button>
              </FlexRow>
          )}
        </FlexColumn>
      }
    </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class AdminWorkspaceComponent extends ReactWrapperBase {
  constructor() {
    super(AdminWorkspace, []);
  }
}
