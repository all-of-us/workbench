import * as React from "react";
import {AuthGuardedAction, hasAuthorityForAction} from "./auth";
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {Authority} from 'generated/fetch';

const noAuth = ProfileStubVariables.PROFILE_STUB;

const instAuth = {...ProfileStubVariables.PROFILE_STUB, authorities: [Authority.INSTITUTIONADMIN]}
const accessAuth = {...ProfileStubVariables.PROFILE_STUB, authorities: [Authority.ACCESSCONTROLADMIN]}
const devAuth = {...ProfileStubVariables.PROFILE_STUB, authorities: [Authority.DEVELOPER]}
const featuredWsAuth = {...ProfileStubVariables.PROFILE_STUB, authorities: [Authority.FEATUREDWORKSPACEADMIN]}

describe('auth', () => {
    it('should correctly authorize INSTITUTION_ADMIN', async () => {
        expect(hasAuthorityForAction(noAuth, AuthGuardedAction.INSTITUTION_ADMIN)).toBeFalsy();
        expect(hasAuthorityForAction(accessAuth, AuthGuardedAction.INSTITUTION_ADMIN)).toBeFalsy();

        expect(hasAuthorityForAction(instAuth, AuthGuardedAction.INSTITUTION_ADMIN)).toBeTruthy();
        expect(hasAuthorityForAction(devAuth, AuthGuardedAction.INSTITUTION_ADMIN)).toBeTruthy();
    });

    it('should correctly authorize SHOW_ADMIN_MENU as a special case', async () => {
        expect(hasAuthorityForAction(noAuth, AuthGuardedAction.SHOW_ADMIN_MENU)).toBeFalsy();
        // FEATUREDWORKSPACEADMIN does not guard anything in the admin menu
        expect(hasAuthorityForAction(featuredWsAuth, AuthGuardedAction.SHOW_ADMIN_MENU)).toBeFalsy();
        // ACCESSCONTROLADMIN does
        expect(hasAuthorityForAction(accessAuth, AuthGuardedAction.SHOW_ADMIN_MENU)).toBeTruthy();
        // DEVELOPER enables everything
        expect(hasAuthorityForAction(devAuth, AuthGuardedAction.SHOW_ADMIN_MENU)).toBeTruthy();
    });
});