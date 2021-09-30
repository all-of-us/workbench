import {Guard} from 'app/components/app-router';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {authStore, profileStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {eligibaleForRegisteredForTier} from 'app/utils/access-utils';

export const signInGuard: Guard = {
  allowed: (): boolean => {
    return authStore.get().isSignedIn;
  },
  redirectPath: '/login'
};

export const disabledGuard = (userDisabled: boolean): Guard => ({
  // Show disable screen when user account is disabled or remoevd from registered tier by institution admin.
  allowed: (): boolean => (!userDisabled && eligibaleForRegisteredForTier(profileStore.get().profile.tierEligibilities)),
  redirectPath: '/user-disabled'
});

export const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
  redirectPath: '/data-access-requirements'
};

export const expiredGuard: Guard = {
  allowed: (): boolean => !profileStore.get().profile.accessModules.anyModuleHasExpired,
  redirectPath: '/access-renewal'
};
