import {Component} from '@angular/core';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {BugReportModal} from 'app/views/bug-report';
import {Profile, Workspace} from 'generated/fetch';

const styles = reactStyles({
  tableStyle: {
    fontSize: 12,
    minWidth: 1100
  },
  colStyle: {
    lineHeight: '0.5rem',
    fontSize: 12
  }
});

/**
 * Review Workspaces. Users with the REVIEW_RESEARCH_PURPOSE permission use this
 * to view other users' workspaces for which a review has been requested, and approve/reject them.
 */
export const AdminReviewWorkspace = withUserProfile()(class extends React.Component<
  {profileState: {profile: Profile, reload: Function, updateCache: Function}},
  {contentLoaded: boolean, workspaces: Workspace[], fetchingWorkspaceError: boolean,
    reviewedWorkspace: Workspace, reviewError: boolean}> {

  constructor(props) {
    super(props);

    this.state = {
      contentLoaded: false,
      workspaces: [],
      fetchingWorkspaceError: false,
      reviewedWorkspace: null,
      reviewError: false,
    };
  }

  async componentDidMount() {
    try {
      const resp = await workspacesApi().getWorkspacesForReview();
      this.setState({workspaces: resp.items, contentLoaded: true});
    } catch (error) {
      this.setState({fetchingWorkspaceError: true});
    }
  }

  async approve(workspace: Workspace, approved: boolean) {
    const {workspaces} = this.state;
    this.setState({reviewedWorkspace: workspace});
    try {
      await workspacesApi()
        .reviewWorkspace(workspace.namespace, workspace.id, {approved});
      const i = workspaces.indexOf(workspace, 0);
      if (i >= 0) {
        workspaces.splice(i, 1);
        this.setState({workspaces: workspaces, reviewedWorkspace: null});
      }
    } catch (error) {
      this.setState({reviewError: true});
    }
  }

  convertWorkspaceToFields(workspaces: Workspace[]) {
    return workspaces.map(ws => ({...ws, description: <div>
        <i>Field of intended study:</i>
        <br/>{ws.researchPurpose.intendedStudy}<br/>
        <i>Reason for choosing All of Us:</i>
        <br/>{ws.researchPurpose.reasonForAllOfUs}<br/>
        <i>Anticipated findings:</i>
        <br/>{ws.researchPurpose.anticipatedFindings}<br/>
    </div>, actions: <div>
        {this.state.reviewedWorkspace === ws ? <Spinner size={50}/> :
        <React.Fragment>
          <Button onClick={() => this.approve(ws, true)}
                  data-test-id='approve'>Approve</Button>
          <Button type='secondary' onClick={() => this.approve(ws, false)}
                style={{marginLeft: '0.5rem'}}>Reject</Button>
        </React.Fragment>}
      </div>}));
  }

  render() {
    const {contentLoaded, fetchingWorkspaceError, workspaces,
      reviewError, reviewedWorkspace} = this.state;
    return <div style={{position: 'relative'}}>
      <h2>Review Workspaces</h2>
      {contentLoaded ?
        <DataTable value={this.convertWorkspaceToFields(workspaces)} style={styles.tableStyle}
                   data-test-id='reviewWorkspacesTable'>
          <Column field='name' header='Workspace Name' headerStyle={{width: '20%'}}
                  bodyStyle={{...styles.colStyle, fontSize: 14, fontWeight: 600}}
                  sortable={true} data-test-id='workspaceName'/>
          <Column field='creator' header='Workspace Author' headerStyle={{width: '20%'}}
                  sortable={true}/>
          <Column field='description' header='Research Purpose' headerStyle={{width: '40%'}}/>
          <Column field='actions' header='Approve/Reject' headerStyle={{width: '20%'}}
                  bodyStyle={{textAlign: 'center'}} data-test-id='actionButtons'/>
        </DataTable> :
        <div>
          Loading workspaces for review...
          <SpinnerOverlay overrideStylesOverlay={{alignItems: 'flex-start', marginTop: '2rem'}}/>
        </div>
      }
      {fetchingWorkspaceError &&
      <BugReportModal bugReportDescription='Could not fetch workspaces for approval'
        onClose={() => this.setState({fetchingWorkspaceError: false})}/>}
      {reviewError &&
      <BugReportModal bugReportDescription={'Could not review workspace ' +
          reviewedWorkspace.namespace + '/' + reviewedWorkspace.name}
        onClose={() => this.setState({reviewError: false, reviewedWorkspace: null})}/>}
    </div>;
  }
});


@Component({
  template: '<div #root></div>'
})
export class AdminReviewWorkspaceComponent extends ReactWrapperBase {
  constructor() {
    super(AdminReviewWorkspace, []);
  }
}
