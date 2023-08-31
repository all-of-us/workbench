import * as React from 'react';
import * as fp from 'lodash/fp';

import { withRouteData } from 'app/components/app-router';
import { BreadcrumbType } from 'app/components/breadcrumb-type';
import { withRoutingSpinner } from 'app/components/with-routing-spinner';
import { DataComponent } from 'app/pages/data/data-component';
import { DataComponentTanagra } from 'app/pages/data/tanagra-dev/data-component-tanagra';
import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';

export const DataComponentSplitter = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(({ cdrVersionTiersResponse, workspace: { cdrVersionId } }) => {
  const DataComponentToReturn = fp.flow(
    withRouteData,
    withRoutingSpinner
  )(
    findCdrVersion(cdrVersionId, cdrVersionTiersResponse)?.tanagraEnabled
      ? DataComponentTanagra
      : DataComponent
  );
  return (
    <DataComponentToReturn
      routeData={{
        title: 'Data Page',
        breadcrumb: BreadcrumbType.Workspace,
        workspaceNavBarTab: 'data',
        pageKey: 'data',
      }}
    />
  );
});
