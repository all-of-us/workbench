import {Component} from '@angular/core';
import * as HighCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as moment from 'moment';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {
  Error as ErrorDiv,
  TextInput
} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {clusterApi, workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, UrlParamsProps, withUrlParams} from 'app/utils';
import {
  getSelectedPopulations,
  getSelectedResearchPurposeItems
} from 'app/utils/research-purpose';
import {
  AdminFederatedWorkspaceDetailsResponse,
  CloudStorageTraffic, ListClusterResponse,
} from 'generated/fetch';
import {ReactFragment} from 'react';

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

const PurpleLabel = ({style = {}, children}) => {
  return <label style={{color: colors.primary, ...style}}>
    {...children}
  </label>;
};

interface State {
  workspaceDetails?: AdminFederatedWorkspaceDetailsResponse;
  cloudStorageTraffic?: CloudStorageTraffic;
  loadingData?: boolean;
  clusterToDelete?: ListClusterResponse;
  confirmDeleteCluster?: boolean;
  dataLoadError?: Response;
}

class AdminWorkspaceImpl extends React.Component<UrlParamsProps, State> {
  constructor(props) {
    super(props);

    this.state = {
      workspaceDetails: {},
      cloudStorageTraffic: null,
    };
  }

  componentDidMount() {
    this.getFederatedWorkspaceInformation();
  }

  async getFederatedWorkspaceInformation() {
    const {urlParams: { workspaceNamespace } } = this.props;

    this.setState({
      loadingData: true,
    });

    try {
      // Fire off both requests in parallel
      const workspaceDetailsPromise = workspaceAdminApi().getFederatedWorkspaceDetails(workspaceNamespace);
      const cloudStorageTrafficPromise = workspaceAdminApi().getCloudStorageTraffic(workspaceNamespace);
      // Wait for both promises to complete before updating state.
      const workspaceDetails = await workspaceDetailsPromise;
      const cloudStorageTraffic = await cloudStorageTrafficPromise;
      this.setState({cloudStorageTraffic, workspaceDetails});
    } catch (error) {
      if (error instanceof Response) {
        console.log('error', error, await error.json());
        this.setState({dataLoadError: error});
      }
    } finally {
      this.setState({loadingData: false});
    }
  }

  maybeGetFederatedWorkspaceInformation(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      return this.getFederatedWorkspaceInformation();
    }
  }

  workspaceInfoField(labelText, divContents) {
    return <FlexRow style={{width: '80%', maxWidth: '1000px'}}>
      <PurpleLabel
        style={{
          width: '250px',
          minWidth: '180px',
          textAlign: 'right',
          marginRight: '1rem',
        }}
      >
        {labelText}
      </PurpleLabel>
      <div
        style={{
          flex: 1,
          wordWrap: 'break-word',
        }}
      >
        {divContents}
      </div>
    </FlexRow>;
  }

  renderHighChart(cloudStorageTraffic: CloudStorageTraffic): ReactFragment {
    HighCharts.setOptions({
      global: {
        useUTC: false,
      },
      lang: {
        decimalPoint: '.',
        thousandsSep: ','
      },
    });
    const options = {
      animation: false,
      chart: {
        animation: false,
        height: '150px',
      },
      credits: {
        enabled: false,
      },
      legend: {
        enabled: false,
      },
      title: {
        text: undefined,
      },
      tooltip: {
        xDateFormat: '%A, %b %e, %H:%M',
        valueDecimals: 0,
      },
      xAxis: {
        min: moment().subtract(6, 'hours').valueOf(),
        max: moment().valueOf(),
        title: {
          enabled: false,
        },
        type: 'datetime',
        zoomEnabled: false,
      },
      yAxis: {
        title: {
          enabled: false,
        },
        zoomEnabled: false,
      },
      series: [{
        data: cloudStorageTraffic.receivedBytes.map(x => [x.timestamp, x.value]),
        lineWidth: 0.5,
        name: 'GCS received bytes'
      }]
    };
    return <div style={{width: '500px', zIndex: 1001}}>
      <HighchartsReact highcharts={HighCharts} options={options}/>
    </div>;
  }

  private async deleteCluster() {
    await clusterApi().deleteClustersInProject(
      this.props.urlParams.workspaceNamespace,
      {clustersToDelete: [this.state.clusterToDelete.clusterName]});
    this.setState({clusterToDelete: null});
    await this.getFederatedWorkspaceInformation();
  }

  private cancelDeleteCluster() {
    this.setState({
      confirmDeleteCluster: false,
      clusterToDelete: null
    });
  }

  render() {
    const {
      cloudStorageTraffic,
      clusterToDelete,
      confirmDeleteCluster,
      loadingData,
      dataLoadError,
      workspaceDetails: {collaborators, resources, workspace},
    } = this.state;
    return <div style={{marginTop: '1rem', marginBottom: '1rem'}}>

      {dataLoadError &&
        <ErrorDiv>
          Error loading data. Please refresh the page or contact the development team.
        </ErrorDiv>
      }
      {loadingData && <SpinnerOverlay />}

      {workspace &&
        <div>
          <h2>Workspace</h2>
          <h3>Basic Information</h3>
          <div className='basic-info' style={{marginTop: '1rem'}}>
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
          </div>
          <h3>Collaborators</h3>
          <div className='collaborators' style={{marginTop: '1rem'}}>
            {collaborators.map((userRole, i) =>
              <div key={i}>
                {userRole.email + ': ' + userRole.role}
              </div>
            )}
          </div>
          <h3>Cohort Builder</h3>
          <div className='cohort-builder' style={{marginTop: '1rem'}}>
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
          </div>
          <h3>Cloud Storage Objects</h3>
          <div className='cloud-storage-objects' style={{marginTop: '1rem'}}>
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
          </div>
          <h3>Research Purpose</h3>
          <div className='research-purpose' style={{marginTop: '1rem'}}>
            {
              this.workspaceInfoField(
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
              this.workspaceInfoField(
                'Reason for choosing All of Us',
                workspace.researchPurpose.reasonForAllOfUs
              )
            }
            {
              this.workspaceInfoField(
                'Area of intended study',
                workspace.researchPurpose.intendedStudy
              )
            }
            {
              this.workspaceInfoField(
                'Anticipated findings',
                workspace.researchPurpose.anticipatedFindings
              )
            }
            {
              workspace.researchPurpose.population && this.workspaceInfoField(
                'Population area(s) of focus',
                getSelectedPopulations(workspace.researchPurpose).map((selectedPopulation, i) => <div key={i}>{selectedPopulation}</div>)
              )
            }
          </div>
        </div>
      }

      {cloudStorageTraffic && cloudStorageTraffic.receivedBytes &&
        <div>
          <h2>Cloud Storage Traffic</h2>
          <div>Cloud Storage <i>received_bytes_count</i> over the past 6 hours.</div>
          {this.renderHighChart(cloudStorageTraffic)}
        </div>
      }

      {resources && resources.clusters.length === 0 && <div>
        <h2>Clusters</h2>
        <p>No active clusters exist for this workspace.</p>
      </div>
      }
      {resources && resources.clusters.length > 0 && <div>
        <h2>Clusters</h2>
        <FlexColumn>
          <FlexRow>
            <PurpleLabel style={styles.narrowWithMargin}>Cluster Name</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Google Project</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Created Time</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Last Accessed Time</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Status</PurpleLabel>
          </FlexRow>
          {resources.clusters.map((cluster, i) =>
              <FlexRow key={i}>
                <div style={styles.narrowWithMargin}>{cluster.clusterName}</div>
                <div style={styles.narrowWithMargin}>{cluster.googleProject}</div>
                <div style={styles.narrowWithMargin}>{new Date(cluster.createdDate).toDateString()}</div>
                <div style={styles.narrowWithMargin}>{new Date(cluster.dateAccessed).toDateString()}</div>
                <div style={styles.narrowWithMargin}>{cluster.status}</div>
                <Button onClick={() =>
                  this.setState({clusterToDelete: cluster, confirmDeleteCluster: true})}
                  disabled={clusterToDelete && clusterToDelete.clusterName === cluster.clusterName}>
                  Delete
                </Button>
              </FlexRow>
          )}
        </FlexColumn>
      </div>
      }
      {confirmDeleteCluster &&
        <Modal onRequestClose={() => this.cancelDeleteCluster()}>
          <ModalTitle>Delete Cluster</ModalTitle>
          <ModalBody>
            This will immediately delete the given cluster. This will disrupt the user's work
            and may cause data loss.<br/><br/><b>Are you sure?</b>
          </ModalBody>
          <ModalFooter>
            <Button type='secondary'
                    onClick={() => this.cancelDeleteCluster()}>Cancel</Button>
            <Button style={{marginLeft: '0.5rem'}}
                    onClick={() => {
                      this.setState({confirmDeleteCluster: false});
                      this.deleteCluster();
                    }}
            >Delete</Button>
          </ModalFooter>
      </Modal>}
    </div>;
  }
}

export const AdminWorkspace = withUrlParams()(AdminWorkspaceImpl);

@Component({
  template: '<div #root></div>'
})
export class AdminWorkspaceComponent extends ReactWrapperBase {
  constructor() {
    super(AdminWorkspace, []);
  }
}
