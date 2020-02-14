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
  fixedWidthWithMargin: {
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
});

const FlexWithMargin = ({style = {}, children}) => {
  return <FlexColumn style={{...styles.fixedWidthWithMargin, ...style}}>
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
  detailsOpen: boolean;
}

export class AdminWorkspace extends React.Component<{}, State> {
  constructor(props) {
    super(props);

    this.state = {
      googleProject: '',
      workspace: null,
      collaborators: [],
      resources: {},
      detailsOpen: false
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

  workspaceInfoField(labelText, divContents, widthMultiplier = 1) {
    const width = 10 * (widthMultiplier - 1) + 9;
    const widthString = width + 'rem';
    return <FlexWithMargin style={{width: widthString}}>
      <PurpleLabel>{labelText}</PurpleLabel>
      <div style={{wordWrap: 'break-word'}}>{divContents}</div>
    </FlexWithMargin>;
  }

  render() {
    const {workspace, collaborators, resources, detailsOpen} = this.state;
    return <div>
      <h2>Manage Workspaces</h2>
      <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center'}}>
        <label style={{marginRight: '1rem'}}>Google Project ID</label>
        <TextInput
            style={styles.fixedWidthWithMargin}
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
      {workspace && (resources.workspaceObjects || resources.cloudStorage) &&
        <FlexColumn style={{flex: '1 0 auto'}}>
          <h3>Basic Information</h3>
          <FlexRow>
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
            {
              this.workspaceInfoField(
                'Primary purpose of project',
                getSelectedResearchPurposeItems(
                  workspace.researchPurpose).map(
                    (
                      researchPurposeItem, i) =>
                      <div key={i}>{researchPurposeItem}</div>
                    ),
                2
                )
            }
            {
              this.workspaceInfoField(
                'Reason for choosing All of Us',
                workspace.researchPurpose.reasonForAllOfUs,
                2
              )
            }
            {
              this.workspaceInfoField(
                'Area of intended study',
                workspace.researchPurpose.intendedStudy,
                2
              )
            }
            {
              this.workspaceInfoField(
                'Anticipated findings',
                workspace.researchPurpose.anticipatedFindings,
                2
              )
            }
            {
              workspace.researchPurpose.population && this.workspaceInfoField(
                'Population area(s) of focus',
                getSelectedPopulations(workspace.researchPurpose).map((selectedPopulation, i) => <div key={i}>{selectedPopulation}</div>),
                2
              )
            }
          </FlexRow>
        </FlexColumn>
      }
      {detailsOpen && resources.workspaceObjects && <FlexColumn>
          <h3>Workspace Objects</h3>
          <FlexRow>
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
          </FlexRow>
        </FlexColumn>
      }
      {detailsOpen && resources.cloudStorage && <FlexColumn>
        <h3>Cloud Storage</h3>
        <FlexRow style={{width: '100%'}}>
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
            <PurpleLabel style={styles.fixedWidthWithMargin}>Cluster Name</PurpleLabel>
            <PurpleLabel style={styles.fixedWidthWithMargin}>Google Project</PurpleLabel>
            <PurpleLabel style={styles.fixedWidthWithMargin}>Created Time</PurpleLabel>
            <PurpleLabel style={styles.fixedWidthWithMargin}>Last Accessed Time</PurpleLabel>
            <PurpleLabel style={styles.fixedWidthWithMargin}>Status</PurpleLabel>
          </FlexRow>
          {resources.clusters.map((cluster, i) =>
              <FlexRow key={i}>
                <div style={styles.fixedWidthWithMargin}>{cluster.clusterName}</div>
                <div style={styles.fixedWidthWithMargin}>{cluster.googleProject}</div>
                <div style={styles.fixedWidthWithMargin}>{new Date(cluster.createdDate).toDateString()}</div>
                <div style={styles.fixedWidthWithMargin}>{new Date(cluster.dateAccessed).toDateString()}</div>
                <div style={styles.fixedWidthWithMargin}>{cluster.status}</div>
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
