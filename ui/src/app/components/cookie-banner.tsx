import {faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {FlexRow} from 'app/components/flex';
import colors from 'app/styles/colors';
import {cookiesEnabled, reactStyles} from 'app/utils';
import * as React from 'react';
import {StyledAnchorTag} from 'app/components/buttons';

const styles = reactStyles({
  cookiePolicyMessage: {
    position: 'fixed',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    padding: '0.5rem 1rem',
    borderTop: `7px solid ${colors.secondary}`,
    bottom: 0,
    backgroundColor: colors.light
  },
  iconStyles: {
    height: 24,
    width: 24,
    color: colors.accent,
    cursor: 'pointer'
  }
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
    if (cookiesEnabled()) {
      this.setState({cookieBannerClosed: true});
      localStorage.setItem(cookieKey, 'cookie-banner-dismissed');
    }
  }

  cookieBannerVisible() {
    if (cookiesEnabled()) {
      return !localStorage.getItem(cookieKey) && !this.state.cookieBannerClosed;
    } else {
      return true;
    }
  }

  render() {
    return this.cookieBannerVisible() && <div style={styles.cookiePolicyMessage}>
            <FlexRow style={{alignItems: 'center'}}>
                <img src='assets/images/cookies.png'/>
                <div style={{paddingLeft: '1rem', color: colors.primary}}>
                    We use cookies to help provide you with the best experience we can. By continuing to use our site, you consent
                    to our <StyledAnchorTag href='/cookie-policy' target='_blank'>Cookie Policy</StyledAnchorTag>.
                </div>
            </FlexRow>
            <FontAwesomeIcon icon={faTimes} style={styles.iconStyles} onClick={() => this.handleCloseCookies()} />
        </div>;
  }
}
