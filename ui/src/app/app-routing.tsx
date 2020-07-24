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


const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const CookiePolicyPage = withRouteData(CookiePolicy);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);
const SessionExpiredPage = withRouteData(SessionExpired);
const SignInAgainPage = withRouteData(SignInAgain);
const UserAuditPage = withRouteData(UserAudit);
const UserDisabledPage = withRouteData(UserDisabled);
const WorkspaceAuditPage = withRouteData(WorkspaceAudit);

export const AppRoutingComponent: React.FunctionComponent<{signIn: Function}> = ({signIn}) => {
  const {authLoaded = false} = useStore(authStore);
  return authLoaded && <AppRouter>
    <AppRoute
        path='/cookie-policy'
        component={() => <CookiePolicyPage routeData={{title: 'Cookie Policy'}}/>}
    />
    <AppRoute
        path='/session-expired'
        data={{signIn: signIn}}
        component={() => <SessionExpiredPage routeData={{title: 'You have been signed out'}}/>}
    />
    <AppRoute
        path='/sign-in-again'
        data={{signIn: signIn}}
        component={() => <SignInAgainPage routeData={{title: 'You have been signed out'}}/>}
    />
    <AppRoute
        path='/user-disabled'
        component={() => <UserDisabledPage routeData={{title: 'Disabled'}}/>}
    />

    <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute
          path='/data-code-of-conduct'
          component={ () => <DataUserCodeOfConductPage routeData={{
            title: 'Data User Code of Conduct',
            minimizeChrome: true
          }} />}
        />
        <AppRoute path='/admin/user-audit'
                  component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}/>
        <AppRoute path='/admin/user-audit/:username'
                  component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}/>
        <AppRoute path='/admin/workspace-audit'
                  component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}/>
        <AppRoute path='/admin/workspace-audit/:workspaceNamespace'
                  component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}/>
    </ProtectedRoutes>
  </AppRouter>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(AppRoutingComponent, ['signIn']);
    this.signIn = this.signIn.bind(this);
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
