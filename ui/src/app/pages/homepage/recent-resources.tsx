import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import {
  CdrVersionTiersResponse,
  Workspace,
  WorkspaceResourceResponse,
  WorkspaceResponse,
} from 'generated/fetch';

import { AlertWarning } from 'app/components/alert';
import { SmallHeader } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { userMetricsApi } from 'app/services/swagger-fetch-clients';
import { cond, withCdrVersions } from 'app/utils';
import { ResourcesList } from 'app/utils/resource-list';

interface Props {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  workspaces: WorkspaceResponse[];
}

export const RecentResources = fp.flow(withCdrVersions())((props: Props) => {
  const [loading, setLoading] = useState(true);
  const [resources, setResources] = useState<WorkspaceResourceResponse>();
  const [wsMap, setWorkspaceMap] = useState<Map<string, Workspace>>();
  const [apiLoadError, setApiLoadError] = useState<string>(null);

  const loadResources = async () => {
    setLoading(true);
    await userMetricsApi()
      .getUserRecentResources()
      .then(setResources)
      .catch(() => {
        setApiLoadError(
          'An error occurred while loading recent resources. Please refresh the page to reload.'
        );
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const { workspaces } = props;
    if (workspaces) {
      const workspaceTuples = workspaces.map(
        (r) => [r.workspace.id, r.workspace] as [string, Workspace]
      );
      setWorkspaceMap(new Map(workspaceTuples));
      loadResources();
    }
  }, [props.workspaces]);

  return cond(
    [
      apiLoadError && !loading,
      () => (
        <AlertWarning style={{ fontSize: 14 }}>
          <ClrIcon className='is-solid' shape='exclamation-triangle' />
          {apiLoadError}
        </AlertWarning>
      ),
    ],
    [
      resources && wsMap && !loading,
      () => (
        <React.Fragment>
          {resources.length > 0 && (
            <SmallHeader>Recently Accessed Items</SmallHeader>
          )}
          <div
            data-test-id='recent-resources-table'
            style={{ paddingTop: '1rem' }}
          >
            <ResourcesList
              recentResourceSource
              workspaceResources={resources}
              workspaceMap={wsMap}
              onUpdate={loadResources}
            />
          </div>
        </React.Fragment>
      ),
    ],
    [loading, () => <SpinnerOverlay />]
  );
});
