import * as React from 'react';
import { Redirect, Switch, useParams, useRouteMatch } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { AppRoute, withRouteData } from 'app/components/app-router';
import { LEONARDO_APP_PAGE_KEY } from 'app/components/help-sidebar';
import { withRoutingSpinner } from 'app/components/with-routing-spinner';
import { BreadcrumbType } from 'app/lab/components/breadcrumb-type';
import { GKEAppLauncher } from 'app/pages/analysis/gke-app-launcher';
import { InteractiveNotebook } from 'app/pages/analysis/interactive-notebook';
import {
  LeoApplicationType,
  LeonardoAppLauncher,
} from 'app/pages/analysis/leonardo-app-launcher';
import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { DataComponent } from 'app/pages/data/data-component';
import { Migration } from 'app/pages/workspace/migration';
import { MigrationFolderSync } from 'app/pages/workspace/migration-folder-sync';
import { WorkspaceAbout } from 'app/pages/workspace/workspace-about';
import {
  WorkspaceEdit,
  WorkspaceEditMode,
} from 'app/pages/workspace/workspace-edit';
import { adminLockedGuard, appIsValidGuard } from 'app/routing/guards';
import { MatchParams, withParamsKey } from 'app/utils/stores';

import { analysisTabName } from './utils';

const DataComponentPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(DataComponent);
const MigrationPage = fp.flow(withRouteData)(Migration);
const MigrationFolderSyncPage = fp.flow(withRouteData)(MigrationFolderSync);
const InteractiveNotebookPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(InteractiveNotebook);
const LeonardoAppRedirectPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(LeonardoAppLauncher);
const LeonardoSparkConsoleRedirectPage = fp.flow(
  withRouteData,
  withRoutingSpinner,
  // Force remounting on parameter change.
  withParamsKey('sparkConsolePath')
)(LeonardoAppLauncher);
const WorkspaceAboutPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(WorkspaceAbout);
const WorkspaceEditPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(WorkspaceEdit);
const GKEAppRedirectPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(GKEAppLauncher);
const AppsListPage = fp.flow(withRouteData)(AppFilesList);

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
        path={`${path}/${analysisTabName}`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <AppsListPage
          routeData={{
            title: 'View Analysis Files',
            pageKey: analysisTabName,
            workspaceNavBarTab: analysisTabName,
            breadcrumb: BreadcrumbType.Workspace,
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/${analysisTabName}/preview/:nbName`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <InteractiveNotebookPage
          routeData={{
            pathElementForTitle: 'nbName',
            breadcrumb: BreadcrumbType.AnalysisPreview,
            pageKey: LEONARDO_APP_PAGE_KEY,
            workspaceNavBarTab: analysisTabName,
            minimizeChrome: true,
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/${analysisTabName}/:nbName`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <LeonardoAppRedirectPage
          key='notebook'
          routeData={{
            pathElementForTitle: 'nbName',
            breadcrumb: BreadcrumbType.Analysis,
            // The iframe we use to display the Jupyter notebook does something strange
            // to the height calculation of the container, which is normally set to auto.
            // Setting this flag sets the container to 100% so that no content is clipped.
            contentFullHeightOverride: true,
            pageKey: LEONARDO_APP_PAGE_KEY,
            workspaceNavBarTab: analysisTabName,
            minimizeChrome: true,
          }}
          leoAppType={LeoApplicationType.JupyterNotebook}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/${analysisTabName}/userApp/:appType`}
        guards={[
          adminLockedGuard(ns, terraName),
          appIsValidGuard(ns, terraName),
        ]}
      >
        <GKEAppRedirectPage
          key='app'
          routeData={{
            pathElementForTitle: 'appType',
            breadcrumb: BreadcrumbType.UserApp,
            // The iframe we use to display the Gke App does something strange
            // to the height calculation of the container, which is normally set to auto.
            // Setting this flag sets the container to 100% so that no content is clipped.
            // This is same as the configuration used for Jupyter iframe
            contentFullHeightOverride: true,
            pageKey: LEONARDO_APP_PAGE_KEY,
            workspaceNavBarTab: analysisTabName,
            minimizeChrome: true,
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/terminals`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <LeonardoAppRedirectPage
          key='terminal'
          routeData={{
            breadcrumb: BreadcrumbType.Workspace,
            pageKey: LEONARDO_APP_PAGE_KEY,
            contentFullHeightOverride: true,
            workspaceNavBarTab: analysisTabName,
            minimizeChrome: true,
          }}
          leoAppType={LeoApplicationType.JupyterTerminal}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/spark/:sparkConsolePath`}
        guards={[adminLockedGuard(ns, terraName)]}
      >
        <LeonardoSparkConsoleRedirectPage
          routeData={{
            breadcrumb: BreadcrumbType.Workspace,
            pageKey: LEONARDO_APP_PAGE_KEY,
            contentFullHeightOverride: true,
            workspaceNavBarTab: analysisTabName,
            minimizeChrome: true,
          }}
          leoAppType={LeoApplicationType.SparkConsole}
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
