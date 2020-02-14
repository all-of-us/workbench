import * as React from 'react';

import {Component} from '@angular/core';

import {Button, Clickable} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems
} from 'app/utils/research-purpose';

import {
  AdminWorkspaceResources,
  UserRole,
  Workspace
} from 'generated/fetch';


const styles = reactStyles({
  wideWithMargin: {
    width: '20rem',
    marginRight: '1rem'
  },
  narrowWithMargin: {
    width: '10rem',
    marginRight: '1rem'
  }
});

const FlexWithMargin = ({style = {}, children}) => {
  return <FlexColumn style={{...styles.wideWithMargin, ...style}}>
    {...children}
  </FlexColumn>;
};

const PurpleLabel = ({style = {}, children}) => {
  return <label style={{color: colors.primary, ...style}}>
    {...children}
  </label>;
};

interface State {
  googleProject: string;
  workspace: Workspace;
  collaborators: Array<UserRole>;
  resources: AdminWorkspaceResources;
}

export class AdminWorkspace extends React.Component<{}, State> {
  constructor(props) {
    super(props);

    this.state = {
      googleProject: '',
      workspace: null,
      collaborators: [],
      resources: {},
    };
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
    );
  }

  maybeGetFederatedWorkspaceInformation(event) {
    if (event.key === 'Enter') {
      return this.getFederatedWorkspaceInformation();
    }
  }

  workspaceInfoField(labelText, divContents) {
    return <FlexRow style={{width: '100%'}}>
      <PurpleLabel
        style={{
          width: '40%',
          textAlign: 'right',
          marginRight: '1rem',
        }}
      >
        {labelText}
      </PurpleLabel>
      <div
        style={{
          width: '60%',
          wordWrap: 'break-word',
        }}
      >
        {divContents}
      </div>
    </FlexRow>
  }

  researchPurposeField(labelText, divContents) {
    return <FlexWithMargin>
      <PurpleLabel>{labelText}</PurpleLabel>
      <div style={{wordWrap: 'break-word'}}>{divContents}</div>
    </FlexWithMargin>
  }

  render() {
    const {workspace, collaborators, resources} = this.state;
    return <div>
      <h2 style={{margin: 'auto'}}>Manage Workspaces</h2>
      <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center'}}>
        <PurpleLabel style={{marginRight: '1rem'}}>Google Project ID</PurpleLabel>
        <TextInput
            style={{
              width: '10rem',
              marginRight: '1rem'
            }}
            onChange={value => this.setState({googleProject: value})}
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
      {workspace && resources.workspaceObjects && resources.cloudStorage && collaborators &&
        <FlexRow>
          <FlexColumn style={styles.wideWithMargin}>
            <h3>Basic Information</h3>
            {this.workspaceInfoField('Workspace Name', workspace.name)}
            {this.workspaceInfoField('Google Project Id', workspace.namespace)}
            {this.workspaceInfoField('Billing Status', workspace.billingStatus)}
            {this.workspaceInfoField('Billing Account Type', workspace.billingAccountType)}
            {this.workspaceInfoField(
                'Creation Time',
                new Date(workspace.creationTime).toDateString()
            )}
            {this.workspaceInfoField(
                'Last Modified Time',
                new Date(workspace.lastModifiedTime).toDateString()
            )}
            {this.workspaceInfoField(
                'Workspace Published',
                workspace.published ? 'Yes' : 'No'
            )}
          </FlexColumn>
          <FlexColumn style={styles.wideWithMargin}>
            <h3>Workspace Objects</h3>
            {
              this.workspaceInfoField(
                  '# of Cohorts',
                  resources.workspaceObjects.cohortCount
              )
            }
            {
              this.workspaceInfoField(
                  '# of Concept Sets',
                  resources.workspaceObjects.conceptSetCount
              )
            }
            {
              this.workspaceInfoField(
                  '# of Data Sets',
                  resources.workspaceObjects.datasetCount
              )
            }
          </FlexColumn>
          <FlexColumn style={styles.wideWithMargin}>
            <h3>Cloud Storage</h3>
            {
              this.workspaceInfoField(
                  '# of Notebook Files',
                  resources.cloudStorage.notebookFileCount
              )
            }
            {
              this.workspaceInfoField(
                  '# of Non-Notebook Files',
                  resources.cloudStorage.nonNotebookFileCount
              )
            }
            {
              this.workspaceInfoField(
                  'Storage used (bytes)',
                  resources.cloudStorage.storageBytesUsed
              )
            }
          </FlexColumn>
          <FlexColumn style={styles.wideWithMargin}>
            <h3>Workspace Collaborators</h3>
            {collaborators.map((userRole, i) =>
                <div key={i}>
                  {userRole.email + ': ' + userRole.role}
                </div>
            )}
          </FlexColumn>
        </FlexRow>
      }
      {workspace && <FlexColumn>
          <h3>Research Purpose</h3>
          <FlexRow style={{flex: '1 0 auto'}}>
            {
              this.researchPurposeField(
                'Primary purpose of project',
                getSelectedResearchPurposeItems(
                  workspace.researchPurpose).map(
                    (
                      researchPurposeItem, i) =>
                      <div key={i}>{researchPurposeItem}</div>
                    )
                )
            }
            {
              this.researchPurposeField(
                'Reason for choosing All of Us',
                workspace.researchPurpose.reasonForAllOfUs
              )
            }
            {
              this.researchPurposeField(
                'Area of intended study',
                workspace.researchPurpose.intendedStudy
              )
            }
            {
              this.researchPurposeField(
                'Anticipated findings',
                workspace.researchPurpose.anticipatedFindings
              )
            }
            {
              workspace.researchPurpose.population && this.researchPurposeField(
                'Population area(s) of focus',
                getSelectedPopulations(workspace.researchPurpose).map((selectedPopulation, i) => <div key={i}>{selectedPopulation}</div>)
              )
            }
          </FlexRow>
        </FlexColumn>
      }
      {workspace && resources.clusters.length > 0 && <FlexColumn>
          <h3>Clusters</h3>
          <FlexRow>
            <PurpleLabel style={styles.narrowWithMargin}>Cluster Name</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Google Project</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Created Time</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Last Accessed Time</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Status</PurpleLabel>
          </FlexRow>
          {resources.clusters.map((cluster, i) =>
              <FlexRow>
                <div style={styles.narrowWithMargin}>{cluster.clusterName}</div>
                <div style={styles.narrowWithMargin}>{cluster.googleProject}</div>
                <div style={styles.narrowWithMargin}>{new Date(cluster.createdDate).toDateString()}</div>
                <div style={styles.narrowWithMargin}>{new Date(cluster.dateAccessed).toDateString()}</div>
                <div style={styles.narrowWithMargin}>{cluster.status}</div>
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
