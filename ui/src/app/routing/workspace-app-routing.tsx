import * as React from 'react';
import { Redirect, Switch, useParams, useRouteMatch } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { AppRoute, withRouteData } from 'app/components/app-router';
import { withRoutingSpinner } from 'app/components/with-routing-spinner';
import { BreadcrumbType } from 'app/lab/components/breadcrumb-type';
import { DataComponent } from 'app/pages/data/data-component';
import { Migration } from 'app/pages/workspace/migration';
import { MigrationFolderSync } from 'app/pages/workspace/migration-folder-sync';
import { WorkspaceAbout } from 'app/pages/workspace/workspace-about';
import {
  WorkspaceEdit,
  WorkspaceEditMode,
} from 'app/pages/workspace/workspace-edit';
import { adminLockedGuard } from 'app/routing/guards';
import { MatchParams } from 'app/utils/stores';

const DataComponentPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(DataComponent);
const MigrationPage = fp.flow(withRouteData)(Migration);
const MigrationFolderSyncPage = fp.flow(withRouteData)(MigrationFolderSync);
const WorkspaceAboutPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(WorkspaceAbout);
const WorkspaceEditPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(WorkspaceEdit);

export const WorkspaceRoutes = () => {
  const { path } = useRouteMatch();
  const { ns, terraName } = useParams<MatchParams>();

  return (
    <Switch>
      {/* admin-locked workspaces are redirected to /about in most cases */}
      <AppRoute exact path={`${path}/about`}>
        <WorkspaceAboutPage
          routeData={{
            title: 'View Workspace Details',
            breadcrumb: BreadcrumbType.Workspace,
            workspaceNavBarTab: 'about',
            pageKey: 'about',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/duplicate`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <WorkspaceEditPage
          routeData={{
            title: 'Duplicate Workspace',
            breadcrumb: BreadcrumbType.WorkspaceDuplicate,
            pageKey: 'duplicate',
          }}
          workspaceEditMode={WorkspaceEditMode.Duplicate}
        />
      </AppRoute>
      {/* admin-locked workspaces can still be edited */}
      <AppRoute exact path={`${path}/edit`}>
        <WorkspaceEditPage
          routeData={{
            title: 'Edit Workspace',
            breadcrumb: BreadcrumbType.WorkspaceEdit,
            pageKey: 'edit',
          }}
          workspaceEditMode={WorkspaceEditMode.Edit}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <DataComponentPage
          routeData={{
            title: 'Data Page',
            breadcrumb: BreadcrumbType.Workspace,
            workspaceNavBarTab: 'data',
            pageKey: 'data',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/migration`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <MigrationPage
          routeData={{
            title: 'Workspace Migration',
            breadcrumb: BreadcrumbType.Workspace,
            workspaceNavBarTab: 'data',
            pageKey: 'data',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/folder-sync`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <MigrationFolderSyncPage
          routeData={{
            title: 'Workspace Folder Sync',
            breadcrumb: BreadcrumbType.Workspace,
            workspaceNavBarTab: 'data',
            pageKey: 'data',
          }}
        />
      </AppRoute>
      <AppRoute exact={false} path={`${path}`}>
        <Redirect to={'/not-found'} />
      </AppRoute>
    </Switch>
  );
};
