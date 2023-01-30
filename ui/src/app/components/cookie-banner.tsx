import * as React from 'react';
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { StyledRouterLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { firstPartyCookiesEnabled } from 'app/utils/cookies';
import cookies from 'assets/images/cookies.png';

const styles = reactStyles({
  cookiePolicyMessage: {
    position: 'fixed',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    padding: '0.75rem 1.5rem',
    borderTop: `7px solid ${colors.secondary}`,
    bottom: 0,
    backgroundColor: colors.light,
    // Above the footer, under modals / popups.
    zIndex: 103,
  },
  iconStyles: {
    height: 24,
    width: 24,
    color: colors.accent,
    cursor: 'pointer',
  },
});

const cookieKey = 'aou-cookie-banner-dismissed';

interface CookieBannerState {
  cookieBannerClosed: boolean;
}

export class CookieBanner extends React.Component<{}, CookieBannerState> {
  constructor(props) {
    super(props);
    this.state = {
      // This is only used to handle the removal of the cookie banner after x is clicked, without a refresh.
      cookieBannerClosed: false,
    };
  }

  handleCloseCookies() {
    if (firstPartyCookiesEnabled()) {
      this.setState({ cookieBannerClosed: true });
      localStorage.setItem(cookieKey, 'cookie-banner-dismissed');
    }
  }

  cookieBannerVisible() {
    if (firstPartyCookiesEnabled()) {
      return !localStorage.getItem(cookieKey) && !this.state.cookieBannerClosed;
    } else {
      return true;
    }
  }

  render() {
    return (
      this.cookieBannerVisible() && (
        <div style={styles.cookiePolicyMessage}>
          <FlexRow style={{ alignItems: 'center' }}>
            <img src={cookies} />
            <div style={{ paddingLeft: '1.5rem', color: colors.primary }}>
              We use cookies to help provide you with the best experience we
              can. By continuing to use our site, you consent to our{' '}
              <StyledRouterLink path='/cookie-policy' target='_blank'>
                Cookie Policy
              </StyledRouterLink>
              .
            </div>
          </FlexRow>
          <FontAwesomeIcon
            icon={faTimes}
            style={styles.iconStyles}
            onClick={() => this.handleCloseCookies()}
          />
        </div>
      )
    );
  }
}
