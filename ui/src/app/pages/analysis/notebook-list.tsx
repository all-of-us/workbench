import {Component} from '@angular/core';
import * as React from 'react';

import {CardButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';

import {NewNotebookModal} from 'app/pages/analysis/new-notebook-modal';
import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {convertToResource} from 'app/utils/resourceActions';
import {WorkspaceData} from 'app/utils/workspace-data';


import {NotebookResourceCard} from 'app/pages/analysis/notebook-resource-card';
import {AnalyticsTracker} from 'app/utils/analytics';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {BillingStatus, FileDetail, ResourceType, WorkspaceAccessLevel} from 'generated/fetch';

const styles = {
  heading: {
    color: colors.primary,
    fontSize: 20, fontWeight: 600, lineHeight: '24px'
  }
};

export const NotebookList = withCurrentWorkspace()(class extends React.Component<{
  workspace: WorkspaceData
}, {
  notebookList: FileDetail[],
  notebookNameList: string[],
  creating: boolean,
  loading: boolean
}> {
  constructor(props) {
    super(props);
    this.state = {notebookList: [], notebookNameList: [], creating: false, loading: false};
  }

  componentDidMount() {
    profileApi().updatePageVisits({page: 'notebook'});
    this.loadNotebooks();
  }

  componentDidUpdate(prevProps) {
    if (this.workspaceChanged(prevProps)) {
      this.loadNotebooks();
    }
  }

  private workspaceChanged(prevProps) {
    return this.props.workspace.namespace !== prevProps.workspace.namespace ||
      this.props.workspace.id !== prevProps.workspace.id;
  }

  private async loadNotebooks() {
    try {
      const {workspace: {namespace, id}} = this.props;
      this.setState({loading: true});
      const notebookList = await workspacesApi().getNoteBookList(namespace, id);
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
    const {workspace, workspace: {namespace, id, accessLevel}} = this.props;
    const {notebookList, notebookNameList, creating, loading} = this.state;
    // TODO Remove this cast when we switch to fetch types
    const al = accessLevel as unknown as WorkspaceAccessLevel;
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
          {notebookList.map((notebook, index) => {
            return <NotebookResourceCard
              key={index}
              resource={convertToResource(notebook, namespace, id, al, ResourceType.NOTEBOOK)}
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

@Component({
  template: '<div #root></div>'
})
export class NotebookListComponent extends ReactWrapperBase {
  constructor() {
    super(NotebookList, []);
  }
}
