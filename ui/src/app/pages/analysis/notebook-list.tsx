import * as React from 'react';

import {CardButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

import {NewNotebookModal} from 'app/pages/analysis/new-notebook-modal';
import {profileApi, notebooksApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';

import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {NotebookResourceCard} from 'app/pages/analysis/notebook-resource-card';
import {AnalyticsTracker} from 'app/utils/analytics';
import {convertToResource} from 'app/utils/resources';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {BillingStatus, FileDetail, ResourceType} from 'generated/fetch';
import {FlexColumn} from 'app/components/flex';
import {faExclamationTriangle} from '@fortawesome/pro-solid-svg-icons';

const styles = {
  heading: {
    color: colors.primary,
    fontSize: 20, fontWeight: 600, lineHeight: '24px'
  },
  cloneNotebookCard: {
    backgroundColor: colorWithWhiteness(colors.warning, 0.75),
    minWidth: '200px',
    maxWidth: '200px',
    minHeight: '223px',
    maxHeight: '223px',
    border: 'none'
  },
  cloneNotebookMsg: {
    color: colors.primary,
    fontSize: '12px',
    fontWeight: 600,
    paddingTop: '0.5rem'
  }
};

const NOTEBOOK_TRANSFER_CHECK_INTERVAL = 3000;

interface Props extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
}

export const NotebookList = withCurrentWorkspace()(class extends React.Component<
  Props, {
    notebookList: FileDetail[],
    notebookNameList: string[],
    creating: boolean,
    loading: boolean,
    showWaitingForNotebookTransferMsg: boolean;
  }> {
  private interval: NodeJS.Timeout;
  constructor(props) {
    super(props);
    this.state = {
      notebookList: [],
      notebookNameList: [],
      creating: false,
      loading: false,
      showWaitingForNotebookTransferMsg: false
    };
  }

  componentDidMount() {
    this.props.hideSpinner();
    profileApi().updatePageVisits({page: 'notebook'});
    this.confirmNotebookTransferIsDone();
  }

  componentDidUpdate(prevProps) {
    if (this.workspaceChanged(prevProps)) {
      this.loadNotebooks();
    }
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  async confirmNotebookTransferIsDone() {
    await workspacesApi()
        .notebookTransferComplete(this.props.workspace.namespace, this.props.workspace.id).then((transferDone) => {
          if (!transferDone) {
            // Set the interval so that file transfer check is done after every 3 sec
            this.interval = setInterval(this.tick, NOTEBOOK_TRANSFER_CHECK_INTERVAL);
            this.setState({loading: true, showWaitingForNotebookTransferMsg: true});
          } else {
            // Notebook transfer is done load the notebooks
            this.loadNotebooks();
          }
        });
  }

  // Will execute after every 3 sec: Call getCloneFileTransferDetails to check if the notebook file transfer is done
  tick = () => {
    workspacesApi()
        .notebookTransferComplete(this.props.workspace.namespace, this.props.workspace.id).then((time)=>{
          if (!!time) {
            this.setState({loading: false, showWaitingForNotebookTransferMsg: false});
            clearInterval(this.interval);
            this.loadNotebooks();
          }
        }
    );
  }

  private workspaceChanged(prevProps) {
    return this.props.workspace.namespace !== prevProps.workspace.namespace ||
      this.props.workspace.id !== prevProps.workspace.id;
  }

  private async loadNotebooks() {
    try {
      const {workspace: {namespace, id}} = this.props;
      this.setState({loading: true});
      const notebookList = await notebooksApi().getNoteBookList(namespace, id);
      this.setState({notebookList});
      const notebookNameList = notebookList.map(fd => fd.name.slice(0, -('.ipynb'.length)));
      this.setState({notebookNameList});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({loading: false});
    }
  }

  private canWrite(): boolean {
    return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
  }

  private disabledCreateButtonText(): string {
    if (this.props.workspace.billingStatus === BillingStatus.INACTIVE) {
      return ACTION_DISABLED_INVALID_BILLING;
    } else if (!this.canWrite()) {
      return 'Write permission required to create notebooks';
    }
  }

  render() {
    const {workspace} = this.props;
    const {notebookList, notebookNameList, creating, loading, showWaitingForNotebookTransferMsg} = this.state;
    return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
      <div style={styles.heading}>
        Notebooks&nbsp;
        <TooltipTrigger
          content={`A Notebook is a computational environment where you
            can analyze data with basic programming knowledge in R or Python.`}
        ><InfoIcon size={16} /></TooltipTrigger>
      </div>
      <div style={{display: 'flex', alignItems: 'flex-start'}}>
        <TooltipTrigger content={this.disabledCreateButtonText()}>
          <CardButton
            disabled={workspace.billingStatus === BillingStatus.INACTIVE || !this.canWrite()}
            type='small'
            onClick={() => {
              AnalyticsTracker.Notebooks.OpenCreateModal();
              this.setState({creating: true});
            }}
          >
            Create a<br/>New Notebook
            <ClrIcon shape='plus-circle' size={21} style={{marginTop: 5}} />
          </CardButton>
        </TooltipTrigger>
        <div style={{display: 'flex', flexWrap: 'wrap', justifyContent: 'flex-start' }}>
          {showWaitingForNotebookTransferMsg
              ? <CardButton
                  disabled={showWaitingForNotebookTransferMsg}
                  style={styles.cloneNotebookCard}
                  type='small'
                  onClick={() => {
                  }}>
                <FlexColumn style={{paddingTop: '0.5rem'}}>
                  <div>
                    <FontAwesomeIcon style={{color: colors.warning}}
                                        icon={faExclamationTriangle} size="2x"/>
                  </div>
                  <div style={styles.cloneNotebookMsg}>
                    Copying 1 or more notebooks from another workspace. This may take <b> a few
                    minutes</b>.
                  </div>
                </FlexColumn>
              </CardButton>
              : notebookList.map((notebook, index) => {
                return <NotebookResourceCard
                    key={index}
                    resource={convertToResource(notebook, ResourceType.NOTEBOOK, workspace)}
                    existingNameList={notebookNameList}
                    onUpdate={() => this.loadNotebooks()}
                    disableDuplicate={workspace.billingStatus === BillingStatus.INACTIVE}
                />;
              })}
        </div>
      </div>
      {loading && <SpinnerOverlay />}
      {creating && <NewNotebookModal
        workspace={workspace}
        existingNameList={notebookNameList}
        onClose={() => this.setState({creating: false})}
      />}
    </FadeBox>;
  }
});
