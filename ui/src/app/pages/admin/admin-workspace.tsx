import * as React from 'react';

import {Component} from '@angular/core';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase} from 'app/utils';

import {
  AdminWorkspaceResources,
  UserRole,
  Workspace
} from 'generated/fetch';

const styles = {
  width10: {
    width: '10%'
  },
  width30: {
    width: '30%'
  },
  marginRightSmall: {
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
    const {workspace, resources} = this.state;
    return <div>
      <h2>Manage Workspaces</h2>
      <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center'}}>
        <label style={styles.marginRightSmall}>GCP Project ID</label>
        <TextInput
            style={{...styles.width10, ...styles.marginRightSmall}}
            onChange={value => this.updateProject(value)}
            onKeyDown={event => this.maybeGetFederatedWorkspaceInformation(event)}
        />
        <Button
            style={{height: '1.5rem'}}
            onClick={() => this.getFederatedWorkspaceInformation()}
        >
          Find Clusters
        </Button>
      </FlexRow>
      {workspace && (resources.workspaceObjects || resources.cloudStorage) && <FlexColumn style={{flex: '1 0 auto'}}>
        <h3>Workspace Metadata</h3>
        {resources.workspaceObjects && <FlexRow>
            <label style={{alignSelf: 'center', ...styles.width10, ...styles.marginRightSmall}}>Workspace
              Objects</label>
            <FlexColumn styles={{...styles.width30}}>
              <label># of Cohorts</label>
              <div>{resources.workspaceObjects.cohortCount}</div>
            </FlexColumn>
            <FlexColumn styles={{...styles.width30}}>
              <label># of Concept Sets</label>
              <div>{resources.workspaceObjects.conceptSetCount}</div>
            </FlexColumn>
            <FlexColumn styles={{...styles.width30}}>
              <label># of Data Sets</label>
              <div>{resources.workspaceObjects.datasetCount}</div>
            </FlexColumn>
          </FlexRow>
        }
        {resources.cloudStorage && <FlexRow>
          <label style={{alignSelf: 'center', ...styles.width10, ...styles.marginRightSmall}}>Cloud
            Storage</label>
          <FlexColumn styles={{...styles.width30}}>
            <label># of Notebook Files</label>
            <div>{resources.cloudStorage.notebookFileCount}</div>
          </FlexColumn>
          <FlexColumn styles={{...styles.width30}}>
            <label># of Non-Notebook Files</label>
            <div>{resources.cloudStorage.nonNotebookFileCount}</div>
          </FlexColumn>
          <FlexColumn styles={{...styles.width30}}>
            <label>Storage used (bytes)</label>
            <div>{resources.cloudStorage.storageBytesUsed}</div>
          </FlexColumn>
        </FlexRow>
        }
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
