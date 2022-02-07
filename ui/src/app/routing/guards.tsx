import { Guard } from 'app/components/app-router';
import {
  AccessTierShortNames,
  hasRegisteredTierAccess,
} from 'app/utils/access-tiers';
import { authStore, profileStore } from 'app/utils/stores';
import { ACCESS_RENEWAL_PATH, eligibleForTier } from 'app/utils/access-utils';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
} from 'app/utils/authorities';
import { AuthorityMissing } from './authority-missing';

export const signInGuard: Guard = {
  allowed: (): boolean => {
    return authStore.get().isSignedIn;
  },
  redirectPath: '/login',
};

const userIsEnabled = (userDisabledInDb: boolean) =>
  !userDisabledInDb &&
  eligibleForTier(profileStore.get().profile, AccessTierShortNames.Registered);

export const disabledGuard = (userDisabledInDb: boolean): Guard => ({
  // Show disabled screen when user account is disabled by admin or removed from institution registered tier requirement.
  allowed: (): boolean => userIsEnabled(userDisabledInDb),
  redirectPath: '/user-disabled',
});

export const userDisabledPageGuard = (userDisabledInDb: boolean): Guard => ({
  // enabled users should be redirected to the homepage if they visit the /user-disabled page
  allowed: (): boolean => !userIsEnabled(userDisabledInDb),
  redirectPath: '/',
});

export const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredTierAccess(profileStore.get().profile),
  redirectPath: '/data-access-requirements',
};

export const expiredGuard: Guard = {
  allowed: (): boolean =>
    !profileStore.get().profile.accessModules.anyModuleHasExpired,
  redirectPath: ACCESS_RENEWAL_PATH,
};

export const adminLockedGuard = (ns: string, wsid: string): Guard => {
  return {
    allowed: (): boolean => !currentWorkspaceStore.getValue().adminLocked,
    redirectPath: `/workspaces/${ns}/${wsid}/about`,
  };
};

export const authorityGuard = (
  guardedAction: AuthorityGuardedAction
): Guard => ({
  allowed: () =>
    hasAuthorityForAction(profileStore.get().profile, guardedAction),
  renderBlocked: () => <AuthorityMissing action={guardedAction} />,
});
