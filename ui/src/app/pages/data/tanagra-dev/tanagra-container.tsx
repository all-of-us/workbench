import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { environment } from 'environments/environment';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { getAccessToken } from 'app/utils/authentication';
import { findCdrVersion } from 'app/utils/cdr-versions';
import {
  ExportResources,
  useExitActionListener,
  useExportListener,
  useNavigation,
} from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

export const TanagraContainer = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(({ cdrVersionTiersResponse, hideSpinner, workspace }) => {
  const [navigate] = useNavigation();
  const { 0: splat } = useParams<{ 0: string }>();
  const { cdrVersionId, id, namespace } = workspace;
  const { bigqueryDataset } = findCdrVersion(
    cdrVersionId,
    cdrVersionTiersResponse
  );
  const [exportIds, setExportIds] = useState<ExportResources>();
  const tanagraUrl = `${
    serverConfigStore.get().config.tanagraBaseUrl
  }/tanagra/ui#/tanagra/underlays/aou${bigqueryDataset}/studies/${namespace}/${splat}${
    environment.tanagraLocalAuth ? `/?token=${getAccessToken()}` : ''
  }`;

  useExitActionListener(() => {
    // Navigate to Data tab when exiting Tanagra iframe
    navigate(['workspaces', namespace, id, 'data']);
  });

  useExportListener(async (exportResourceIds) => {
    setExportIds(exportResourceIds);
  });

  return (
    <div
      style={{
        height: '95vh',
      }}
    >
      <iframe
        onLoad={() => hideSpinner()}
        style={{
          border: 0,
          height: '100%',
          width: '100%',
        }}
        src={tanagraUrl}
      />
      {!!exportIds && (
        <ExportDatasetModal
          {...{ workspace }}
          closeFunction={() => setExportIds(undefined)}
          tanagraCohortIds={exportIds.cohortIds}
          tanagraConceptSetIds={exportIds.conceptSetIds}
        />
      )}
    </div>
  );
});
