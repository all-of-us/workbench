import * as React from 'react';

import {Component} from '@angular/core';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import colors from "app/styles/colors";
import {ReactWrapperBase} from 'app/utils';

import {
  AdminWorkspaceResources, BillingAccountType,
  UserRole,
  Workspace
} from 'generated/fetch';

const styles = {
  columnWithRightMargin: {
    width: '10rem',
    marginRight: '1rem'
  }
};

interface Props {}
interface State {
  googleProject: string;
  workspace: Workspace;
  collaborators: Array<UserRole>;
  resources: AdminWorkspaceResources;
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
      }
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
    const {workspace, collaborators, resources} = this.state;
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
            <Button>Disable all users associated with workspace</Button>
            <Button>Exclude workspace from public directory</Button>
            <Button>Log administrative comment</Button>
          </FlexRow>
        </FlexColumn>
      }
      {workspace && (resources.workspaceObjects || resources.cloudStorage) &&
        <FlexColumn style={{flex: '1 0 auto'}}>
          <h3>Workspace Metadata</h3>
          {
            <FlexRow style={{width: '100%'}}>
              <label style={{alignSelf: 'center', color: colors.primary, ...styles.columnWithRightMargin}}>
                Basic Information
              </label>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}>Workspace Name</label>
                <div>{workspace.name}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}>Google Project ID</label>
                <div>{workspace.namespace}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}>Billing Status</label>
                <div>{workspace.billingStatus}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}>Billing Account Type</label>
                <div>{workspace.billingAccountType}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}>Created Time</label>
                <div>{new Date(workspace.creationTime).toDateString()}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}>Last Modified Time</label>
                <div>{new Date(workspace.lastModifiedTime).toDateString()}</div>
              </FlexColumn>
            </FlexRow>
          }
          {resources.workspaceObjects && <FlexRow style={{width: '100%'}}>
              <label style={{alignSelf: 'center', color: colors.primary, ...styles.columnWithRightMargin}}>
                Workspace Objects
              </label>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}># of Cohorts</label>
                <div>{resources.workspaceObjects.cohortCount}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}># of Concept Sets</label>
                <div>{resources.workspaceObjects.conceptSetCount}</div>
              </FlexColumn>
              <FlexColumn style={styles.columnWithRightMargin}>
                <label style={{color: colors.primary}}># of Data Sets</label>
                <div>{resources.workspaceObjects.datasetCount}</div>
              </FlexColumn>
            </FlexRow>
          }
          {resources.cloudStorage && <FlexRow style={{width: '100%'}}>
            <label style={{alignSelf: 'center', color: colors.primary, ...styles.columnWithRightMargin}}>
              Cloud Storage
            </label>
            <FlexColumn style={styles.columnWithRightMargin}>
              <label style={{color: colors.primary}}># of Notebook Files</label>
              <div>{resources.cloudStorage.notebookFileCount}</div>
            </FlexColumn>
            <FlexColumn style={styles.columnWithRightMargin}>
              <label style={{color: colors.primary}}># of Non-Notebook Files</label>
              <div>{resources.cloudStorage.nonNotebookFileCount}</div>
            </FlexColumn>
            <FlexColumn style={styles.columnWithRightMargin}>
              <label style={{color: colors.primary}}>Storage used (bytes)</label>
              <div>{resources.cloudStorage.storageBytesUsed}</div>
            </FlexColumn>
          </FlexRow>
          }
        </FlexColumn>
      }
      {
        workspace && collaborators && <FlexColumn>
          <h3>Workspace Collaborators</h3>
          {collaborators.map((userRole, i) =>
              <div key={i}>
                {userRole.email + ': ' + userRole.role}
              </div>
          )}
        </FlexColumn>
      }
      {
        workspace && resources.clusters && <FlexColumn>
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
