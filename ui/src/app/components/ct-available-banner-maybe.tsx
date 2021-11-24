import * as React from 'react';
import {useEffect, useState} from 'react';

import {ToastBanner, ToastType} from './toast-banner';
import {cookiesEnabled} from 'app/utils/cookies';
import {StyledRouterLink} from './buttons';

const CT_COOKIE_KEY = 'controlled-tier-available';
const DAR_PATH = '/data-access-requirements';

const shouldShowBanner = () => {
  // TODO implement the real logic
  const shouldShow = window.location.pathname !== DAR_PATH;
  if (cookiesEnabled()) {
    const cookie = localStorage.getItem(CT_COOKIE_KEY);
    return !cookie && shouldShow;
  } else {
    return shouldShow;
  }
};

export const CTAvailableBannerMaybe = () => {
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => setShowBanner(shouldShowBanner()), []);

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
