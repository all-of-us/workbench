import * as React from 'react';
import { Redirect, Switch, useParams, useRouteMatch } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { AppRoute, withRouteData } from 'app/components/app-router';
import { BreadcrumbType } from 'app/components/breadcrumb-type';
import { LEONARDO_APP_PAGE_KEY } from 'app/components/help-sidebar';
import { withRoutingSpinner } from 'app/components/with-routing-spinner';
import { InteractiveNotebook } from 'app/pages/analysis/interactive-notebook';
import {
  LeoApplicationType,
  LeonardoAppLauncher,
} from 'app/pages/analysis/leonardo-app-launcher';
import { NotebookList } from 'app/pages/analysis/notebook-list';
import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { CohortActions } from 'app/pages/data/cohort/cohort-actions';
import { CohortPage } from 'app/pages/data/cohort/cohort-page';
import { CohortReviewPage } from 'app/pages/data/cohort-review/cohort-review-page';
import { DetailPage } from 'app/pages/data/cohort-review/detail-page';
import { QueryReport } from 'app/pages/data/cohort-review/query-report.component';
import { ConceptHomepage } from 'app/pages/data/concept/concept-homepage';
import { ConceptSearch } from 'app/pages/data/concept/concept-search';
import { ConceptSetActions } from 'app/pages/data/concept/concept-set-actions';
import { DataComponent } from 'app/pages/data/data-component';
import { DatasetPage } from 'app/pages/data/data-set/dataset-page';
import { DataExplorer } from 'app/pages/data-explorer/data-explorer';
import { TanagraDev } from 'app/pages/tanagra-dev/tanagra-dev';
import { WorkspaceAbout } from 'app/pages/workspace/workspace-about';
import {
  WorkspaceEdit,
  WorkspaceEditMode,
} from 'app/pages/workspace/workspace-edit';
import { adminLockedGuard, tempAppsAnalysisGuard } from 'app/routing/guards';
import { MatchParams, withParamsKey } from 'app/utils/stores';
import { appFilesTabName } from 'app/utils/user-apps-utils';

const CohortPagePage = fp.flow(withRouteData, withRoutingSpinner)(CohortPage);
const CohortActionsPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(CohortActions);
const CohortReviewPagePage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(CohortReviewPage);
const ConceptHomepagePage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(ConceptHomepage);
const ConceptSearchPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(ConceptSearch);
const ConceptSetActionsPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(ConceptSetActions);
const DataExplorerPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(DataExplorer);
const DataComponentPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(DataComponent);
const DataSetComponentPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(DatasetPage);
const DetailPagePage = fp.flow(withRouteData, withRoutingSpinner)(DetailPage);
const InteractiveNotebookPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(InteractiveNotebook);
const NotebookListPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(NotebookList);
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
const QueryReportPage = fp.flow(withRouteData, withRoutingSpinner)(QueryReport);
const WorkspaceAboutPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(WorkspaceAbout);
const WorkspaceEditPage = fp.flow(
  withRouteData,
  withRoutingSpinner
)(WorkspaceEdit);
const AppsListPage = fp.flow(withRouteData, withRoutingSpinner)(AppFilesList);
const TanagraDevPage = fp.flow(withRouteData, withRoutingSpinner)(TanagraDev);

// name of the tab for accessing apps and app files, also used to construct URLs
export const appFilesTabName = environment.showNewAnalysisTab
  ? 'app-files'
  : 'notebooks';

export const WorkspaceRoutes = () => {
  const { path } = useRouteMatch();
  const { ns, wsid } = useParams<MatchParams>();

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
        guards={[adminLockedGuard(ns, wsid)]}
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
      {environment.showNewAnalysisTab ? (
        <AppRoute
          exact
          path={`${path}/${appFilesTabName}`}
          guards={[adminLockedGuard(ns, wsid), tempAppsAnalysisGuard(ns, wsid)]}
        >
          <AppsListPage
            routeData={{
              title: 'View App Files',
              pageKey: appFilesTabName,
              workspaceNavBarTab: appFilesTabName,
              breadcrumb: BreadcrumbType.Workspace,
            }}
          />
        </AppRoute>
      ) : (
        <AppRoute
          exact
          path={`${path}/${appFilesTabName}`}
          guards={[adminLockedGuard(ns, wsid)]}
        >
          <NotebookListPage
            routeData={{
              title: 'View Notebooks',
              pageKey: appFilesTabName,
              workspaceNavBarTab: appFilesTabName,
              breadcrumb: BreadcrumbType.Workspace,
            }}
          />
        </AppRoute>
      )}
      <AppRoute
        exact
        path={`${path}/${appFilesTabName}/preview/:nbName`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <InteractiveNotebookPage
          routeData={{
            pathElementForTitle: 'nbName',
            breadcrumb: BreadcrumbType.AppFile,
            pageKey: LEONARDO_APP_PAGE_KEY,
            workspaceNavBarTab: appFilesTabName,
            minimizeChrome: true,
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/${appFilesTabName}/:nbName`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <LeonardoAppRedirectPage
          key='notebook'
          routeData={{
            pathElementForTitle: 'nbName',
            breadcrumb: BreadcrumbType.AppFile,
            // The iframe we use to display the Jupyter notebook does something strange
            // to the height calculation of the container, which is normally set to auto.
            // Setting this flag sets the container to 100% so that no content is clipped.
            contentFullHeightOverride: true,
            pageKey: LEONARDO_APP_PAGE_KEY,
            workspaceNavBarTab: appFilesTabName,
            minimizeChrome: true,
          }}
          leoAppType={LeoApplicationType.Notebook}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/terminals`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <LeonardoAppRedirectPage
          key='terminal'
          routeData={{
            breadcrumb: BreadcrumbType.Workspace,
            pageKey: LEONARDO_APP_PAGE_KEY,
            contentFullHeightOverride: true,
            workspaceNavBarTab: appFilesTabName,
            minimizeChrome: true,
          }}
          leoAppType={LeoApplicationType.Terminal}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/spark/:sparkConsolePath`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <LeonardoSparkConsoleRedirectPage
          routeData={{
            breadcrumb: BreadcrumbType.Workspace,
            pageKey: LEONARDO_APP_PAGE_KEY,
            contentFullHeightOverride: true,
            workspaceNavBarTab: appFilesTabName,
            minimizeChrome: true,
          }}
          leoAppType={LeoApplicationType.SparkConsole}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data`}
        guards={[adminLockedGuard(ns, wsid)]}
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
        path={`${path}/data/data-sets`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <DataSetComponentPage
          routeData={{
            title: 'Dataset Page',
            breadcrumb: BreadcrumbType.Dataset,
            workspaceNavBarTab: 'data',
            pageKey: 'datasetBuilder',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/data-sets/:dataSetId`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <DataSetComponentPage
          routeData={{
            title: 'Edit Dataset',
            breadcrumb: BreadcrumbType.Dataset,
            workspaceNavBarTab: 'data',
            pageKey: 'datasetBuilder',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/build`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <CohortPagePage
          routeData={{
            title: 'Build Cohort Criteria',
            breadcrumb: BreadcrumbType.CohortAdd,
            workspaceNavBarTab: 'data',
            pageKey: 'cohortBuilder',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/:cid/actions`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <CohortActionsPage
          routeData={{
            title: 'Cohort Actions',
            breadcrumb: BreadcrumbType.Cohort,
            workspaceNavBarTab: 'data',
            pageKey: 'cohortBuilder',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/:cid/reviews/cohort-description`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <QueryReportPage
          routeData={{
            title: 'Review Cohort Description',
            breadcrumb: BreadcrumbType.Cohort,
            workspaceNavBarTab: 'data',
            pageKey: 'cohortDescription',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/:cid/reviews/:crid/cohort-description`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <QueryReportPage
          routeData={{
            title: 'Review Cohort Description',
            breadcrumb: BreadcrumbType.Cohort,
            workspaceNavBarTab: 'data',
            pageKey: 'cohortDescription',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/:cid/reviews`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <CohortReviewPagePage
          routeData={{
            title: 'Review Cohort Participants',
            breadcrumb: BreadcrumbType.Cohort,
            workspaceNavBarTab: 'data',
            pageKey: 'reviewParticipants',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/:cid/reviews/:crid`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <CohortReviewPagePage
          routeData={{
            title: 'Review Cohort Participants',
            breadcrumb: BreadcrumbType.CohortReview,
            workspaceNavBarTab: 'data',
            pageKey: 'reviewParticipants',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/cohorts/:cid/reviews/:crid/participants/:pid`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <DetailPagePage
          routeData={{
            title: 'Participant Detail',
            breadcrumb: BreadcrumbType.Participant,
            workspaceNavBarTab: 'data',
            pageKey: 'reviewParticipantDetail',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/concepts`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <ConceptHomepagePage
          routeData={{
            title: 'Search Concepts',
            breadcrumb: BreadcrumbType.SearchConcepts,
            workspaceNavBarTab: 'data',
            pageKey: 'searchConceptSets',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/concepts/sets/:csid`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <ConceptSearchPage
          routeData={{
            title: 'Concept Set',
            breadcrumb: BreadcrumbType.ConceptSet,
            workspaceNavBarTab: 'data',
            pageKey: 'conceptSets',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/concepts/:domain`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <ConceptSearchPage
          routeData={{
            title: 'Search Concepts',
            breadcrumb: BreadcrumbType.SearchConcepts,
            workspaceNavBarTab: 'data',
            pageKey: 'conceptSets',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data/concepts/sets/:csid/actions`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <ConceptSetActionsPage
          routeData={{
            title: 'Concept Set Actions',
            breadcrumb: BreadcrumbType.ConceptSet,
            workspaceNavBarTab: 'data',
            pageKey: 'conceptSetActions',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/data-explorer`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <DataExplorerPage
          routeData={{
            title: 'Visual Data Explorer',
            breadcrumb: BreadcrumbType.Workspace,
            pageKey: 'data',
            workspaceNavBarTab: 'data-explorer',
          }}
        />
      </AppRoute>
      <AppRoute
        exact
        path={`${path}/tanagra`}
        guards={[adminLockedGuard(ns, wsid)]}
      >
        <TanagraDevPage
          routeData={{
            title: 'Tanagra Dev Env',
            breadcrumb: BreadcrumbType.Workspace,
            pageKey: 'data',
            workspaceNavBarTab: 'tanagra',
          }}
        />
      </AppRoute>
      <AppRoute exact={false} path={`${path}`}>
        <Redirect to={'/not-found'} />
      </AppRoute>
    </Switch>
  );
};
