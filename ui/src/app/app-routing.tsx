import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, ProtectedRoutes, withFullHeight, withRouteData} from 'app/components/app-router';
import {WorkspaceAudit} from 'app/pages/admin/admin-workspace-audit';
import {UserAudit} from 'app/pages/admin/user-audit';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {UserDisabled} from 'app/pages/user-disabled';
import {SignInService} from 'app/services/sign-in.service';
import {hasRegisteredAccess, ReactWrapperBase} from 'app/utils';
import {authStore, profileStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect} from 'react-router';
import {NOTEBOOK_HELP_CONTENT} from './components/help-sidebar';
import {AdminInstitution} from './pages/admin/admin-institution';
import {AdminInstitutionEdit} from './pages/admin/admin-institution-edit';
import {AdminNotebookView} from './pages/admin/admin-notebook-view';
import {AdminUser} from './pages/admin/admin-user';
import {AdminUsers} from './pages/admin/admin-users';
import {AdminWorkspace} from './pages/admin/admin-workspace';
import {AdminWorkspaceSearch} from './pages/admin/admin-workspace-search';
import {InteractiveNotebook} from './pages/analysis/interactive-notebook';
import {NotebookList} from './pages/analysis/notebook-list';
import {NotebookRedirect} from './pages/analysis/notebook-redirect';
import {Homepage} from './pages/homepage/homepage';
import {SignIn} from './pages/login/sign-in';
import {WorkspaceLibrary} from './pages/workspace/workspace-library';
import {AnalyticsTracker} from './utils/analytics';
import {BreadcrumbType} from './utils/navigation';
import {AdminUser} from "./pages/admin/admin-user";
import {AdminUsers} from "./pages/admin/admin-users";


const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.dataAccessLevel),
  redirectPath: '/'
};

const AdminNotebookViewPage = withRouteData(AdminNotebookView);
const CookiePolicyPage = withRouteData(CookiePolicy);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);
const HomepagePage = withRouteData(Homepage); // this name is bad i am sorry
const InstitutionAdminPage = withRouteData(AdminInstitution);
const InstitutionEditAdminPage = withRouteData(AdminInstitutionEdit);
const InteractiveNotebookPage = withRouteData(InteractiveNotebook);
const NotebookListPage = withRouteData(NotebookList);
const NotebookRedirectPage = withRouteData(NotebookRedirect);
const SessionExpiredPage = withRouteData(SessionExpired);
const SignInAgainPage = withRouteData(SignInAgain);
const SignInPage = withRouteData(SignIn);
const UserAdminPage = withRouteData(AdminUser);
const UsersAdminPage = withRouteData(AdminUsers);
const UserAuditPage = withRouteData(UserAudit);
const UserDisabledPage = withRouteData(UserDisabled);
const WorkspaceAdminPage = withRouteData(AdminWorkspace);
const WorkspaceAuditPage = withRouteData(WorkspaceAudit);
const WorkspaceSearchAdminPage = withRouteData(AdminWorkspaceSearch);
const WorkspaceLibraryPage = withRouteData(WorkspaceLibrary);

interface RoutingProps {
  onSignIn: () => void;
  signIn: () => void;
}

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = ({onSignIn, signIn}) => {
  const {authLoaded = false} = useStore(authStore);
  return authLoaded && <AppRouter>
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
      <AppRoute
        path='/'
          component={() => <HomepagePage routeData={{title: 'Homepage'}}/>}
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
      <AppRoute path='/nih-callback' component={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />

      <ProtectedRoutes guards={[registrationGuard]}>
        <AppRoute
          path='/library'
          component={() => <WorkspaceLibraryPage routeData={{title: 'Workspace Library', minimizeChrome: false}}/>}
        />
        <AppRoute
          path='/workspaces/:ns/:wsid/notebooks'
          component={() => <NotebookListPage routeData={{
            title: 'View Notebooks',
            helpContentKey: 'notebooks',
            breadcrumb: BreadcrumbType.Workspace
          }}/>}
        />
        <AppRoute
          path='/workspaces/:ns/:wsid/notebooks/preview/:nbName'
          component={() => <InteractiveNotebookPage routeData={{
            pathElementForTitle: 'nbName',
            breadcrumb: BreadcrumbType.Notebook,
            helpContentKey: NOTEBOOK_HELP_CONTENT,
            notebookHelpSidebarStyles: true,
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
            helpContentKey: NOTEBOOK_HELP_CONTENT,
            notebookHelpSidebarStyles: true,
            minimizeChrome: true
          }}/>}
        />
      </ProtectedRoutes>
    </ProtectedRoutes>
  </AppRouter>;
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
