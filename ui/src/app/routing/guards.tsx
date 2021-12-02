import {Guard} from 'app/components/app-router';
import {hasRegisteredTierAccess} from 'app/utils/access-tiers';
import {authStore, MatchParams, profileStore} from 'app/utils/stores';
import {eligibleForRegisteredTier} from 'app/utils/access-utils';
import {useParams} from 'react-router-dom';
import {currentWorkspaceStore} from 'app/utils/navigation';

export const signInGuard: Guard = {
  allowed: (): boolean => {
    return authStore.get().isSignedIn;
  },
  redirectPath: '/login'
};

const userIsEnabled = (userDisabledInDb: boolean) =>
    (!userDisabledInDb && eligibleForRegisteredTier(profileStore.get().profile.tierEligibilities));

export const disabledGuard = (userDisabledInDb: boolean): Guard => ({
  // Show disabled screen when user account is disabled by admin or removed from institution registered tier requirement.
  allowed: (): boolean => userIsEnabled(userDisabledInDb),
  redirectPath: '/user-disabled'
});

export const userDisabledPageGuard = (userDisabledInDb: boolean): Guard => ({
  // enabled users should be redirected to the homepage if they visit the /user-disabled page
  allowed: (): boolean => !userIsEnabled(userDisabledInDb),
  redirectPath: '/'
});

export const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredTierAccess(profileStore.get().profile),
  redirectPath: '/data-access-requirements'
};

export const expiredGuard: Guard = {
  allowed: (): boolean => !profileStore.get().profile.accessModules.anyModuleHasExpired,
  redirectPath: '/access-renewal'
};

export const adminLockedGuard = (): Guard => {
  const {ns, wsid} = useParams<MatchParams>();
  return ({
    allowed: (): boolean => (!currentWorkspaceStore.getValue().adminLocked),
    redirectPath: `/workspaces/${ns}/${wsid}/about`
  });
};
