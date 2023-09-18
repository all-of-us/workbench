import { Authority } from 'generated/fetch';

import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { AuthorityGuardedAction, hasAuthorityForAction } from './authorities';

const noAuth = ProfileStubVariables.PROFILE_STUB;

const instAuth = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [Authority.INSTITUTION_ADMIN],
};
const accessAuth = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [Authority.ACCESS_CONTROL_ADMIN],
};
const devAuth = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [Authority.DEVELOPER],
};
const featuredWsAuth = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [Authority.FEATURED_WORKSPACE_ADMIN],
};

describe('authorities', () => {
  it('should correctly authorize INSTITUTION_ADMIN', async () => {
    expect(
      hasAuthorityForAction(noAuth, AuthorityGuardedAction.INSTITUTION_ADMIN)
    ).toBeFalsy();
    expect(
      hasAuthorityForAction(
        accessAuth,
        AuthorityGuardedAction.INSTITUTION_ADMIN
      )
    ).toBeFalsy();

    expect(
      hasAuthorityForAction(instAuth, AuthorityGuardedAction.INSTITUTION_ADMIN)
    ).toBeTruthy();
    expect(
      hasAuthorityForAction(devAuth, AuthorityGuardedAction.INSTITUTION_ADMIN)
    ).toBeTruthy();
  });

  it('should correctly authorize SHOW_ADMIN_MENU as a special case', async () => {
    expect(
      hasAuthorityForAction(noAuth, AuthorityGuardedAction.SHOW_ADMIN_MENU)
    ).toBeFalsy();
    // FEATUREDWORKSPACEADMIN does not guard anything in the admin menu
    expect(
      hasAuthorityForAction(
        featuredWsAuth,
        AuthorityGuardedAction.SHOW_ADMIN_MENU
      )
    ).toBeFalsy();
    // ACCESSCONTROLADMIN does
    expect(
      hasAuthorityForAction(accessAuth, AuthorityGuardedAction.SHOW_ADMIN_MENU)
    ).toBeTruthy();
    // DEVELOPER enables everything
    expect(
      hasAuthorityForAction(devAuth, AuthorityGuardedAction.SHOW_ADMIN_MENU)
    ).toBeTruthy();
  });
});
