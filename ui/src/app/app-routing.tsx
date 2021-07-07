import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, Navigate, ProtectedRoutes, withFullHeight, withRouteData} from 'app/components/app-router';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import {AccessRenewal} from 'app/pages/access/access-renewal';
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
import {Homepage} from './pages/homepage/homepage';
import {SignIn} from './pages/login/sign-in';
import {ProfileComponent} from './pages/profile/profile-component';
import {WorkspaceEdit, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceLibrary} from './pages/workspace/workspace-library';
import {WorkspaceList} from './pages/workspace/workspace-list';
import {WorkspaceWrapper} from './pages/workspace/workspace-wrapper';
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

const AccessRenewalPage = fp.flow(withRouteData, withRoutingSpinner)(AccessRenewal);
const ProfilePage = fp.flow(withRouteData, withRoutingSpinner)(ProfileComponent);
const AdminBannerPage = fp.flow(withRouteData, withRoutingSpinner)(AdminBanner);
const AdminNotebookViewPage = fp.flow(withRouteData, withRoutingSpinner)(AdminNotebookView);
const AdminReviewWorkspacePage = fp.flow(withRouteData, withRoutingSpinner)(AdminReviewWorkspace);
const CookiePolicyPage = fp.flow(withRouteData, withRoutingSpinner)(CookiePolicy);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight, withRoutingSpinner)(DataUserCodeOfConduct);
const HomepagePage = fp.flow(withRouteData, withRoutingSpinner)(Homepage);
const InstitutionAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminInstitution);
const InstitutionEditAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminInstitutionEdit);
const SessionExpiredPage = fp.flow(withRouteData, withRoutingSpinner)(SessionExpired);
const SignInAgainPage = fp.flow(withRouteData, withRoutingSpinner)(SignInAgain);
const SignInPage = fp.flow(withRouteData, withRoutingSpinner)(SignIn);
const UserAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminUser);
const UsersAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminUsers);
const UserAuditPage = fp.flow(withRouteData, withRoutingSpinner)(UserAudit);
const UserDisabledPage = fp.flow(withRouteData, withRoutingSpinner)(UserDisabled);
const WorkspaceWrapperPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceWrapper);
const WorkspaceAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminWorkspace);
const WorkspaceAuditPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceAudit);
const WorkspaceEditPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceEdit);
const WorkspaceLibraryPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceLibrary);
const WorkspaceListPage = fp.flow(withRouteData, withRoutingSpinner)(WorkspaceList);
const WorkspaceSearchAdminPage = fp.flow(withRouteData, withRoutingSpinner)(AdminWorkspaceSearch);

interface RoutingProps {
  onSignIn: () => void;
  signIn: () => void;
}

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = ({onSignIn, signIn}) => {
  const {authLoaded = false} = useStore(authStore);

  return authLoaded && <React.Fragment>
    {/* Once Angular is removed the app structure will change and we can put this in a more appropriate place */}
    <NotificationModal/>
    <AppRouter>
      <AppRoute
          path='/cookie-policy'
          component={() => <CookiePolicyPage routeData={{title: 'Cookie Policy'}}/>}
      />
      <AppRoute
          path='/login'
          component={() => <SignInPage routeData={{title: 'Sign In'}} onSignIn={onSignIn} signIn={signIn}/>}
      />
      <AppRoute
          path='/session-expired'
          component={() => <SessionExpiredPage routeData={{title: 'You have been signed out'}} signIn={signIn}/>}
      />
      <AppRoute
          path='/sign-in-again'
          component={() => <SignInAgainPage routeData={{title: 'You have been signed out'}} signIn={signIn}/>}
      />
      <AppRoute
          path='/user-disabled'
          component={() => <UserDisabledPage routeData={{title: 'Disabled'}}/>}
      />
      <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute path='/access-renewal' component={() => serverConfigStore.get().config.enableAccessRenewal
            ? <AccessRenewalPage routeData={{title: 'Access Renewal'}}/>
            : <Navigate to={'/profile'}/>
        }/>
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
      </ProtectedRoutes>
    </AppRouter>
  </React.Fragment>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(AppRoutingComponent, ['onSignIn', 'signIn']);
    this.onSignIn = this.onSignIn.bind(this);
    this.signIn = this.signIn.bind(this);
  }

  onSignIn(): void {
    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        return <Redirect to='/'/>;
      }
    });
  }

  signIn(): void {
    AnalyticsTracker.Registration.SignIn();
    this.signInService.signIn();
  }
}
