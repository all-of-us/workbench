import * as React from 'react';
import * as fp from 'lodash/fp';

import { withRouteData } from 'app/components/app-router';
import { BreadcrumbType } from 'app/components/breadcrumb-type';
import { withRoutingSpinner } from 'app/components/with-routing-spinner';
import { DataComponent } from 'app/pages/data/data-component';
import { DataComponentTanagra } from 'app/pages/data/tanagra-dev/data-component-tanagra';
import { withCurrentWorkspace } from 'app/utils';

export const DataComponentSplitter = fp.flow(withCurrentWorkspace())(
  ({ workspace: { usesTanagra } }) => {
    const DataComponentToReturn = fp.flow(
      withRouteData,
      withRoutingSpinner
    )(usesTanagra ? DataComponentTanagra : DataComponent);
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
  }
);
