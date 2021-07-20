import * as fp from 'lodash/fp';
import * as React from 'react';
import {
  AppRoute,
  withFullHeight,
  withRouteData
} from './components/app-router';
import {withRoutingSpinner} from './components/with-routing-spinner';
import {expiredGuard, registrationGuard} from './guards/react-guards';
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
import {BreadcrumbType} from './utils/navigation';

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

/*
 * TODO angular2react: Adding memo here feels a little off but it was necessary to prevent signed-in
 *  component from rendering over and over again on page load, rendering (hah) the app unusable.
 *  We should be able to refactor this once we are driving the entire app through React router.
 */
export const SignedInRoutes = React.memo(() => {
  return <React.Fragment>
    <AppRoute
        path='/'
        guards={[expiredGuard]}
        render={() => <HomepagePage routeData={{title: 'Homepage'}}/>}
    />
    <AppRoute
        path='/access-renewal'
        render={() => <AccessRenewalPage routeData={{title: 'Access Renewal'}}/>}
    />
    <AppRoute
        path='/admin/banner'
        render={() => <AdminBannerPage routeData={{title: 'Create Banner'}}/>}
    />
    <AppRoute
        path='/admin/institution'
        render={() => <InstitutionAdminPage routeData={{title: 'Institution Admin'}}/>}
    />
    <AppRoute
        path='/admin/institution/add'
        render={() => <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>}
    />
    <AppRoute
        path='/admin/institution/edit/:institutionId'
        render={() => <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>}
    />
    <AppRoute
        path='/admin/user' // included for backwards compatibility
        render={() => <UsersAdminPage routeData={{title: 'User Admin Table'}}/>}
    />
    <AppRoute
        path='/admin/review-workspace'
        render={() => <AdminReviewWorkspacePage routeData={{title: 'Review Workspaces'}}/>}
    />
    <AppRoute
        path='/admin/users'
        render={() => <UsersAdminPage routeData={{title: 'User Admin Table'}}/>}
    />
    <AppRoute
        path='/admin/users/:usernameWithoutGsuiteDomain'
        render={() => <UserAdminPage routeData={{title: 'User Admin'}}/>}
    />
    <AppRoute
        path='/admin/user-audit'
        render={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}
    />
    <AppRoute
        path='/admin/user-audit/:username'
        render={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}
    />
    <AppRoute
        path='/admin/workspaces'
        render={() => <WorkspaceSearchAdminPage routeData={{title: 'Workspace Admin'}}/>}
    />
    <AppRoute
        path='/admin/workspaces/:workspaceNamespace'
        render={() => <WorkspaceAdminPage routeData={{title: 'Workspace Admin'}}/>}
    />
    <AppRoute
        path='/admin/workspace-audit'
        render={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}
    />
    <AppRoute
        path='/admin/workspace-audit/:workspaceNamespace'
        component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}
    />
    <AppRoute
        path='/admin/workspaces/:workspaceNamespace/:nbName'
        render={() => <AdminNotebookViewPage routeData={{
          pathElementForTitle: 'nbName',
          minimizeChrome: true
        }}/>}
    />
    <AppRoute
        path='/data-code-of-conduct'
        render={() => <DataUserCodeOfConductPage routeData={{
          title: 'Data User Code of Conduct',
          minimizeChrome: true
        }} />}
    />
    <AppRoute path='/profile' render={() => <ProfilePage routeData={{title: 'Profile'}}/>}/>
    <AppRoute path='/nih-callback' render={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />
    <AppRoute path='/ras-callback' render={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />
    <AppRoute
        path='/library'
        guards={[expiredGuard, registrationGuard]}
        render={() => <WorkspaceLibraryPage routeData={{title: 'Workspace Library', minimizeChrome: false}}/>}
    />
    <AppRoute
        path='/workspaces'
        guards={[expiredGuard, registrationGuard]}
        render={() => <WorkspaceListPage
            routeData={{
              title: 'View Workspaces',
              breadcrumb: BreadcrumbType.Workspaces
            }}
        />}
    />
    <AppRoute
        path='/workspaces/build'
        guards={[expiredGuard, registrationGuard]}
        component={() => <WorkspaceEditPage
            routeData={{title: 'Create Workspace'}}
            workspaceEditMode={WorkspaceEditMode.Create}
        />}
    />
    <AppRoute
        path='/workspaces/:ns/:wsid'
        guards={[expiredGuard, registrationGuard]}
        exact={false}
        render={() => <WorkspaceWrapperPage intermediaryRoute={true} routeData={{}}/>}
    />
  </React.Fragment>;
});
