import {CohortPage} from 'app/cohort-search/cohort-page/cohort-page.component';
import {AppRoute, withRouteData} from 'app/components/app-router';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';
import {Switch, useRouteMatch} from 'react-router-dom';
import {NOTEBOOK_PAGE_KEY} from './components/help-sidebar';
import {InteractiveNotebook} from './pages/analysis/interactive-notebook';
import {NotebookList} from './pages/analysis/notebook-list';
import {NotebookRedirect} from './pages/analysis/notebook-redirect';
import {CohortReview} from './pages/data/cohort-review/cohort-review';
import {DetailPage} from './pages/data/cohort-review/detail-page';
import {QueryReport} from './pages/data/cohort-review/query-report.component';
import {ParticipantsTable} from './pages/data/cohort-review/table-page';
import {CohortActions} from './pages/data/cohort/cohort-actions';
import {ConceptHomepage} from './pages/data/concept/concept-homepage';
import {ConceptSearch} from './pages/data/concept/concept-search';
import {ConceptSetActions} from './pages/data/concept/concept-set-actions';
import {DataComponent} from './pages/data/data-component';
import {DatasetPage} from './pages/data/data-set/dataset-page';
import {WorkspaceAbout} from './pages/workspace/workspace-about';
import {WorkspaceEdit, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {BreadcrumbType} from './utils/navigation';
import {NotFound} from "app/pages/not-found";

const CohortPagePage = fp.flow(withRouteData, withRoutingSpinner)(CohortPage);
const CohortActionsPage = fp.flow(withRouteData, withRoutingSpinner)(CohortActions);
const CohortReviewPage = fp.flow(withRouteData, withRoutingSpinner)(CohortReview);
const ConceptHomepagePage = fp.flow(withRouteData, withRoutingSpinner)(ConceptHomepage);
const ConceptSearchPage = fp.flow(withRouteData, withRoutingSpinner)(ConceptSearch);
const ConceptSetActionsPage = fp.flow(withRouteData, withRoutingSpinner)(ConceptSetActions);
const DataComponentPage = fp.flow(withRouteData, withRoutingSpinner)(DataComponent);
const DataSetComponentPage = fp.flow(withRouteData, withRoutingSpinner)(DatasetPage);
const DetailPagePage = fp.flow(withRouteData, withRoutingSpinner)(DetailPage);
const InteractiveNotebookPage = fp.flow(withRouteData, withRoutingSpinner)(InteractiveNotebook);
const NotebookListPage = fp.flow(withRouteData, withRoutingSpinner)(NotebookList);
const NotebookRedirectPage = fp.flow(withRouteData, withRoutingSpinner)(NotebookRedirect);
const NotFoundPage = fp.flow(withRouteData, withRoutingSpinner)(NotFound);
const ParticipantsTablePage = fp.flow(withRouteData, withRoutingSpinner)(ParticipantsTable);
const QueryReportPage = fp.flow(withRouteData, withRoutingSpinner)(QueryReport);
const WorkspaceAboutPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceAbout);
const WorkspaceEditPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceEdit);

// TODO angular2react: Adding memo here feels a little off but it was necessary to prevent workspace-wrapper from
// rendering over and over again on page load, rendering (hah) the app unusable.
// We should be able to refactor this once we are driving the entire app through React router.
export const WorkspaceRoutes = React.memo(() => {
  const { path } = useRouteMatch();

  console.log('Rendering WorkspaceRoutes');
  useEffect(() => {
    console.log('Mounting WorkspaceRoutes (ish)');

    return () => console.log('Unmounting WorkspaceRoutes');
  }, []);
  return <Switch>
    <AppRoute exact path={`${path}/about`}>
      <WorkspaceAboutPage
          routeData={{
            title: 'View Workspace Details',
            breadcrumb: BreadcrumbType.Workspace,
            workspaceNavBarTab: 'about',
            pageKey: 'about'
          }}
      />
    </AppRoute>
    <AppRoute exact path={`${path}/duplicate`}>
      <WorkspaceEditPage
          routeData={{
            title: 'Duplicate Workspace',
            breadcrumb: BreadcrumbType.WorkspaceDuplicate,
            pageKey: 'duplicate'
          }}
          workspaceEditMode={WorkspaceEditMode.Duplicate}
      />
    </AppRoute>
    <AppRoute exact path={`${path}/edit`}>
      <WorkspaceEditPage
          routeData={{
            title: 'Edit Workspace',
            breadcrumb: BreadcrumbType.WorkspaceEdit,
            pageKey: 'edit'
          }}
          workspaceEditMode={WorkspaceEditMode.Edit}
      />
    </AppRoute>
    <AppRoute exact path={`${path}/notebooks`}>
      <NotebookListPage routeData={{
        title: 'View Notebooks',
        pageKey: 'notebooks',
        workspaceNavBarTab: 'notebooks',
        breadcrumb: BreadcrumbType.Workspace
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/notebooks/preview/:nbName`}>
      <InteractiveNotebookPage routeData={{
        pathElementForTitle: 'nbName',
        breadcrumb: BreadcrumbType.Notebook,
        pageKey: NOTEBOOK_PAGE_KEY,
        workspaceNavBarTab: 'notebooks',
        minimizeChrome: true
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/notebooks/:nbName`}>
      <NotebookRedirectPage routeData={{
        pathElementForTitle: 'nbName', // use the (urldecoded) captured value nbName
        breadcrumb: BreadcrumbType.Notebook,
        // The iframe we use to display the Jupyter notebook does something strange
        // to the height calculation of the container, which is normally set to auto.
        // Setting this flag sets the container to 100% so that no content is clipped.
        contentFullHeightOverride: true,
        pageKey: NOTEBOOK_PAGE_KEY,
        workspaceNavBarTab: 'notebooks',
        minimizeChrome: true
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data`}>
      <DataComponentPage routeData={{
        title: 'Data Page',
        breadcrumb: BreadcrumbType.Workspace,
        workspaceNavBarTab: 'data',
        pageKey: 'data'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/data-sets`}>
      <DataSetComponentPage routeData={{
        title: 'Dataset Page',
        breadcrumb: BreadcrumbType.Dataset,
        workspaceNavBarTab: 'data',
        pageKey: 'datasetBuilder'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/data-sets/:dataSetId`}>
      <DataSetComponentPage routeData={{
        title: 'Edit Dataset',
        breadcrumb: BreadcrumbType.Dataset,
        workspaceNavBarTab: 'data',
        pageKey: 'datasetBuilder'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/cohorts/build`}>
      <CohortPagePage routeData={{
        title: 'Build Cohort Criteria',
        breadcrumb: BreadcrumbType.CohortAdd,
        workspaceNavBarTab: 'data',
        pageKey: 'cohortBuilder'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/cohorts/:cid/actions`}>
      <CohortActionsPage routeData={{
        title: 'Cohort Actions',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'cohortBuilder'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/cohorts/:cid/review/participants`}>
      <ParticipantsTablePage routeData={{
        title: 'Review Cohort Participants',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'reviewParticipants'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/cohorts/:cid/review/participants/:pid`}>
      <DetailPagePage routeData={{
        title: 'Participant Detail',
        breadcrumb: BreadcrumbType.Participant,
        workspaceNavBarTab: 'data',
        pageKey: 'reviewParticipantDetail'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/cohorts/:cid/review/cohort-description`}>
      <QueryReportPage routeData={{
        title: 'Review Cohort Description',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'cohortDescription'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/cohorts/:cid/review`}>
      <CohortReviewPage routeData={{
        title: 'Review Cohort Participants',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'reviewParticipants'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/concepts`}>
      <ConceptHomepagePage routeData={{
        title: 'Search Concepts',
        breadcrumb: BreadcrumbType.SearchConcepts,
        workspaceNavBarTab: 'data',
        pageKey: 'searchConceptSets'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/concepts/sets/:csid`}>
      <ConceptSearchPage routeData={{
        title: 'Concept Set',
        breadcrumb: BreadcrumbType.ConceptSet,
        workspaceNavBarTab: 'data',
        pageKey: 'conceptSets'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/concepts/:domain`}>
      <ConceptSearchPage routeData={{
        title: 'Search Concepts',
        breadcrumb: BreadcrumbType.SearchConcepts,
        workspaceNavBarTab: 'data',
        pageKey: 'conceptSets'
      }}/>
    </AppRoute>
    <AppRoute exact path={`${path}/data/concepts/sets/:csid/actions`}>
      <ConceptSetActionsPage routeData={{
        title: 'Concept Set Actions',
        breadcrumb: BreadcrumbType.ConceptSet,
        workspaceNavBarTab: 'data',
        pageKey: 'conceptSetActions'
      }}/>
    </AppRoute>
    <AppRoute exact path="*">
      <NotFoundPage routeData={{title: 'Workspace Wrapper Not Found'}}/>
    </AppRoute>
  </Switch>;
});
