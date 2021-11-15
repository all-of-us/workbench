// utilities around Authority and page-based authorization

import {Authority, Profile} from 'generated/fetch';

// Admin actions guarded by a particular Authority
enum AuthorityGuardedAction {
    EGRESS_EVENTS,
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
  Authority.SECURITYADMIN
]);

const authorityByPage: Map<AuthorityGuardedAction, Authority> = new Map([
    [AuthorityGuardedAction.EGRESS_EVENTS, Authority.SECURITYADMIN],
    [AuthorityGuardedAction.USER_ADMIN, Authority.ACCESSCONTROLADMIN],
    [AuthorityGuardedAction.USER_AUDIT, Authority.ACCESSCONTROLADMIN],
    [AuthorityGuardedAction.WORKSPACE_ADMIN, Authority.RESEARCHERDATAVIEW],
    [AuthorityGuardedAction.WORKSPACE_AUDIT, Authority.RESEARCHERDATAVIEW],
    [AuthorityGuardedAction.SERVICE_BANNER, Authority.COMMUNICATIONSADMIN],
    [AuthorityGuardedAction.INSTITUTION_ADMIN, Authority.INSTITUTIONADMIN],
    [AuthorityGuardedAction.PUBLISH_WORKSPACE, Authority.FEATUREDWORKSPACEADMIN],
]);

const hasAuthorityForAction = (profile: Profile, action: AuthorityGuardedAction): boolean => {
  // DEVELOPER is the super-Authority which includes all others
  if (profile.authorities.includes(Authority.DEVELOPER)) {
    return true;
  }

  // return true if we have any of the menu-displaying authorities
  if (action === AuthorityGuardedAction.SHOW_ADMIN_MENU) {
    return profile.authorities.some(auth => adminMenuAuthorities.has(auth));
  }

  return profile.authorities.includes(authorityByPage.get(action));
};

export {
    AuthorityGuardedAction,
    hasAuthorityForAction,
};
