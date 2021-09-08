import { Link, StyledAnchorTag } from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SemiBoldHeader} from 'app/components/headers';
import {AoU} from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import {reactStyles, withUserProfile} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';
import { environment } from 'environments/environment';
import * as React from 'react';


const styles = reactStyles({
  footerAnchor: {
    color: colors.secondary,
    fontSize: 12,
  },
  footerAside: {
    color: colors.white,
    fontSize: 10,
    lineHeight: '22px',
    alignSelf: 'center'
  },
  footerImage: {
    width: '6rem',
    height: '2rem'
  },
  footerSectionHeader: {
    color: colors.white,
    fontSize: 12,
    marginTop: 0,
    textTransform: 'uppercase',
    width: '100%'
  },
  footerSectionDivider: {
    borderBottom: `1px solid ${colors.white}`,
    marginBottom: '.2rem',
    paddingBottom: '.2rem'
  },
  footerTemplate: {
    width: '100%',
    backgroundColor: colors.primary,
    padding: '1rem',
    // Must be higher than the side helpbar index for now. See help-bar.tsx.
    // Eventually the footer should be reflowed beneath this, per RW-5110. The footer
    // should be beneath any other floating elements, such as modals.
    zIndex: 102
  },
  workbenchFooterItem: {
    width: '12rem',
    marginRight: '3rem'
  }
});

const FooterAnchorTag = ({style = {}, href, ...props}) => {
  return <StyledAnchorTag style={{...styles.footerAnchor, ...style}} href={href} {...props}>
    {props.children}
  </StyledAnchorTag>;
};

const NewTabFooterAnchorTag = ({style = {}, href, ...props}) => {
  return <FooterAnchorTag style={style} href={href} target='_blank' {...props}>{props.children}</FooterAnchorTag>;
};

const DataBrowserLink = (props) => (
  <NewTabFooterAnchorTag href={environment.publicUiUrl}
                         analyticsFn={AnalyticsTracker.Footer.DataBrowser}
                         {...props}>
    Data Browser
  </NewTabFooterAnchorTag>
);

const ResearchHubLink = (props) => (
  <NewTabFooterAnchorTag href='https://researchallofus.org'
                         analyticsFn={AnalyticsTracker.Footer.ResearchHub}
                         {...props}>
    Research Hub
  </NewTabFooterAnchorTag>
);

const FooterTemplate = ({style = {}, ...props}) => {
  return <div style={{...styles.footerTemplate, ...style}} {...props}>
    <FlexRow>
      <FlexColumn style={{height: '4rem', justifyContent: 'space-between'}}>
        <img style={styles.footerImage} src='assets/images/all-of-us-logo-footer.svg'/>
        <img style={{...styles.footerImage, height: '1rem'}} src='assets/images/nih-logo-footer.png'/>
      </FlexColumn>
      <div style={{marginLeft: '1.5rem', width: '100%'}}>
        {props.children}
        <div style={{...styles.footerAside, marginTop: '20px'}}>
          The <AoU/> logo is a service mark of the&nbsp;
          <NewTabFooterAnchorTag
              href='https://www.hhs.gov'
              analyticsFn={AnalyticsTracker.Footer.HHS}>
            U.S. Department of Health and Human Services
          </NewTabFooterAnchorTag>.<br/>
          The <AoU/> platform is for research only and does not provide medical advice, diagnosis or treatment. Copyright 2020.
        </div>
      </div>
    </FlexRow>
  </div>;
};

const FooterSection = ({style = {}, header, ...props}) => {
  return <FlexColumn style={style}>
    <SemiBoldHeader style={{
      ...styles.footerSectionDivider,
      ...styles.footerSectionHeader}}>{header}</SemiBoldHeader>
    <div>{props.children}</div>
  </FlexColumn>;
};

interface WorkbenchFooterProps {
  profileState;
}

const WorkbenchFooter = withUserProfile()(
  class extends React.Component<WorkbenchFooterProps, {}> {
    constructor(props) {
      super(props);
    }

    render() {
      const tracker = AnalyticsTracker.Footer;
      return <FooterTemplate>
        <FlexRow style={{justifyContent: 'flex-start', width: '100%'}}>
          <FooterSection style={styles.workbenchFooterItem} header='Quick Links'>
            <FlexRow>
              <FlexColumn style={{width: '50%'}}>
                <FooterAnchorTag href='/'
                                 analyticsFn={tracker.Home}>
                  Home
                </FooterAnchorTag>
                <FooterAnchorTag href='library'
                                 analyticsFn={tracker.FeaturedWorkspaces}>
                  Featured Workspaces
                </FooterAnchorTag>
                <FooterAnchorTag href='workspaces'
                                 analyticsFn={tracker.YourWorkspaces}>
                  Your Workspaces
                </FooterAnchorTag>
              </FlexColumn>
              <FlexColumn style={{width: '50%'}}>
                <DataBrowserLink />
                <ResearchHubLink />
              </FlexColumn>
            </FlexRow>
          </FooterSection>
          <FooterSection style={styles.workbenchFooterItem} header='User Support Hub'>
            <FlexRow>
              <FlexColumn style={{width: '50%'}}>
                <NewTabFooterAnchorTag href={supportUrls.gettingStarted} analyticsFn={tracker.GettingStarted}>
                  Getting Started
                </NewTabFooterAnchorTag>
                <NewTabFooterAnchorTag href={supportUrls.tableOfContents}
                                       analyticsFn={tracker.SupportDocs}>
                  Documentation
                </NewTabFooterAnchorTag>
                <NewTabFooterAnchorTag href={supportUrls.communityForum}
                                       analyticsFn={tracker.CommunityForum}>
                  Community Forum
                </NewTabFooterAnchorTag>
              </FlexColumn>
              <FlexColumn style={{width: '50%'}}>
                <NewTabFooterAnchorTag href={supportUrls.faq}
                                       analyticsFn={tracker.SupportFAQ}>
                  FAQs
                </NewTabFooterAnchorTag>
                <Link style={styles.footerAnchor} onClick={() => {
                  tracker.ContactUs('Zendesk');
                  openZendeskWidget(
                    this.props.profileState.profile.givenName,
                    this.props.profileState.profile.familyName,
                    this.props.profileState.profile.username,
                    this.props.profileState.profile.contactEmail,
                  ); }
                } href='#'>
                  Contact Us
                </Link>
              </FlexColumn>
            </FlexRow>
          </FooterSection>
        </FlexRow>
      </FooterTemplate>;
    }
  });

const supportEmailAddress = 'support@researchallofus.org';

const RegistrationFooter = ({style = {}, ...props}) => {
  return <FooterTemplate {...props}>
    <FlexColumn>
      <FlexRow style={{...styles.footerSectionDivider, color: colors.white, width: '25rem'}}>
        <DataBrowserLink />
        <ResearchHubLink style={{marginLeft: '1.5rem'}} />
        <div style={{fontSize: 12, marginLeft: '1.5rem'}}>
          Contact Us: <FooterAnchorTag href={'mailto:' + supportEmailAddress}
                                       analyticsFn={() => AnalyticsTracker.Footer.ContactUs('Email')}>
            {supportEmailAddress}
          </FooterAnchorTag>
        </div>
      </FlexRow>
    </FlexColumn>
  </FooterTemplate>;
};


export enum FooterTypeEnum {
  Registration,
  Workbench,
}

interface FooterProps {
  type: FooterTypeEnum;
}

export class Footer extends React.Component<FooterProps> {
  render() {
    switch (this.props.type) {
      case FooterTypeEnum.Registration:
        return <RegistrationFooter />;
      case FooterTypeEnum.Workbench:
        return <WorkbenchFooter />;
    }
  }
}
