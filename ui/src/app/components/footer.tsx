import {Component, Input} from '@angular/core';
import { Link, StyledAnchorTag } from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {openZendeskWidget} from 'app/utils/zendesk';
import { environment } from 'environments/environment';
import * as React from 'react';

const styles = reactStyles({
  footerAnchor: {
    color: colors.secondary
  },
  footerAside: {
    color: colors.white,
    fontSize: 10,
    lineHeight: '18px',
    alignSelf: 'center'
  },
  footerImage: {
    width: '6rem',
    height: '2rem'
  },
  footerSectionHeader: {
    color: colors.white,
    borderBottom: `1px solid ${colors.white}`,
    width: '100%'
  },
  footerTemplate: {
    width: '100%',
    height: '6.5rem',
    backgroundColor: colors.primary,
    padding: '1rem'
  },
  workbenchFooterItem: {
    width: '30%'
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



const FooterTemplate = ({style = {}, ...props}) => {
  return <div style={{...styles.footerTemplate, ...style}} {...props}>
    <FlexRow>
      <FlexColumn style={{height: '4rem', justifyContent: 'space-between'}}>
        <img style={styles.footerImage} src='assets/images/all-of-us-logo-footer.svg'/>
        <img style={{...styles.footerImage, height: '1rem'}} src='assets/images/nih-logo-footer.png'/>
      </FlexColumn>
      <div style={{marginLeft: '1.5rem', width: '100%', marginTop: '0.5rem'}}>
        {props.children}
      </div>
    </FlexRow>
  </div>;
};

const FooterSection = ({style = {}, header, ...props}) => {
  return <FlexColumn style={style}>
    <div style={styles.footerSectionHeader}>{header}</div>
    <div>{props.children}</div>
  </FlexColumn>;
};

const gettingStartedUrl = 'https://aousupporthelp.zendesk.com/hc/en-us/categories/360002157352-Getting-Started';
const documentationUrl = 'https://aousupporthelp.zendesk.com/hc/en-us/categories/360002625291-Documentation';
const communityForumUrl = 'https://aousupporthelp.zendesk.com/hc/en-us/community/topics';
const faqUrl = 'https://aousupporthelp.zendesk.com/hc/en-us/categories/360002157532-Frequently-Asked-Questions';

interface WorkbenchFooterProps {
  profileState;
}

const WorkbenchFooter = withUserProfile()(
  class extends React.Component<WorkbenchFooterProps, {}> {
    constructor(props) {
      super(props);
    }

    render() {
      return <FooterTemplate>
        <FlexRow style={{justifyContent: 'space-between', width: '100%', height: '100%'}}>
          <FooterSection style={styles.workbenchFooterItem} header='Quick Links'>
            <FlexRow>
              <FlexColumn style={{width: '50%'}}>
                <FooterAnchorTag href='/'>Home</FooterAnchorTag>
                <FooterAnchorTag href='library'>Featured Workspaces</FooterAnchorTag>
                <FooterAnchorTag href='workspaces'>Your workspaces</FooterAnchorTag>
              </FlexColumn>
              <FlexColumn style={{width: '50%'}}>
                <NewTabFooterAnchorTag href={environment.publicUiUrl}>
                  Data Browser
                </NewTabFooterAnchorTag>
                <NewTabFooterAnchorTag href='https://researchallofus.org'>
                  Research Hub
                </NewTabFooterAnchorTag>
              </FlexColumn>
            </FlexRow>
          </FooterSection>
          <FooterSection style={styles.workbenchFooterItem} header='User Support'>
            <FlexRow>
              <FlexColumn style={{width: '50%'}}>
                <NewTabFooterAnchorTag href={gettingStartedUrl}>
                  Getting Started
                </NewTabFooterAnchorTag>
                <NewTabFooterAnchorTag href={documentationUrl}>
                  Documentation
                </NewTabFooterAnchorTag>
                <NewTabFooterAnchorTag href={communityForumUrl}>
                  Community Forum
                </NewTabFooterAnchorTag>
              </FlexColumn>
              <FlexColumn style={{width: '50%'}}>
                <NewTabFooterAnchorTag href={faqUrl}>
                  FAQs
                </NewTabFooterAnchorTag>
                <Link style={styles.footerAnchor} onClick={() => {
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
          <div style={{...styles.workbenchFooterItem, ...styles.footerAside}}>
            The <i>All of Us</i> logo is a service mark of the
            U.S. Department of Health and Human Services.
            The <i>All of Us</i> platform is for research only and does
            not provide medical advice, diagnosis or treatment. Copyright 2020.
          </div>
        </FlexRow>
      </FooterTemplate>;
    }
  });

const supportEmailAddress = 'support@researchallofus.org';

const RegistrationFooter = ({style = {}, ...props}) => {
  return <FooterTemplate {...props}>
    <FlexColumn>
      <FlexRow style={{...styles.footerSectionHeader, width: '25rem'}}>
        <NewTabFooterAnchorTag href={environment.publicUiUrl}>
          Data Browser
        </NewTabFooterAnchorTag>
        <NewTabFooterAnchorTag style={{marginLeft: '1.5rem'}} href='https://researchallofus.org'>
          Research Hub
        </NewTabFooterAnchorTag>
        <div style={{marginLeft: '1.5rem'}}>
          Contact Us: <FooterAnchorTag href={'mailto:' + supportEmailAddress}>
            {supportEmailAddress}
          </FooterAnchorTag>
        </div>
      </FlexRow>
      <div style={{...styles.footerAside, alignSelf: 'flex-start', marginTop: '1rem'}}>The All of Us logo is a service
        mark of the U.S. Department of Health and Human Services.</div>
      <div style={{...styles.footerAside, alignSelf: 'flex-start'}}>The All of Us platform is for research only and
        does not provide medical advice, diagnosis or treatment. Copyright 2020.</div>
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

@Component({
  selector: 'app-footer',
  template: '<div #root></div>'
})
export class FooterComponent extends ReactWrapperBase {
  @Input('type') type: FooterProps['type'];
  constructor() {
    super(Footer, [
      'type',
    ]);
  }
}

