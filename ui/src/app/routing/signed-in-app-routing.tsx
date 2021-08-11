import {
  AppRoute,
  withFullHeight,
  withRouteData
} from 'app/components/app-router';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import {AccessRenewal} from 'app/pages/access/access-renewal';
import {AdminBanner} from 'app/pages/admin/admin-banner';
import {AdminInstitution} from 'app/pages/admin/admin-institution';
import {AdminInstitutionEdit} from 'app/pages/admin/admin-institution-edit';
import {AdminNotebookView} from 'app/pages/admin/admin-notebook-view';
import {AdminReviewWorkspace} from 'app/pages/admin/admin-review-workspace';
import {AdminUser} from 'app/pages/admin/admin-user';
import {AdminUsers} from 'app/pages/admin/admin-users';
import {AdminWorkspace} from 'app/pages/admin/admin-workspace';
import {WorkspaceAudit} from 'app/pages/admin/admin-workspace-audit';
import {AdminWorkspaceSearch} from 'app/pages/admin/admin-workspace-search';
import {UserAudit} from 'app/pages/admin/user-audit';
import {Homepage} from 'app/pages/homepage/homepage';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import {ProfileComponent} from 'app/pages/profile/profile-component';
import {WorkspaceEdit, WorkspaceEditMode} from 'app/pages/workspace/workspace-edit';
import {WorkspaceLibrary} from 'app/pages/workspace/workspace-library';
import {WorkspaceList} from 'app/pages/workspace/workspace-list';
import {WorkspaceWrapper} from 'app/pages/workspace/workspace-wrapper';
import {BreadcrumbType} from 'app/utils/navigation';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect, Switch} from 'react-router-dom';
import {expiredGuard, registrationGuard} from './guards';

const AccessRenewalPage = fp.flow(withRouteData, withRoutingSpinner)(AccessRenewal);
const AdminBannerPage = fp.flow(withRouteData, withRoutingSpinner)(AdminBanner);
const AdminNotebookViewPage = fp.flow(withRouteData, withRoutingSpinner)(AdminNotebookView);
const AdminReviewWorkspacePage = fp.flow(withRouteData, withRoutingSpinner)(AdminReviewWorkspace);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight, withRoutingSpinner)(DataUserCodeOfConduct);
const HomepagePage = fp.flow(withRouteData, withRoutingSpinner)(Homepage);
const InstitutionAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminInstitution);
const InstitutionEditAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminInstitutionEdit);
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

export const SignedInRoutes = () => {
  return <Switch>
    <AppRoute exact path='/' guards={[expiredGuard]}>
      <HomepagePage routeData={{title: 'Homepage'}}/>
    </AppRoute>
    <AppRoute exact path='/access-renewal'>
      <AccessRenewalPage routeData={{title: 'Access Renewal'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/banner'>
      <AdminBannerPage routeData={{title: 'Create Banner'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/institution'>
      <InstitutionAdminPage routeData={{title: 'Institution Admin'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/institution/add'>
      <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/institution/edit/:institutionId'>
      <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/user'> // included for backwards compatibility
      <UsersAdminPage routeData={{title: 'User Admin Table'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/review-workspace'>
      <AdminReviewWorkspacePage routeData={{title: 'Review Workspaces'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/users'>
      <UsersAdminPage routeData={{title: 'User Admin Table'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/users/:usernameWithoutGsuiteDomain'>
      <UserAdminPage routeData={{title: 'User Admin'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/user-audit'>
      <UserAuditPage routeData={{title: 'User Audit'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/user-audit/:username'>
      <UserAuditPage routeData={{title: 'User Audit'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/workspaces'>
      <WorkspaceSearchAdminPage routeData={{title: 'Workspace Admin'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/workspaces/:workspaceNamespace'>
      <WorkspaceAdminPage routeData={{title: 'Workspace Admin'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/workspace-audit'>
      <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/workspace-audit/:workspaceNamespace'>
      <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>
    </AppRoute>
    <AppRoute exact path='/admin/workspaces/:workspaceNamespace/:nbName'>
      <AdminNotebookViewPage routeData={{pathElementForTitle: 'nbName', minimizeChrome: true}}/>
    </AppRoute>
    <AppRoute exact path='/data-code-of-conduct'>
      <DataUserCodeOfConductPage routeData={{title: 'Data User Code of Conduct', minimizeChrome: true}}/>
    </AppRoute>
    <AppRoute exact path='/profile'>
      <ProfilePage routeData={{title: 'Profile'}}/>
    </AppRoute>
    <AppRoute exact path='/nih-callback'>
      <HomepagePage routeData={{title: 'Homepage'}}/>
    </AppRoute>
    <AppRoute exact path='/ras-callback'>
      <HomepagePage routeData={{title: 'Homepage'}}/>
    </AppRoute>
    <AppRoute exact path='/library' guards={[expiredGuard, registrationGuard]}>
      <WorkspaceLibraryPage routeData={{title: 'Workspace Library', minimizeChrome: false}}/>
    </AppRoute>
    <AppRoute exact path='/workspaces' guards={[expiredGuard, registrationGuard]}>
      <WorkspaceListPage routeData={{title: 'View Workspaces', breadcrumb: BreadcrumbType.Workspaces}}/>
    </AppRoute>
    <AppRoute exact path='/workspaces/build' guards={[expiredGuard, registrationGuard]}>
      <WorkspaceEditPage routeData={{title: 'Create Workspace'}} workspaceEditMode={WorkspaceEditMode.Create}/>
    </AppRoute>
    <AppRoute path='/workspaces/:ns/:wsid' exact={false} guards={[expiredGuard, registrationGuard]}>
      <WorkspaceWrapperPage intermediaryRoute={true} routeData={{}}/>
    </AppRoute>
    <AppRoute exact path='*'>
      <Redirect to={'/not-found'}/>
    </AppRoute>
  </Switch>;
};
