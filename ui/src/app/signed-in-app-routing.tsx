import * as React from "react";
import {
  AppRoute, Guard,
  Navigate,
  ProtectedRoutes,
  withFullHeight,
  withRouteData
} from "./components/app-router";
import {profileStore, serverConfigStore} from "./utils/stores";
import {BreadcrumbType} from "./utils/navigation";
import {WorkspaceEdit, WorkspaceEditMode} from "./pages/workspace/workspace-edit";
import * as fp from "lodash/fp";
import {withRoutingSpinner} from "./components/with-routing-spinner";
import {AccessRenewal} from "./pages/access/access-renewal";
import {AdminBanner} from "./pages/admin/admin-banner";
import {AdminNotebookView} from "./pages/admin/admin-notebook-view";
import {AdminReviewWorkspace} from "./pages/admin/admin-review-workspace";
import {DataUserCodeOfConduct} from "./pages/profile/data-user-code-of-conduct";
import {Homepage} from "./pages/homepage/homepage";
import {AdminInstitution} from "./pages/admin/admin-institution";
import {AdminInstitutionEdit} from "./pages/admin/admin-institution-edit";
import {ProfileComponent} from "./pages/profile/profile-component";
import {AdminUser} from "./pages/admin/admin-user";
import {AdminUsers} from "./pages/admin/admin-users";
import {UserAudit} from "./pages/admin/user-audit";
import {WorkspaceWrapper} from "./pages/workspace/workspace-wrapper";
import {AdminWorkspace} from "./pages/admin/admin-workspace";
import {WorkspaceAudit} from "./pages/admin/admin-workspace-audit";
import {WorkspaceLibrary} from "./pages/workspace/workspace-library";
import {WorkspaceList} from "./pages/workspace/workspace-list";
import {AdminWorkspaceSearch} from "./pages/admin/admin-workspace-search";
import {hasRegisteredAccess} from "./utils/access-tiers";

const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
  redirectPath: '/'
};

const expiredGuard: Guard = {
  allowed: (): boolean => !profileStore.get().profile.renewableAccessModules.anyModuleHasExpired,
  redirectPath: '/access-renewal'
};

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
    <AppRoute path='/access-renewal' component={() => <AccessRenewalPage routeData={{title: 'Access Renewal'}}/>}/>
    <ProtectedRoutes guards={[expiredGuard]}>
      <AppRoute
          path='/'
          component={() => <HomepagePage routeData={{title: 'Homepage'}}/>}
      />
    </ProtectedRoutes>
    <AppRoute
        path='/admin/banner'
        component={() => <AdminBannerPage routeData={{title: 'Create Banner'}}/>}
    />
    <AppRoute
        path='/admin/institution'
        component={() => <InstitutionAdminPage routeData={{title: 'Institution Admin'}}/>}
    />
    <AppRoute
        path='/admin/institution/add'
        component={() => <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>}
    />
    <AppRoute
        path='/admin/institution/edit/:institutionId'
        component={() => <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>}
    />
    <AppRoute
        path='/admin/user' // included for backwards compatibility
        component={() => <UsersAdminPage routeData={{title: 'User Admin Table'}}/>}
    />
    <AppRoute
        path='/admin/review-workspace'
        component={() => <AdminReviewWorkspacePage routeData={{title: 'Review Workspaces'}}/>}
    />
    <AppRoute
        path='/admin/users'
        component={() => <UsersAdminPage routeData={{title: 'User Admin Table'}}/>}
    />
    <AppRoute
        path='/admin/users/:usernameWithoutGsuiteDomain'
        component={() => <UserAdminPage routeData={{title: 'User Admin'}}/>}
    />
    <AppRoute
        path='/admin/user-audit'
        component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}
    />
    <AppRoute
        path='/admin/user-audit/:username'
        component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}
    />
    <AppRoute
        path='/admin/workspaces'
        component={() => <WorkspaceSearchAdminPage routeData={{title: 'Workspace Admin'}}/>}
    />
    <AppRoute
        path='/admin/workspaces/:workspaceNamespace'
        component={() => <WorkspaceAdminPage routeData={{title: 'Workspace Admin'}}/>}
    />
    <AppRoute
        path='/admin/workspace-audit'
        component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}
    />
    <AppRoute
        path='/admin/workspace-audit/:workspaceNamespace'
        component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}
    />
    <AppRoute
        path='/admin/workspaces/:workspaceNamespace/:nbName'
        component={() => <AdminNotebookViewPage routeData={{
          pathElementForTitle: 'nbName',
          minimizeChrome: true
        }}/>}
    />
    <AppRoute
        path='/data-code-of-conduct'
        component={() => <DataUserCodeOfConductPage routeData={{
          title: 'Data User Code of Conduct',
          minimizeChrome: true
        }} />}
    />
    <AppRoute path='/profile' component={() => <ProfilePage routeData={{title: 'Profile'}}/>}/>
    <AppRoute path='/nih-callback' component={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />
    <AppRoute path='/ras-callback' component={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />

    <ProtectedRoutes guards={[expiredGuard, registrationGuard]}>
      <AppRoute
          path='/library'
          component={() => <WorkspaceLibraryPage routeData={{title: 'Workspace Library', minimizeChrome: false}}/>}
      />
      <AppRoute
          path='/workspaces'
          component={() => <WorkspaceListPage
              routeData={{
                title: 'View Workspaces',
                breadcrumb: BreadcrumbType.Workspaces
              }}
          />}
      />
      <AppRoute
          path='/workspaces/build'
          component={() => <WorkspaceEditPage
              routeData={{title: 'Create Workspace'}}
              workspaceEditMode={WorkspaceEditMode.Create}
          />}
      />
      <AppRoute
          path='/workspaces/:ns/:wsid'
          exact={false}
          component={() => <WorkspaceWrapperPage intermediaryRoute={true} routeData={{}}/>}
      />
    </ProtectedRoutes>
  </React.Fragment>
});
