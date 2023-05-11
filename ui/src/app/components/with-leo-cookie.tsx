import * as React from 'react';
import { useEffect, useState } from 'react';

import { leoProxyApi } from 'app/services/notebooks-swagger-fetch-clients';
import { authStore, notificationStore, useStore } from 'app/utils/stores';

const LeoCookieWrapper = ({ children }) => {
  const { auth } = useStore(authStore);

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const pollAborter = new AbortController();

    const setLeoCookie = () => {
      console.log('setting cookie');
      leoProxyApi()
        .setCookie({
          withCredentials: true,
          crossDomain: true,
          credentials: 'include',
          signal: pollAborter.signal,
        })
        .catch(() => {
          if (pollAborter.signal.aborted) {
            return;
          }

          notificationStore.set({
            title: 'Authentication Error',
            message:
              'There was an error authenticating with the environments server. Please refresh the page.',
            showBugReportLink: true,
          });
        })
        .finally(() => {
          console.log('Done setting cookie.');
          setLoading(false);
        });
    };

    setLeoCookie();
    const timer = setInterval(
      setLeoCookie,
      // The Leo cookie is associated with an access token and expires when the access token expires.
      // When we refresh a soon-to-expire access token "X" to obtain access token "Y", there is a small window of time
      // equal to accessTokenExpiringNotificationTimeInSeconds where both tokens are valid.
      // The Leo refresh interval must be shorter than this window to prevent an edge case where the cookie expires
      // before we associate the cookie with Y.
      // This implementation is based on Terra UI's implementation:
      // https://github.com/DataBiosphere/terra-ui/blob/356f27342ff44d322b2b52077fa4efb1c5f920ce/src/libs/auth.js#LL49C10-L49C10
      (auth.settings.accessTokenExpiringNotificationTimeInSeconds - 30) * 1000
    );

    return () => {
      clearTimeout(timer);
      console.log('aborting');
      pollAborter.abort();
    };
  }, []);

  if (loading) {
    return null;
  }

  return children;
};

export const withLeoCookie = (WrappedComponent) => {
  return (props) => (
    <LeoCookieWrapper>
      <WrappedComponent {...props} />
    </LeoCookieWrapper>
  );
};
