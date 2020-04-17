
import {
  Profile
} from 'generated/fetch';

import {AlertDanger} from 'app/components/alert';
import {FadeBox} from 'app/components/containers';
import {FlexRow} from 'app/components/flex';
import {ListPageHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {NewWorkspaceButton} from 'app/pages/workspace/new-workspace-button';
import {WorkspaceCard} from 'app/pages/workspace/workspace-card';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {
  reactStyles,
  withUserProfile
} from 'app/utils';
import {convertAPIError} from 'app/utils/errors';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import * as React from 'react';
import RSelect from 'react-select';

const styles = reactStyles({
  fadeBox: {
    margin: '1rem auto 0 auto', width: '97.5%', padding: '0 1rem'
  },
  cardArea: {
    display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'
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
      const response = await convertAPIError(e);
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
          <FlexRow style={{marginTop: '0.5em'}}>
            <div style={{margin: '0', padding: '0.5em 0.75em 0 0'}}>Filter by</div>
            <RSelect options={filters}
              defaultValue={defaultFilter}
              onChange={(levels) => {
                this.reloadWorkspaces(level => levels.value.includes(level));
              }}/>
          </FlexRow>
          {errorText && <AlertDanger>
            <ClrIcon shape='exclamation-circle'/>
            {errorText}
          </AlertDanger>}
          <div style={styles.cardArea}>
            {workspacesLoading ?
              (<Spinner style={{width: '100%', marginTop: '1.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '1.5rem', flexWrap: 'wrap'}}>
                <NewWorkspaceButton />
                {workspaceList.map(wp => {
                  return <WorkspaceCard
                    key={wp.workspace.namespace}
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

