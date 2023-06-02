import * as React from 'react';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

import { CdrVersionTier, Profile } from 'generated/fetch';

import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';
import {
  DATA_ACCESS_REQUIREMENTS_PATH,
  eligibleForTier,
} from 'app/utils/access-utils';
import { firstPartyCookiesEnabled } from 'app/utils/cookies';
import { cdrVersionStore, profileStore, useStore } from 'app/utils/stores';

import { StyledRouterLink } from './buttons';
import { AoU } from './text-wrappers';
import { ToastBanner, ToastType } from './toast-banner';

const CT_COOKIE_KEY = 'controlled-tier-available';

const shouldShowBanner = (
  profile: Profile,
  cdrVersionTiers: CdrVersionTier[],
  pathname: string
) => {
  const ctCdrVersions = cdrVersionTiers?.find(
    (v) => v.accessTierShortName === AccessTierShortNames.Controlled
  );

  // all of the following must be true
  const shouldShow =
    profile &&
    ctCdrVersions &&
    // the user is eligible for the CT
    eligibleForTier(profile, AccessTierShortNames.Controlled) &&
    // the user does not currently have CT access
    !profile.accessTierShortNames.includes(AccessTierShortNames.Controlled) &&
    // the user's first sign-in time was before the release of the default CT CDR Version
    profile.firstSignInTime < ctCdrVersions.defaultCdrVersionCreationTime &&
    // the user is not currently visiting the DAR page
    pathname !== DATA_ACCESS_REQUIREMENTS_PATH;

  if (firstPartyCookiesEnabled()) {
    const cookie = localStorage.getItem(CT_COOKIE_KEY);
    return !cookie && shouldShow;
  } else {
    return shouldShow;
  }
};

export const CTAvailableBannerMaybe = () => {
  const [showBanner, setShowBanner] = useState(false);
  const { profile } = useStore(profileStore);
  const { tiers } = useStore(cdrVersionStore);
  const { pathname } = useLocation();

  useEffect(
    () => setShowBanner(shouldShowBanner(profile, tiers, pathname)),
    [profile, tiers, pathname]
  );

  const acknowledgeBanner = () => {
    if (firstPartyCookiesEnabled()) {
      localStorage.setItem(CT_COOKIE_KEY, 'acknowledged');
    }
    setShowBanner(false);
  };

  const bannerBody = (
    <div data-test-id='controlled-tier-available'>
      To access genomic data and more in the <AoU />{' '}
      {AccessTierDisplayNames.Controlled},
      <StyledRouterLink path={DATA_ACCESS_REQUIREMENTS_PATH}>
        complete the necessary steps
      </StyledRouterLink>
      .
    </div>
  );

  return showBanner ? (
    <ToastBanner
      title={`${AccessTierDisplayNames.Controlled} data now available.`}
      message={bannerBody}
      footer='You can also do this later from your profile page.'
      onClose={() => acknowledgeBanner()}
      toastType={ToastType.INFO}
      zIndex={200}
    />
  ) : null;
};
