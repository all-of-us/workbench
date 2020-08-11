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
import { ReactWrapperBase } from 'app/utils';
import {authStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect} from 'react-router';
import {SignIn} from './pages/login/sign-in';
import {AnalyticsTracker} from './utils/analytics';
import {WorkspaceLibrary} from "./pages/workspace/workspace-library";
import {RegistrationGuard} from "./guards/registration-guard.service";


const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const registrationGuard: Guard = {
  // TODO lol
  allowed: (): boolean => true,
  redirectPath: '/login'
};

const CookiePolicyPage = withRouteData(CookiePolicy);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);
const SessionExpiredPage = withRouteData(SessionExpired);
const SignInAgainPage = withRouteData(SignInAgain);
const SignInPage = withRouteData(SignIn);
const UserAuditPage = withRouteData(UserAudit);
const UserDisabledPage = withRouteData(UserDisabled);
const WorkspaceAuditPage = withRouteData(WorkspaceAudit);
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
        <AppRoute path='/admin/user-audit'
                  component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}/>
      <AppRoute path='/admin/user-audit/:username'
                  component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}/>
      <AppRoute path='/admin/workspace-audit'
                  component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}/>
      <AppRoute path='/admin/workspace-audit/:workspaceNamespace'
                  component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}/>
      <AppRoute
          path='/data-code-of-conduct'
          component={ () => <DataUserCodeOfConductPage routeData={{
            title: 'Data User Code of Conduct',
            minimizeChrome: true
          }} />}
      />
      <ProtectedRoutes guards={[registrationGuard]}>
        <AppRoute
            path='/library'
            component={() => <WorkspaceLibraryPage routeData={{title: 'Workspace Library'}}/>}
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
