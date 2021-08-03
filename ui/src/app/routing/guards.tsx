import {GuardFunction} from "react-router-guards";
import {authStore, profileStore} from "../utils/stores";
import {hasRegisteredAccess} from "../utils/access-tiers";

// export const signInGuard: Guard = {
//   allowed: (): boolean => {
//     // console.log(authStore.get().isSignedIn);
//     return authStore.get().isSignedIn;
//   },
//   redirectPath: '/login'
// };


export const signInGuard: GuardFunction = (to, from, next) => {
  if (to.meta[RouteAccess.SIGNED_IN_ONLY] /*&& !to.meta.userSignedIn*/) {
    console.log("redirecting to login");
    next.redirect('/login');
  }
  else {
    console.log("not redirecting to login!")
    next();
  }
}

// export const disabledGuard = (userDisabled: boolean): Guard => ({
//   allowed: (): boolean => !userDisabled,
//   redirectPath: '/user-disabled'
// });

export const disabledGuard: GuardFunction = (to, from, next) => {
  if (to.meta[RouteAccess.ENABLED_ONLY] && to.meta.userDisabled) {
    next.redirect('/user-disabled');
  }
  else {
    next();
  }
}

// export const registrationGuard: Guard = {
//   allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
//   redirectPath: '/'
// };

export const registrationGuard: GuardFunction = (to, from, next) => {
  if (to.meta[RouteAccess.REGISTERED_ONLY] && !hasRegisteredAccess(profileStore.get().profile.accessTierShortNames)) {
    next.redirect('/');
  }
  else {
    next();
  }
}

// export const expiredGuard: Guard = {
//   allowed: (): boolean => !profileStore.get().profile.renewableAccessModules.anyModuleHasExpired,
//   redirectPath: '/access-renewal'
// };

export const expiredGuard: GuardFunction = (to, from, next) => {
  if (to.meta[RouteAccess.UNEXPIRED_ONLY] && profileStore.get().profile.renewableAccessModules.anyModuleHasExpired) {
    next.redirect('/access-renewal')
  }
  else {
    next();
  }
}

export enum RouteAccess {
  SIGNED_IN_ONLY = "SIGNED_IN_ONLY",
  ENABLED_ONLY = "ENABLED_ONLY",
  REGISTERED_ONLY = "REGISTERED_ONLY",
  UNEXPIRED_ONLY = "UNEXPIRED_ONLY"
}
