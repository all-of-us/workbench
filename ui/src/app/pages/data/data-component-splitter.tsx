import * as React from 'react';
import * as fp from 'lodash/fp';

import { DataComponent } from 'app/pages/data/data-component';
import { DataComponentTanagra } from 'app/pages/data/data-component-tanagra';
import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';

export const DataComponentSplitter = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(({ cdrVersionTiersResponse, workspace: { cdrVersionId } }) => {
  return findCdrVersion(cdrVersionId, cdrVersionTiersResponse)
    .tanagraEnabled ? (
    <DataComponentTanagra />
  ) : (
    <DataComponent />
  );
});
