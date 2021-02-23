import {Component} from '@angular/core';
import * as HighCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Error as ErrorDiv, TextArea} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {runtimeApi, workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, UrlParamsProps, withUrlParams} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {
  getSelectedPopulations,
  getSelectedPrimaryPurposeItems
} from 'app/utils/research-purpose';
import {
  CloudStorageTraffic,
  FileDetail,
  ListRuntimeResponse,
  WorkspaceAdminView,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {ReactFragment, useState} from 'react';

const styles = reactStyles({
  infoRow: {
    width: '80%',
    maxWidth: '1000px'
  },
  infoLabel: {
    width: '300px',
    minWidth: '180px',
    textAlign: 'right',
    marginRight: '1rem',
  },
  infoValue: {
    flex: 1,
    wordWrap: 'break-word',
  },
  wideWithMargin: {
    width: '20rem',
    marginRight: '1rem'
  },
  narrowWithMargin: {
    width: '10rem',
    marginRight: '1rem'
  },
  fileDetailsTable: {
    maxWidth: '1000px',
    marginTop: '1rem',
  },
  accessReasonText: {
    maxWidth: '1000px',
    height: '3rem',
  },
  previewButton: {
    marginLeft: '20px',
    height: '1rem',
  },
});

const PurpleLabel = ({style = {}, children}) => {
  return <label style={{color: colors.primary, ...style}}>
    {...children}
  </label>;
};

const WorkspaceInfoField = ({labelText, children}) => {
  return <FlexRow style={styles.infoRow}>
    <PurpleLabel style={styles.infoLabel}>{labelText}</PurpleLabel>
    <div style={styles.infoValue}>{...children}</div>
  </FlexRow>;
};

const formatMB = (fileSize: number): string => {
  const mb = fileSize / 1000000.0;
  if (mb < 1.0) {
    return '<1';
  } else {
    return mb.toFixed(2);
  }
};

const parseLocation = (file: FileDetail, bucket: string): string => {
  const prefixLength = bucket.length;
  const start = prefixLength + 1;  // slash after bucket name
  const suffixPos = file.path.lastIndexOf(file.name);
  const end = suffixPos - 1;  // slash before filename

  return file.path.substring(start, end);
};

const NOTEBOOKS_DIRECTORY = 'notebooks';
const NOTEBOOKS_SUFFIX = '.ipynb';
const MAX_NOTEBOOK_READ_SIZE_BYTES = 5 * 1000 * 1000; // see NotebooksServiceImpl

interface NameCellProps {
  file: FileDetail;
  bucket: string;
  workspaceNamespace: string;
  accessReason: string;
}

const NameCell = (props: NameCellProps) => {
  const {file, bucket, workspaceNamespace, accessReason} = props;
  const filename = file.name.trim();

  const filenameSpan = () => <span>{filename}</span>;

  const fileTooLarge = () => <FlexRow>
    {filenameSpan()}
    <TooltipTrigger
        content={`Files larger than ${formatMB(MAX_NOTEBOOK_READ_SIZE_BYTES)} MB are too large to preview`}
    ><Button style={styles.previewButton} disabled={true}>Preview</Button>
    </TooltipTrigger>
  </FlexRow>;

  const navigateToPreview = () => navigate(
      ['admin', 'workspaces', workspaceNamespace, encodeURIComponent(filename)],
      { queryParams: { accessReason: accessReason } });

  const fileWithPreviewButton = () => <FlexRow>
    {filenameSpan()}
    <TooltipTrigger content='Please enter an access reason below' disabled={accessReason && accessReason.trim()}>
      <Button style={styles.previewButton}
              disabled={!accessReason || !accessReason.trim()}
              onClick={navigateToPreview}>Preview</Button>
    </TooltipTrigger>
  </FlexRow>;

  // remove first check after RW-5626
  const isNotebook = () => (NOTEBOOKS_DIRECTORY === parseLocation(file, bucket)) && filename.endsWith(NOTEBOOKS_SUFFIX);
  const isTooLargeNotebook = () => isNotebook() && (file.sizeInBytes > MAX_NOTEBOOK_READ_SIZE_BYTES);

  // if (tooLarge()) fileTooLarge();
  // else if (isNotebook()) fileWithPreviewButton();
  // else filenameSpan();
  const requiredDummyParameter = undefined;
  return fp.cond([
    [isTooLargeNotebook, fileTooLarge],
    [isNotebook, fileWithPreviewButton],
    [fp.stubTrue, filenameSpan]
  ])(requiredDummyParameter);
};

interface FileDetailsProps {
  workspaceNamespace: string;
  data: Array<FileDetail>;
  bucket: string;
}

const FileDetailsTable = (props: FileDetailsProps) => {
  const {workspaceNamespace, data, bucket} = props;

  interface TableEntry {
    location: string;
    rawName: string;
    nameCell: JSX.Element;
    size: string;
  }

  const initTable = (accessReason: string): Array<TableEntry> => {
    return data
      .map(file => {
        return {
          location: parseLocation(file, bucket),
          rawName: file.name,
          nameCell: <NameCell
              file={file}
              bucket={bucket}
              workspaceNamespace={workspaceNamespace}
              accessReason={accessReason}/>,
          size: formatMB(file.sizeInBytes),
        }; })
      .sort((a, b) => {
        const locationComparison = a.location.localeCompare(b.location);
        return locationComparison === 0 ? a.rawName.localeCompare(b.rawName) : locationComparison;
      });
  };

  const [tableData, setTable] = useState<Array<TableEntry>>(initTable(''));

  return <FlexColumn>
    <DataTable
        data-test-id='object-details-table'
        value={tableData}
        style={styles.fileDetailsTable}
        scrollable={true}
        paginator={true}
        paginatorTemplate='CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown'
        currentPageReportTemplate='Showing {first} to {last} of {totalRecords} entries'>
      <Column field='location' header='Location'/>
      <Column field='nameCell' header='Filename'/>
      <Column field='size' header='File size (MB)' style={{textAlign: 'right'}}/>
    </DataTable>
    <PurpleLabel>To preview notebooks, enter Access Reason (for auditing purposes)</PurpleLabel>
    <TextArea style={styles.accessReasonText} onChange={v => setTable(initTable(v))}/>
  </FlexColumn>;
};

interface State {
  workspaceDetails?: WorkspaceAdminView;
  cloudStorageTraffic?: CloudStorageTraffic;
  loadingData?: boolean;
  runtimeToDelete?: ListRuntimeResponse;
  confirmDeleteRuntime?: boolean;
  dataLoadError?: Response;
  files?: Array<FileDetail>;
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
      // Fire off all requests in parallel
      const workspaceDetailsPromise = workspaceAdminApi().getWorkspaceAdminView(workspaceNamespace);
      const cloudStorageTrafficPromise = workspaceAdminApi().getCloudStorageTraffic(workspaceNamespace);
      const filesPromise = workspaceAdminApi().listFiles(workspaceNamespace);
      // Wait for all promises to complete before updating state.
      const [workspaceDetails, cloudStorageTraffic, files] =
          await Promise.all([workspaceDetailsPromise, cloudStorageTrafficPromise, filesPromise]);
      this.setState({workspaceDetails, cloudStorageTraffic, files});
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

  renderHighChart(cloudStorageTraffic: CloudStorageTraffic): ReactFragment {
    HighCharts.setOptions({
      time: {
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

  private async deleteRuntime() {
    await runtimeApi().deleteRuntimesInProject(
      this.props.urlParams.workspaceNamespace,
      {runtimesToDelete: [this.state.runtimeToDelete.runtimeName]});
    this.setState({runtimeToDelete: null});
    await this.getFederatedWorkspaceInformation();
  }

  private cancelDeleteRuntime() {
    this.setState({
      confirmDeleteRuntime: false,
      runtimeToDelete: null
    });
  }

  render() {
    const {
      cloudStorageTraffic,
      runtimeToDelete,
      confirmDeleteRuntime,
      loadingData,
      dataLoadError,
      files,
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
            <WorkspaceInfoField labelText='Workspace Name'>{workspace.name}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Google Project Id'>{workspace.namespace}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Billing Status'>{workspace.billingStatus}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Billing Account Type'>{workspace.billingAccountType}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Creation Time'>{new Date(workspace.creationTime).toDateString()}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Last Modified Time'>{new Date(workspace.lastModifiedTime).toDateString()}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Workspace Published'>{workspace.published ? 'Yes' : 'No'}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='Audit'>
              <a href={`/admin/workspace-audit/${workspace.namespace}`}>Audit History</a>
            </WorkspaceInfoField>
          </div>
          <h3>Collaborators</h3>
          <div className='collaborators' style={{marginTop: '1rem'}}>
            {collaborators.map((workspaceUserAdminView, i) =>
              <div key={i}>
                {workspaceUserAdminView.userModel.userName + ': ' + workspaceUserAdminView.role}
              </div>
            )}
          </div>
          <h3>Cohort Builder</h3>
          <div className='cohort-builder' style={{marginTop: '1rem'}}>
            <WorkspaceInfoField labelText='# of Cohorts'>{resources.workspaceObjects.cohortCount}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='# of Concept Sets'>{resources.workspaceObjects.conceptSetCount}</WorkspaceInfoField>
            <WorkspaceInfoField labelText='# of Data Sets'>{resources.workspaceObjects.datasetCount}</WorkspaceInfoField>
          </div>
          <h3>Cloud Storage Objects</h3>
          <div className='cloud-storage-objects' style={{marginTop: '1rem'}}>
            <div style={{color: colors.warning, fontWeight: 'bold', maxWidth: '1000px'}}>
              NOTE: if there are more than ~1000 files in the bucket, these counts and the table below may be
              incomplete because we process only a single page of storage list results.
            </div>
            <WorkspaceInfoField labelText='GCS bucket path'>
              {resources.cloudStorage.storageBucketPath}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='# of Workbench-managed notebook files'>
              {resources.cloudStorage.notebookFileCount}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='# of other files'>
              {resources.cloudStorage.nonNotebookFileCount}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Storage used (MB)'>
              {formatMB(resources.cloudStorage.storageBytesUsed)}
            </WorkspaceInfoField>
          </div>
          {files && <FileDetailsTable
              workspaceNamespace={workspace.namespace}
              data={files}
              bucket={resources.cloudStorage.storageBucketPath}/>}
          <h3>Research Purpose</h3>
          <div className='research-purpose' style={{marginTop: '1rem'}}>
            <WorkspaceInfoField labelText='Primary purpose of project'>
              {getSelectedPrimaryPurposeItems(workspace.researchPurpose).map((researchPurposeItem, i) =>
                  <div key={i}>{researchPurposeItem}</div>)}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Reason for choosing All of Us'>
              {workspace.researchPurpose.reasonForAllOfUs}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Area of intended study'>
              {workspace.researchPurpose.intendedStudy}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Anticipated findings'>
              {workspace.researchPurpose.anticipatedFindings}
            </WorkspaceInfoField>
            {workspace.researchPurpose.populationDetails.length > 0 &&
              <WorkspaceInfoField labelText='Population area(s) of focus'>
                {getSelectedPopulations(workspace.researchPurpose)}
              </WorkspaceInfoField>}
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

      {resources && resources.runtimes.length === 0 && <div>
        <h2>Runtimes</h2>
        <p>No active runtimes exist for this workspace.</p>
      </div>
      }
      {resources && resources.runtimes.length > 0 && <div>
        <h2>Runtimes</h2>
        <FlexColumn>
          <FlexRow>
            <PurpleLabel style={styles.narrowWithMargin}>Runtime Name</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Google Project</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Created Time</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Last Accessed Time</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Status</PurpleLabel>
          </FlexRow>
          {resources.runtimes.map((runtime, i) =>
              <FlexRow key={i}>
                <div style={styles.narrowWithMargin}>{runtime.runtimeName}</div>
                <div style={styles.narrowWithMargin}>{runtime.googleProject}</div>
                <div style={styles.narrowWithMargin}>{new Date(runtime.createdDate).toDateString()}</div>
                <div style={styles.narrowWithMargin}>{new Date(runtime.dateAccessed).toDateString()}</div>
                <div style={styles.narrowWithMargin}>{runtime.status}</div>
                <Button onClick={() =>
                  this.setState({runtimeToDelete: runtime, confirmDeleteRuntime: true})}
                  disabled={runtimeToDelete && runtimeToDelete.runtimeName === runtime.runtimeName}>
                  Delete
                </Button>
              </FlexRow>
          )}
        </FlexColumn>
      </div>
      }
      {confirmDeleteRuntime &&
        <Modal onRequestClose={() => this.cancelDeleteRuntime()}>
          <ModalTitle>Delete Runtime</ModalTitle>
          <ModalBody>
            This will immediately delete the given runtime. This will disrupt the user's work
            and may cause data loss.<br/><br/><b>Are you sure?</b>
          </ModalBody>
          <ModalFooter>
            <Button type='secondary'
                    onClick={() => this.cancelDeleteRuntime()}>Cancel</Button>
            <Button style={{marginLeft: '0.5rem'}}
                    onClick={() => {
                      this.setState({confirmDeleteRuntime: false});
                      this.deleteRuntime();
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
