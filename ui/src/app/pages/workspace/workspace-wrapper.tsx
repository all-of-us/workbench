import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {SpinnerOverlay} from 'app/components/spinners';
import {DataPage} from 'app/pages/data/data-page';
import {WorkspaceAbout} from 'app/pages/workspace/workspace-about';
import {WorkspaceNavBarReact} from 'app/pages/workspace/workspace-nav-bar';
import {WorkspaceShare} from 'app/pages/workspace/workspace-share';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withUrlParams, withUserProfile} from 'app/utils';
import {currentWorkspaceStore, navigate, routeConfigDataStore} from 'app/utils/navigation';
import {Profile, UserRole, Workspace, WorkspaceAccessLevel} from 'generated/fetch';

export interface State {
  accessLevel: WorkspaceAccessLevel;
  deleting: boolean;
  displayNavBar: boolean;
  sharing: boolean;
  tabPath: string;
  userRoles: UserRole[];
  userRolesLoading: boolean;
  workspace: Workspace;
  workspaceDeletionError: boolean;
}

export const WorkspaceWrapper = fp.flow(withUrlParams(), withUserProfile())
(class extends React.Component<{urlParams: any, profileState: {profile: Profile, reload: Function}}, State> {
  constructor(props) {
    super(props);
    this.state = {
      displayNavBar: false,
      deleting: false,
      workspace: undefined,
      accessLevel: undefined,
      tabPath: '',
      userRolesLoading: false,
      userRoles: [],
      sharing: false,
      workspaceDeletionError: false
    };
  }

  componentDidMount() {
    this.getTabPath();
    this.getWorkspace();
    this.setState({displayNavBar: !routeConfigDataStore.getValue().minimizeChrome});
  }

  async getWorkspace() {
    const {urlParams: {ns, wsid}} = this.props;
    workspacesApi().getWorkspace(ns, wsid).then(wsResponse => {
      currentWorkspaceStore.next({...wsResponse.workspace, accessLevel: wsResponse.accessLevel});
      this.setState({workspace: wsResponse.workspace, accessLevel: wsResponse.accessLevel});
    });

  }

  // TODO: use the router to get this when it is converted to React
  getTabPath() {
    this.setState({tabPath: window.location.pathname.split('/').splice(-1).pop()});
  }

  // The function called when the "share" action is called from the workspace nav bar menu dropdown
  async handleShareAction() {
    const {workspace} = this.state;
    this.setState({userRolesLoading: true});
    const userRolesResponse = await workspacesApi().getFirecloudWorkspaceUserRoles(
      workspace.namespace,
      workspace.id);
    // Trigger the sharing dialog to be shown.
    this.setState({userRolesLoading: false, userRoles: userRolesResponse.items, sharing: true});
  }

  delete(workspace: Workspace): void {
    workspacesApi().deleteWorkspace(
      workspace.namespace, workspace.id).then(() => {
      navigate(['/workspaces']);
    }).catch(() => {
      this.setState({workspaceDeletionError: true});
    });
  }

  render() {
    const {accessLevel, deleting, displayNavBar, sharing, tabPath, userRoles,
      workspace} = this.state;
    return <React.Fragment>
      {displayNavBar &&
      <WorkspaceNavBarReact shareFunction={() => this.handleShareAction()} tabPath={tabPath}
          deleteFunction={() => this.setState({deleting: true})}/>}
      {workspace ? <React.Fragment>
        {deleting && <ConfirmDeleteModal
            closeFunction={() => this.setState({deleting: false})}
            resourceType='workspace' receiveDelete={() => this.delete(workspace)}
            resourceName={workspace.name}/>}
        {sharing && <WorkspaceShare workspace={workspace} accessLevel={accessLevel}
            userRoles={userRoles} userEmail={this.props.profileState.profile}/>}
      </React.Fragment> :
      <SpinnerOverlay/>}
    </React.Fragment>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class WorkspaceWrapperComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceWrapper, []);
  }
}
