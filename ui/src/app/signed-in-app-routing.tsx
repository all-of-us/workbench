import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';
import {
  ProtectedRoutes,
  withFullHeight,
  withRouteData
} from './components/app-router';
import {withRoutingSpinner} from './components/with-routing-spinner';
import {AccessRenewal} from './pages/access/access-renewal';
import {AdminBanner} from './pages/admin/admin-banner';
import {AdminInstitution} from './pages/admin/admin-institution';
import {AdminInstitutionEdit} from './pages/admin/admin-institution-edit';
import {AdminNotebookView} from './pages/admin/admin-notebook-view';
import {AdminReviewWorkspace} from './pages/admin/admin-review-workspace';
import {AdminUser} from './pages/admin/admin-user';
import {AdminUsers} from './pages/admin/admin-users';
import {AdminWorkspace} from './pages/admin/admin-workspace';
import {WorkspaceAudit} from './pages/admin/admin-workspace-audit';
import {AdminWorkspaceSearch} from './pages/admin/admin-workspace-search';
import {UserAudit} from './pages/admin/user-audit';
import {Homepage} from './pages/homepage/homepage';
import {DataUserCodeOfConduct} from './pages/profile/data-user-code-of-conduct';
import {ProfileComponent} from './pages/profile/profile-component';
import {WorkspaceEdit, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceLibrary} from './pages/workspace/workspace-library';
import {WorkspaceList} from './pages/workspace/workspace-list';
import {WorkspaceWrapper} from './pages/workspace/workspace-wrapper';
import {hasRegisteredAccess} from './utils/access-tiers';
import {BreadcrumbType} from './utils/navigation';
import {profileStore} from './utils/stores';
import {NotFound} from "app/pages/not-found";
import {Switch} from "react-router-dom";
import {RouteAccess} from "./routing/guards";
import {GuardedRoute} from "react-router-guards";

const AccessRenewalPage = fp.flow(withRouteData, withRoutingSpinner)(AccessRenewal);
const AdminBannerPage = fp.flow(withRouteData, withRoutingSpinner)(AdminBanner);
const AdminNotebookViewPage = fp.flow(withRouteData, withRoutingSpinner)(AdminNotebookView);
const AdminReviewWorkspacePage = fp.flow(withRouteData, withRoutingSpinner)(AdminReviewWorkspace);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight, withRoutingSpinner)(DataUserCodeOfConduct);
const HomepagePage = fp.flow(withRouteData, withRoutingSpinner)(Homepage);
const InstitutionAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminInstitution);
const InstitutionEditAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminInstitutionEdit);
const NotFoundPage = fp.flow(withRouteData, withRoutingSpinner)(NotFound);
const ProfilePage = fp.flow(withRouteData, withRoutingSpinner)(ProfileComponent);
const UserAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminUser);
const UsersAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminUsers);
const UserAuditPage = fp.flow(withRouteData, withRoutingSpinner)(UserAudit);
const WorkspaceWrapperPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceWrapper);
const WorkspaceAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminWorkspace);
const WorkspaceAuditPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceAudit);
const WorkspaceEditPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceEdit);
const WorkspaceLibraryPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceLibrary);
const WorkspaceListPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceList);
const WorkspaceSearchAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminWorkspaceSearch);

/*
 * TODO angular2react: Adding memo here feels a little off but it was necessary to prevent signed-in
 *  component from rendering over and over again on page load, rendering (hah) the app unusable.
 *  We should be able to refactor this once we are driving the entire app through React router.
 */
export const SignedInRoutes = React.memo(() => {
  console.log('Rendering SignedInRoutes');
  useEffect(() => {
    console.log('Mounting SignedInRoutes (ish)');

    return () => console.log('Unmounting SignedInRoutes');
  }, []);
  return <Switch>
    <GuardedRoute exact path='/' meta={{
      [RouteAccess.UNEXPIRED_ONLY]: true
    }}>
      <HomepagePage routeData={{title: 'Homepage'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/access-renewal'>
      <AccessRenewalPage routeData={{title: 'Access Renewal'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/banner'>
      <AdminBannerPage routeData={{title: 'Create Banner'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/institution'>
      <InstitutionAdminPage routeData={{title: 'Institution Admin'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/institution/add'>
      <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/institution/edit/:institutionId'>
      <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/user'> // included for backwards compatibility
      <UsersAdminPage routeData={{title: 'User Admin Table'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/review-workspace'>
      <AdminReviewWorkspacePage routeData={{title: 'Review Workspaces'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/users'>
      <UsersAdminPage routeData={{title: 'User Admin Table'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/users/:usernameWithoutGsuiteDomain'>
      <UserAdminPage routeData={{title: 'User Admin'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/user-audit'>
      <UserAuditPage routeData={{title: 'User Audit'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/user-audit/:username'>
      <UserAuditPage routeData={{title: 'User Audit'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/workspaces'>
      <WorkspaceSearchAdminPage routeData={{title: 'Workspace Admin'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/workspaces/:workspaceNamespace'>
      <WorkspaceAdminPage routeData={{title: 'Workspace Admin'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/workspace-audit'>
      <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/workspace-audit/:workspaceNamespace'>
      <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/admin/workspaces/:workspaceNamespace/:nbName'>
      <AdminNotebookViewPage routeData={{pathElementForTitle: 'nbName', minimizeChrome: true}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/data-code-of-conduct'>
      <DataUserCodeOfConductPage routeData={{title: 'Data User Code of Conduct', minimizeChrome: true}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/profile'>
      <ProfilePage routeData={{title: 'Profile'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/nih-callback'>
      <HomepagePage routeData={{title: 'Homepage'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/ras-callback'>
      <HomepagePage routeData={{title: 'Homepage'}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/library' meta={{
      [RouteAccess.UNEXPIRED_ONLY]: true,
      [RouteAccess.REGISTERED_ONLY]: true
    }}>
      <WorkspaceLibraryPage routeData={{title: 'Workspace Library', minimizeChrome: false}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/workspaces' meta={{
      [RouteAccess.UNEXPIRED_ONLY]: true,
      [RouteAccess.REGISTERED_ONLY]: true
    }}>
      <WorkspaceListPage routeData={{title: 'View Workspaces', breadcrumb: BreadcrumbType.Workspaces}}/>
    </GuardedRoute>
    <GuardedRoute exact path='/workspaces/build' meta={{
      [RouteAccess.UNEXPIRED_ONLY]: true,
      [RouteAccess.REGISTERED_ONLY]: true
    }}>
      <WorkspaceEditPage routeData={{title: 'Create Workspace'}} workspaceEditMode={WorkspaceEditMode.Create}/>
    </GuardedRoute>
    <GuardedRoute path='/workspaces/:ns/:wsid' exact={false} meta={{
      [RouteAccess.UNEXPIRED_ONLY]: true,
      [RouteAccess.REGISTERED_ONLY]: true
    }}>
      <WorkspaceWrapperPage intermediaryRoute={false} routeData={{}}/>
    </GuardedRoute>
    <GuardedRoute>
      <NotFoundPage routeData={{title: 'Not Found'}}/>
    </GuardedRoute>
  </Switch>;
});
