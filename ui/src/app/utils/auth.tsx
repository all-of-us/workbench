// utilities around Authority and page-based authorization

import {Authority, Profile} from 'generated/fetch';

// Admin actions guarded by a particular Authority
enum AuthGuardedAction {
    SHOW_ADMIN_MENU,
    USER_ADMIN,
    USER_AUDIT,
    WORKSPACE_ADMIN,
    WORKSPACE_AUDIT,
    SERVICE_BANNER,
    INSTITUTION_ADMIN,
    PUBLISH_WORKSPACE,
}

// The full set of Authorities which guard admin-menu actions
const adminMenuAuthorities = new Set([
  Authority.ACCESSCONTROLADMIN,
  Authority.RESEARCHERDATAVIEW,
  Authority.COMMUNICATIONSADMIN,
  Authority.INSTITUTIONADMIN,
]);

const authorityByPage: Map<AuthGuardedAction, Authority> = new Map([
    [AuthGuardedAction.USER_ADMIN, Authority.ACCESSCONTROLADMIN],
    [AuthGuardedAction.USER_AUDIT, Authority.ACCESSCONTROLADMIN],
    [AuthGuardedAction.WORKSPACE_ADMIN, Authority.RESEARCHERDATAVIEW],
    [AuthGuardedAction.WORKSPACE_AUDIT, Authority.RESEARCHERDATAVIEW],
    [AuthGuardedAction.SERVICE_BANNER, Authority.COMMUNICATIONSADMIN],
    [AuthGuardedAction.INSTITUTION_ADMIN, Authority.INSTITUTIONADMIN],
    [AuthGuardedAction.PUBLISH_WORKSPACE, Authority.FEATUREDWORKSPACEADMIN],
]);

const hasAuthorityForAction = (profile: Profile, action: AuthGuardedAction): boolean => {
  // DEVELOPER is the super-Authority which includes all others
  if (profile.authorities.includes(Authority.DEVELOPER)) {
    return true;
  }

  // return true if we have any of the menu-displaying authorities
  if (action === AuthGuardedAction.SHOW_ADMIN_MENU) {
    return profile.authorities.some(auth => adminMenuAuthorities.has(auth));
  }

  return profile.authorities.includes(authorityByPage.get(action));
};

export {
    AuthGuardedAction,
    hasAuthorityForAction,
};
