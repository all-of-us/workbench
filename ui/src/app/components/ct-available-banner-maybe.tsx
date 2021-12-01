import * as React from 'react';
import {useEffect, useState} from 'react';

import {ToastBanner, ToastType} from './toast-banner';
import {cookiesEnabled} from 'app/utils/cookies';
import {StyledRouterLink} from './buttons';
import {environment} from "../../environments/environment";
import {AccessTierShortNames} from "../utils/access-tiers";
import {AccessModule, CdrVersionTier, CdrVersionTiersResponse, Profile} from "../../generated/fetch";
import {eligibleForTier, getAccessModuleStatusByName} from "../utils/access-utils";
import {getCdrVersionTier} from "../utils/cdr-versions";
import {cdrVersionStore, profileStore, useStore} from "../utils/stores";

const CT_COOKIE_KEY = 'controlled-tier-available';
const DAR_PATH = '/data-access-requirements';

const shouldShowBanner = (profile: Profile, cdrVersionTiers: CdrVersionTier[]) => {
  // all of the following must be true
  const shouldShow =
    // the user is eligible for the CT
    eligibleForTier(profile, AccessTierShortNames.Controlled) &&
    // the user does not currently have CT access
    !profile.accessTierShortNames.includes(AccessTierShortNames.Controlled) &&
    // the environment allows users to see the CT (in the UI)
    environment.accessTiersVisibleToUsers.includes(AccessTierShortNames.Controlled) &&
    // the user's DUCC access module completion time was before the release of the default CT CDR Version
    (getAccessModuleStatusByName(profile, AccessModule.DATAUSERCODEOFCONDUCT).completionEpochMillis <
      cdrVersionTiers.find(v => v.accessTierShortName === AccessTierShortNames.Controlled)?.defaultCdrVersionCreationTime) &&
    // the user is not currently visiting the DAR page
    (window.location.pathname !== DAR_PATH);

  if (cookiesEnabled()) {
    const cookie = localStorage.getItem(CT_COOKIE_KEY);
    return !cookie && shouldShow;
  } else {
    return shouldShow;
  }
};

export const CTAvailableBannerMaybe = () => {
  const [showBanner, setShowBanner] = useState(false);
  const {profile} = useStore(profileStore);
  const {tiers} = useStore(cdrVersionStore);

  useEffect(() => setShowBanner(shouldShowBanner(profile, tiers)), []);

  const acknowledgeBanner = () => {
    if (cookiesEnabled()) {
      localStorage.setItem(CT_COOKIE_KEY, 'acknowledged');
    }
    setShowBanner(false);
  };

  const bannerBody = <div>
    To access genomic data and more in the <i>All of Us</i> controlled tier,
    <StyledRouterLink path={DAR_PATH}>complete the necessary steps</StyledRouterLink>.
  </div>;

  return showBanner
    ? <ToastBanner
      title='Controlled tier data now available.'
      message={bannerBody}
      footer='You can also do this later from your profile page.'
      onClose={() => acknowledgeBanner()}
      toastType={ToastType.INFO}/>
    : null;
}
