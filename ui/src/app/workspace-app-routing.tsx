import {CohortPage} from 'app/cohort-search/cohort-page/cohort-page.component';
import {AppRoute, AppRouter, Guard, Navigate, ProtectedRoutes, withFullHeight, withRouteData} from 'app/components/app-router';
import {AccessRenewalPage} from 'app/pages/access/access-renewal-page';
import {WorkspaceAudit} from 'app/pages/admin/admin-workspace-audit';
import {UserAudit} from 'app/pages/admin/user-audit';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {UserDisabled} from 'app/pages/user-disabled';
import {SignInService} from 'app/services/sign-in.service';
import {ReactWrapperBase} from 'app/utils';
import {authStore, profileStore, useStore} from 'app/utils/stores';
import {serverConfigStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect} from 'react-router';
import {NOTEBOOK_PAGE_KEY} from './components/help-sidebar';
import {NotificationModal} from './components/modals';
import {AdminBanner} from './pages/admin/admin-banner';
import {AdminInstitution} from './pages/admin/admin-institution';
import {AdminInstitutionEdit} from './pages/admin/admin-institution-edit';
import {AdminNotebookView} from './pages/admin/admin-notebook-view';
import {AdminReviewWorkspace} from './pages/admin/admin-review-workspace';
import {AdminUser} from './pages/admin/admin-user';
import {AdminUsers} from './pages/admin/admin-users';
import {AdminWorkspace} from './pages/admin/admin-workspace';
import {AdminWorkspaceSearch} from './pages/admin/admin-workspace-search';
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
import {Homepage} from './pages/homepage/homepage';
import {SignIn} from './pages/login/sign-in';
import {ProfilePage} from './pages/profile/profile-page';
import {WorkspaceAbout} from './pages/workspace/workspace-about';
import {WorkspaceEdit, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceLibrary} from './pages/workspace/workspace-library';
import {WorkspaceList} from './pages/workspace/workspace-list';
import {hasRegisteredAccess} from './utils/access-tiers';
import {AnalyticsTracker} from './utils/analytics';
import {BreadcrumbType} from './utils/navigation';


const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
  redirectPath: '/'
};

const expiredGuard: Guard = {
  allowed: (): boolean => !profileStore.get().profile.renewableAccessModules.anyModuleHasExpired,
  redirectPath: '/access-renewal'
};

const AdminBannerPage = withRouteData(AdminBanner);
const AdminNotebookViewPage = withRouteData(AdminNotebookView);
const AdminReviewWorkspacePage = withRouteData(AdminReviewWorkspace);
const CohortPagePage = withRouteData(CohortPage);
const CohortActionsPage = withRouteData(CohortActions);
const CohortReviewPage = withRouteData(CohortReview);
const ConceptHomepagePage = withRouteData(ConceptHomepage);
const ConceptSearchPage = withRouteData(ConceptSearch);
const ConceptSetActionsPage = withRouteData(ConceptSetActions);
const CookiePolicyPage = withRouteData(CookiePolicy);
const DataComponentPage = withRouteData(DataComponent);
const DataSetComponentPage = withRouteData(DatasetPage);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);
const DetailPagePage = withRouteData(DetailPage);
const HomepagePage = withRouteData(Homepage); // this name is bad i am sorry
const InstitutionAdminPage = withRouteData(AdminInstitution);
const InstitutionEditAdminPage = withRouteData(AdminInstitutionEdit);
const InteractiveNotebookPage = withRouteData(InteractiveNotebook);
const NotebookListPage = withRouteData(NotebookList);
const NotebookRedirectPage = withRouteData(NotebookRedirect);
const ParticipantsTablePage = withRouteData(ParticipantsTable);
const QueryReportPage = withRouteData(QueryReport);
const SessionExpiredPage = withRouteData(SessionExpired);
const SignInAgainPage = withRouteData(SignInAgain);
const SignInPage = withRouteData(SignIn);
const UserAdminPage = withRouteData(AdminUser);
const UsersAdminPage = withRouteData(AdminUsers);
const UserAuditPage = withRouteData(UserAudit);
const UserDisabledPage = withRouteData(UserDisabled);
const WorkspaceAboutPage = withRouteData(WorkspaceAbout);
const WorkspaceAdminPage = withRouteData(AdminWorkspace);
const WorkspaceAuditPage = withRouteData(WorkspaceAudit);
const WorkspaceEditPage = withRouteData(WorkspaceEdit);
const WorkspaceLibraryPage = withRouteData(WorkspaceLibrary);
const WorkspaceListPage = withRouteData(WorkspaceList);
const WorkspaceSearchAdminPage = withRouteData(AdminWorkspaceSearch);

export const WorkspaceRoutes = () => {
  return <React.Fragment>
    <AppRoute
      path='/workspaces/:ns/:wsid/about'
      component={() => <WorkspaceAboutPage
        routeData={{
          title: 'View Workspace Details',
          breadcrumb: BreadcrumbType.Workspace,
          workspaceNavBarTab: 'about',
          pageKey: 'about'
        }}
      />}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/duplicate'
      component={() => <WorkspaceEditPage
        routeData={{
          title: 'Duplicate Workspace',
          breadcrumb: BreadcrumbType.WorkspaceDuplicate,
          pageKey: 'duplicate'
        }}
        workspaceEditMode={WorkspaceEditMode.Duplicate}
      />}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/edit'
      component={() => <WorkspaceEditPage
        routeData={{
          title: 'Edit Workspace',
          breadcrumb: BreadcrumbType.WorkspaceEdit,
          pageKey: 'edit'
        }}
        workspaceEditMode={WorkspaceEditMode.Edit}
      />}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/notebooks'
      component={() => <NotebookListPage routeData={{
        title: 'View Notebooks',
        pageKey: 'notebooks',
        workspaceNavBarTab: 'notebooks',
        breadcrumb: BreadcrumbType.Workspace
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/notebooks/preview/:nbName'
      component={() => <InteractiveNotebookPage routeData={{
        pathElementForTitle: 'nbName',
        breadcrumb: BreadcrumbType.Notebook,
        pageKey: NOTEBOOK_PAGE_KEY,
        workspaceNavBarTab: 'notebooks',
        minimizeChrome: true
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/notebooks/:nbName'
      component={() => <NotebookRedirectPage routeData={{
        pathElementForTitle: 'nbName', // use the (urldecoded) captured value nbName
        breadcrumb: BreadcrumbType.Notebook,
        // The iframe we use to display the Jupyter notebook does something strange
        // to the height calculation of the container, which is normally set to auto.
        // Setting this flag sets the container to 100% so that no content is clipped.
        contentFullHeightOverride: true,
        pageKey: NOTEBOOK_PAGE_KEY,
        workspaceNavBarTab: 'notebooks',
        minimizeChrome: true
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data'
      component={() => <DataComponentPage routeData={{
        title: 'Data Page',
        breadcrumb: BreadcrumbType.Workspace,
        workspaceNavBarTab: 'data',
        pageKey: 'data'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/data-sets'
      component={() => <DataSetComponentPage routeData={{
        title: 'Dataset Page',
        breadcrumb: BreadcrumbType.Dataset,
        workspaceNavBarTab: 'data',
        pageKey: 'datasetBuilder'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/data-sets/:dataSetId'
      component={() => <DataSetComponentPage routeData={{
        title: 'Edit Dataset',
        breadcrumb: BreadcrumbType.Dataset,
        workspaceNavBarTab: 'data',
        pageKey: 'datasetBuilder'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/cohorts/build'
      component={() => <CohortPagePage routeData={{
        title: 'Build Cohort Criteria',
        breadcrumb: BreadcrumbType.CohortAdd,
        workspaceNavBarTab: 'data',
        pageKey: 'cohortBuilder'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/cohorts/:cid/actions'
      component={() => <CohortActionsPage routeData={{
        title: 'Cohort Actions',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'cohortBuilder'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/cohorts/:cid/review/participants'
      component={() => <ParticipantsTablePage routeData={{
        title: 'Review Cohort Participants',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'reviewParticipants'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/cohorts/:cid/review/participants/:pid'
      component={() => <DetailPagePage routeData={{
        title: 'Participant Detail',
        breadcrumb: BreadcrumbType.Participant,
        workspaceNavBarTab: 'data',
        pageKey: 'reviewParticipantDetail'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/cohorts/:cid/review/cohort-description'
      component={() => <QueryReportPage routeData={{
        title: 'Review Cohort Description',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'cohortDescription'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/cohorts/:cid/review'
      component={() => <CohortReviewPage routeData={{
        title: 'Review Cohort Participants',
        breadcrumb: BreadcrumbType.Cohort,
        workspaceNavBarTab: 'data',
        pageKey: 'reviewParticipants'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/concepts'
      component={() => <ConceptHomepagePage routeData={{
        title: 'Search Concepts',
        breadcrumb: BreadcrumbType.SearchConcepts,
        workspaceNavBarTab: 'data',
        pageKey: 'searchConceptSets'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/concepts/sets/:csid'
      component={() => <ConceptSearchPage routeData={{
        title: 'Concept Set',
        breadcrumb: BreadcrumbType.ConceptSet,
        workspaceNavBarTab: 'data',
        pageKey: 'conceptSets'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/concepts/:domain'
      component={() => <ConceptSearchPage routeData={{
        title: 'Search Concepts',
        breadcrumb: BreadcrumbType.SearchConcepts,
        workspaceNavBarTab: 'data',
        pageKey: 'conceptSets'
      }}/>}
    />
    <AppRoute
      path='/workspaces/:ns/:wsid/data/concepts/sets/:csid/actions'
      component={() => <ConceptSetActionsPage routeData={{
        title: 'Concept Set Actions',
        breadcrumb: BreadcrumbType.ConceptSet,
        workspaceNavBarTab: 'data',
        pageKey: 'conceptSetActions'
      }}/>}
    />
  </React.Fragment>;
}
