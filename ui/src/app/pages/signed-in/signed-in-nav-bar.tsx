import {Component, Input} from "@angular/core";
import {reactStyles, ReactWrapperBase, withUserProfile} from "app/utils";
import {Subscription} from "rxjs";
import * as React from "react";
import {ClrIcon} from "app/components/icons";
import colors from "app/styles/colors";
import {Breadcrumb} from "app/components/breadcrumb";
import {SideNav} from "app/components/side-nav";

const styles = reactStyles({
  headerContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    boxShadow: '3px 0px 10px',
    paddingTop: '1rem',
    paddingBottom: '0.5rem',
    backgroundColor: colors.white,
    /*
     * NOTE: if you ever need to change this number, you need to ALSO change the
     * min-height calc in .content-container in signed-in/component.css or we'll
     * wind up with a container that is either too short or so tall it creates a
     * scrollbar
     */
    height: '4rem',
  },
  sidenavToggle: {
    transform: 'rotate(0deg)',
    display: 'inline-block',
    marginLeft: '1rem',
    transition: 'transform 0.5s',
  },
  headerImage: {
    height: '57px',
    width: '155px',
    marginLeft: '1rem',
  },
  displayTag: {
    marginLeft: '1rem',
    height: '12px',
    width: '126px',
    borderRadius: '2px',
    backgroundColor: colors.primary,
    color: colors.white,
    fontFamily: 'Montserrat',
    fontSize: '8px',
    lineHeight: '12px',
    textAlign: 'center',
  }
});

export interface Props {
  hasDataAccess: boolean;
  hasReviewResearchPurpose: boolean;
  hasAccessModuleAdmin: boolean;
  headerImg: string;
  displayTag: string;
  shouldShowDisplayTag: boolean;
  givenName: string;
  familyName: string;
  aouAccountEmailAddress: string;
  contactEmailAddress: string;
  profileImage: string;
  sidenavToggle: boolean;
  publicUiUrl: string;
  minimizeChrome: boolean;
  zendeskLoadError: boolean;
}

export interface State {
  profileLoadingSub: Subscription;
  subscriptions: Array<Subscription>;
  sideNavVisible: boolean;
  barsTransform: string;
}

const barsTransformNotRotated = 'rotate(0deg)';
const barsTransformRotated = 'rotate(90deg)';

export const SignedInNavBar = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        profileLoadingSub: null,
        subscriptions: [],
        sideNavVisible: false,
        barsTransform: barsTransformNotRotated
      };
    }

    onToggleSideNav() {
      this.setState(previousState => ({sideNavVisible: !previousState.sideNavVisible}));
      this.setState(previousState => ({
        barsTransform: previousState.barsTransform === barsTransformNotRotated
          ? barsTransformRotated
          : barsTransformNotRotated
      }));
    }

    render() {
      return <div style={styles.headerContainer}>
        <div style={{
          transform: this.state.barsTransform,
          display: 'inline-block',
          marginLeft: '1rem',
          transition: 'transform 0.5s',
        }}>
          <ClrIcon
            shape='bars'
            onClick={() => this.onToggleSideNav()}
            style={{
              width: '1.5rem',
              height: '1.5rem',
              fill: colors.accent
            }}
          >
          </ClrIcon>
        </div>
        <div>
          <a href={"/"}>
            <img
              src={this.props.headerImg}
              style={styles.headerImage}
            />
          </a>
          {
            this.props.shouldShowDisplayTag
              ? <div style={styles.displayTag}>
                {this.props.displayTag}
              </div>
              : null
          }
        </div>
        <Breadcrumb/>
        {
          this.state.sideNavVisible
          && <SideNav
            givenName={this.props.givenName}
            familyName={this.props.familyName}
          />
        }
      </div>
    }
  }
);

@Component({
  selector: 'app-signed-in-nav-bar',
  template: '<div #root></div>'
})
export class SignedInNavBarComponent extends ReactWrapperBase {
  @Input('hasDataAccess') hasDataAccess: Props['hasDataAccess'];
  @Input('hasReviewResearchPurpose') hasReviewResearchPurpose: Props['hasReviewResearchPurpose'];
  @Input('hasAccessModuleAdmin') hasAccessModuleAdmin: Props['hasAccessModuleAdmin'];
  @Input('headerImg') headerImg: Props['headerImg'];
  @Input('displayTag') displayTag: Props['displayTag'];
  @Input('shouldShowDisplayTag') shouldShowDisplayTag: Props['shouldShowDisplayTag'];
  @Input('givenName') givenName: Props['givenName'];
  @Input('familyName') familyName: Props['familyName'];
  @Input('aouAccountEmailAddress') aouAccountEmailAddress: Props['aouAccountEmailAddress'];
  @Input('contactEmailAddress') contactEmailAddress: Props['contactEmailAddress'];
  @Input('profileImage') profileImage: Props['profileImage'];
  @Input('publicUiUrl') publicUiUrl: Props['publicUiUrl'];
  @Input('minimizeChrome') minimizeChrome: Props['minimizeChrome'];
  @Input('zendeskLoadError') zendeskLoadError: Props['zendeskLoadError'];
  constructor() {
    super(SignedInNavBar, [
      'hasDataAccess',
      'hasReviewResearchPurpose',
      'hasAccessModuleAdmin',
      'headerImg',
      'displayTag',
      'shouldShowDisplayTag',
      'givenName',
      'familyName',
      'aouAccountEmailAddress',
      'contactEmailAddress',
      'profileImage',
      'publicUiUrl',
      'minimizeChrome',
      'zendeskLoadError',
    ]);
  }
}

