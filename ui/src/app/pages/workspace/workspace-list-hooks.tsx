import {Component} from '@angular/core';

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
  ReactWrapperBase,
  withUserProfile
} from 'app/utils';
import {convertAPIError} from 'app/utils/errors';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import * as React from 'react';
import RSelect from 'react-select';
import * as fp from 'lodash';
import { r } from '@angular/core/src/render3';

const {useState, useEffect} = React;

const withBusyState = fp.curry((setBusy, wrappedFn) => async (...args) => {
  setBusy(true);
  try {
    await wrappedFn(...args);
  } finally {
    setBusy(false);
  }
});

const withErrorText = fp.curry((setError, wrappedFn) => async(...args) => {
  try {
    await wrappedFn(...args);
  } catch (e) {
    setError(e);
  }
});

const loadWorkspaces = async({filter, setWorkspaceList}) => {
  filter = filter ? filter : (() => true);
  const workspacesReceived = (await workspacesApi().getWorkspaces()).items.filter(response => filter(response.accessLevel));
  workspacesReceived.sort((a, b) => a.workspace.name.localeCompare(b.workspace.name));
  setWorkspaceList(workspacesReceived.map(w => new WorkspacePermissions(w)));
  throw new Error('test');
};

const styles = reactStyles({
  fadeBox: {
    margin: '1rem auto 0 auto', width: '97.5%', padding: '0 1rem'
  },
  cardArea: {
    display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'
  }
});

export const FnWorkspaceList = ({ profileState: { profile } }) => {
  const [error, setError] = useState('');
  const [workspaceList, setWorkspaceList] = useState([]);
  const [loadingWorkspaces, setLoadingWorkspaces] = useState();
  const reloadWorkspaces = fp.flow(
    withBusyState(setLoadingWorkspaces),
    withErrorText(async e => {
      const response = await convertAPIError(e);
      setError(response.message);
    })
  )(loadWorkspaces);

  useEffect(() => {
    reloadWorkspaces({filter: null, setError, setWorkspaceList});
  }, []);

  // Maps each "Filter by" dropdown element to a set of access levels to display.
  const filters = [
    { label: 'Owner',  value: ['OWNER'] },
    { label: 'Writer', value: ['WRITER'] },
    { label: 'Reader', value: ['READER'] },
    { label: 'All',    value: ['OWNER', 'READER', 'WRITER'] },
  ];
  const defaultFilter = filters.find(f => f.label === 'All');
console.log(error);
  return <React.Fragment>
    <FadeBox style={styles.fadeBox}>
      <div style={{padding: '0 1rem'}}>
        <ListPageHeader>Workspaces</ListPageHeader>
        <FlexRow style={{marginTop: '0.5em'}}>
          <div style={{margin: '0', padding: '0.5em 0.75em 0 0'}}>Filter by</div>
          <RSelect options={filters}
            defaultValue={defaultFilter}
            onChange={(levels) => {
              reloadWorkspaces({
                filter: (level: any) => levels.value.includes(level),
                setError,
                setWorkspaceList
              });
            }}/>
        </FlexRow>
        {error && <AlertDanger>
          <ClrIcon shape='exclamation-circle'/>
          {error}
        </AlertDanger>}
        <div style={styles.cardArea}>
          {loadingWorkspaces ?
            (<Spinner style={{width: '100%', marginTop: '1.5rem'}}/>) :
            (<div style={{display: 'flex', marginTop: '1.5rem', flexWrap: 'wrap'}}>
              <NewWorkspaceButton />
              {workspaceList.map(wp => {
                return <WorkspaceCard
                  key={wp.workspace.namespace}
                  workspace={wp.workspace}
                  accessLevel={wp.accessLevel}
                  userEmail={profile.username}
                  reload={ () => reloadWorkspaces({filter: null, setError, setWorkspaceList}) }
                />;
              })}
            </div>)}
        </div>
      </div>
    </FadeBox>
  </React.Fragment>;

};

export const WorkspaceList = withUserProfile()(FnWorkspaceList);


@Component({
  template: '<div #root></div>'
})
export class WorkspaceListComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceList, []);
  }
}
