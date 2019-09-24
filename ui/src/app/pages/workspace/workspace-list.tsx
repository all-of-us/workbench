import {Component} from '@angular/core';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {navigate} from 'app/utils/navigation';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';

import {AlertDanger} from 'app/components/alert';
import {
  CardButton,
} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ListPageHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {
  reactStyles,
  ReactWrapperBase,
  withUserProfile
} from 'app/utils';
import {
  ErrorResponse,
  Profile
} from 'generated/fetch';
import * as React from 'react';
import RSelect from 'react-select';
import {WorkspaceCard} from "app/pages/workspace/workspace-card";

const styles = reactStyles({
  fadeBox: {
    margin: '1rem auto 0 auto', width: '97.5%', padding: '0 1rem'
  },
  cardArea: {
    display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'
  },
  addCard: {
    margin: '0 1rem 1rem 0', fontWeight: 600, color: 'rgb(33, 111, 180)'
  }
});

export const WorkspaceList = withUserProfile()
(class extends React.Component<
  { profileState: { profile: Profile, reload: Function } },
  { workspacesLoading: boolean,
    workspaceList: WorkspacePermissions[],
    errorText: string,
    firstSignIn: Date,
  }> {
  private timer: NodeJS.Timer;

  constructor(props) {
    super(props);
    this.state = {
      workspacesLoading: true,
      workspaceList: [],
      errorText: '',
      firstSignIn: undefined
    };
  }

  componentDidMount() {
    this.reloadWorkspaces(null);
  }

  componentWillUnmount() {
    clearTimeout(this.timer);
  }

  async reloadWorkspaces(filter) {
    filter = filter ? filter : (() => true);
    this.setState({workspacesLoading: true});
    try {
      const workspacesReceived = (await workspacesApi().getWorkspaces())
        .items.filter(response => filter(response.accessLevel));
      workspacesReceived.sort(
        (a, b) => a.workspace.name.localeCompare(b.workspace.name));
      this.setState({workspaceList: workspacesReceived
          .map(w => new WorkspacePermissions(w))});
      this.setState({workspacesLoading: false});
    } catch (e) {
      const response = ErrorHandlingService.convertAPIError(e) as unknown as ErrorResponse;
      this.setState({errorText: response.message});
    }
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {
      errorText,
      workspaceList,
      workspacesLoading
    } = this.state;

    // Maps each "Filter by" dropdown element to a set of access levels to display.
    const filters = [
      { label: 'Owner',  value: ['OWNER'] },
      { label: 'Writer', value: ['WRITER'] },
      { label: 'Reader', value: ['READER'] },
      { label: 'All',    value: ['OWNER', 'READER', 'WRITER'] },
    ];
    const defaultFilter = filters.find(f => f.label === 'All');

    return <React.Fragment>
      <FadeBox style={styles.fadeBox}>
        <div style={{padding: '0 1rem'}}>
          <ListPageHeader>Workspaces</ListPageHeader>
          <div style={{marginTop: '0.5em', display: 'flex', flexDirection: 'row'}}>
            <div style={{margin: '0', padding: '0.5em 0.75em 0 0'}}>Filter by</div>
            <RSelect options={filters}
              defaultValue={defaultFilter}
              onChange={(levels) => {
                this.reloadWorkspaces(level => levels.value.includes(level));
              }}/>
          </div>
          {errorText && <AlertDanger>
            <ClrIcon shape='exclamation-circle'/>
            {errorText}
          </AlertDanger>}
          <div style={styles.cardArea}>
            {workspacesLoading ?
              (<Spinner style={{width: '100%', marginTop: '1.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '1.5rem', flexWrap: 'wrap'}}>
                <CardButton onClick={() => navigate(['workspaces/build'])}
                            style={styles.addCard}>
                  Create a <br/> New Workspace
                  <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                </CardButton>
                {workspaceList.map(wp => {
                  return <WorkspaceCard
                    key={wp.workspace.name}
                    workspace={wp.workspace}
                    accessLevel={wp.accessLevel}
                    userEmail={profile.username}
                    reload={() => this.reloadWorkspaces(null)}
                  />;
                })}
              </div>)}
          </div>
        </div>
      </FadeBox>
    </React.Fragment>;
  }


});

@Component({
  template: '<div #root></div>'
})
export class WorkspaceListComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceList, []);
  }
}
