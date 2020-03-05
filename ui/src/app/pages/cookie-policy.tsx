import {Component} from '@angular/core';
import {StyledAnchorTag} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header, SmallHeader} from 'app/components/headers';
import {AouTitle} from 'app/components/text-wrappers';
import {SIGNED_OUT_HEADER_IMAGE} from 'app/pages/login/sign-in';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';

const styles = {
  tableItem: {
    marginLeft: 6,
    marginTop: 6,
    height: 'auto',
    padding: '0.5rem',
    backgroundColor: colorWithWhiteness(colors.dark, 0.92),
    borderRadius: 5
  },
  wideItem: {
    width: '50%'
  },
  smallItem: {
    width: '25%',
  },
  tableHeader: {
    backgroundColor: colorWithWhiteness(colors.dark, 0.85),
    fontWeight: 500
  },
  header: {
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px',
    marginTop: 0
  },
  smallHeader: {
    fontSize: 16,
    fontWeight: 600,
  },
  textSection: {
    marginTop: '.75rem'
  }
};

const COOKIE_DELETION_LINK = 'https://www.aboutcookies.org/how-to-delete-cookies/';
const GOOGLE_PRIVACY_LINK = 'https://policies.google.com/privacy';
const ZENDESK_PRIVACY_LINK = 'https://www.zendesk.com/company/customers-partners/privacy-policy/';

export class CookiePolicy extends React.Component<{}, {}> {
  render() {
    return <React.Fragment>
      <div style={{width: '100%', height: '3.5rem',
        borderBottom: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`}}>
        <img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
             src={SIGNED_OUT_HEADER_IMAGE}/>
      </div>
      <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%', color: colors.primary}}>
        <Header style={styles.header}><AouTitle/> Cookie Policy</Header>
        <div style={styles.textSection}>
          The <AouTitle/> platform uses cookies to help provide you with the best experience we can.
          Cookies are small text files that are placed on your computer or mobile phone when you browse websites.
        </div>
        <div style={styles.textSection}>
          Our cookies help us to:
          <ul>
            <li>Make our platform function as you would expect</li>
            <li>Improve the speed and security of the site</li>
            <li>Continuously improve our platform for you</li>
          </ul>
        </div>
        <div style={styles.textSection}>
          We do not use cookies to:
          <ul>
            <li>Collect any personally identifiable information (without your express permission)</li>
            <li>Collect any sensitive information (without your express permission)</li>
            <li>Pass personally identifiable data to third parties</li>
            <li>Pay sales commissions</li>
          </ul>
        </div>
        <div style={styles.textSection}>
          You can learn more about all the cookies we use below.
        </div>
        <SmallHeader style={styles.smallHeader}>Granting us permission to use cookies</SmallHeader>
        <div style={styles.textSection}>If the settings on your software that you are using to view
          the <AouTitle/> platform are adjusted to accept cookies, we take this, and your continued use of
          our platform, to mean that you accept this. Should you wish to not accept cookies, you can learn how to do
          this below.</div>
        <SmallHeader style={styles.smallHeader}>Anonymous visitor statistics cookies</SmallHeader>
        <div style={styles.textSection}>
          We use cookies to compile visitor statistics including the number of visitors to our platform, technology used
          to visit our platform (e.g. operating systems, helping to identify specific problems), time spent on the
          platform, popular pages, etc. This helps us to continuously improve our platform. Analytics programs also tell
          us, anonymously, how visitors found our platform (e.g. through a search engine) and whether they have been
          here before, helping us to invest in the development of our services instead of marketing.
        </div>
        <div style={styles.textSection}>
          There will also be cookies on our platform placed by third parties whose services we use to deliver our
          services. To control third-party cookies, you can adjust your browser settings, as below.
        </div>
        <SmallHeader style={styles.smallHeader}>Cookies we use</SmallHeader>
        <FlexRow>
          <div style={{...styles.tableItem, ...styles.smallItem, ...styles.tableHeader}}>Cookie ID</div>
          <div style={{...styles.tableItem, ...styles.smallItem, ...styles.tableHeader}}>Provider</div>
          <div style={{...styles.tableItem, ...styles.wideItem, ...styles.tableHeader}}>Description</div>
        </FlexRow>
        <FlexRow>
          <div style={{...styles.tableItem, ...styles.smallItem}}>__cfduid</div>
          <div style={{...styles.tableItem, ...styles.smallItem}}><StyledAnchorTag href={ZENDESK_PRIVACY_LINK}>
            Zendesk</StyledAnchorTag></div>
          <div style={{...styles.tableItem, ...styles.wideItem}}>Used by the CloudFlare content delivery network to
            identify trusted web traffic from the embedded Zendesk help widget.</div>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{...styles.tableItem, ...styles.smallItem}}>
            <div>_ga*</div>
            <div>_gid*</div>
          </FlexColumn>
          <div style={{...styles.tableItem, ...styles.smallItem}}><StyledAnchorTag href={GOOGLE_PRIVACY_LINK}>
            Google</StyledAnchorTag></div>
          <div style={{...styles.tableItem, ...styles.wideItem}}>Used by Google Analytics to distinguish users.</div>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{...styles.tableItem, ...styles.smallItem}}>
            <div>1P_JAR</div>
            <div>APISID</div>
            <div>HSID</div>
            <div>NID</div>
            <div>SAPISID</div>
            <div>SID</div>
            <div>SIDCC</div>
            <div>SSID</div>
          </FlexColumn>
          <div style={{...styles.tableItem, ...styles.smallItem}}><StyledAnchorTag href={GOOGLE_PRIVACY_LINK}>
            Google</StyledAnchorTag></div>
          <div style={{...styles.tableItem, ...styles.wideItem}}>Used to track site navigation, responsiveness, user
            preferences, and other metrics to help us improve visitors' experiences on our sites.</div>
        </FlexRow>
        <SmallHeader style={styles.smallHeader}>Controlling Cookies</SmallHeader>
        <div style={styles.textSection}>You may also refuse to accept cookies by activating the setting on your browser
          which allows you to refuse the setting of cookies. However, if you select this setting you may be unable to
          access certain parts of our site. Unless you have adjusted your browser setting so that it will refuse
          cookies, our system will issue cookies when you log on to our site.</div>
        <div style={styles.textSection}>If you wish to delete cookies, you can do so through your browser, and further
          information on how to do this can be found at the following <StyledAnchorTag href={COOKIE_DELETION_LINK}>
            link</StyledAnchorTag>.</div>
      </FadeBox>
    </React.Fragment>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class CookiePolicyComponent extends ReactWrapperBase {
  constructor() {
    super(CookiePolicy, []);
  }
}
