import {Component, Input} from "@angular/core";
import {reactStyles, ReactWrapperBase, withUserProfile} from "../../utils";
import {Subscription} from "rxjs";
import * as React from "react";
import {ClrIcon} from "../../components/icons";
import colors from "../../styles/colors";
import {BreadCrumb} from "primereact/breadcrumb";
import {environment} from "../../../environments/environment";
import {MenuItem} from "../../components/buttons";

const styles = reactStyles({
  headerContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    boxShadow: '3px 0px 10px',
    paddingTop: '1rem',
    paddingBottom: '0.5rem',
    backgroundColor: 'white',
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
    color: '#FFFFFF',
    fontFamily: 'Montserrat',
    fontSize: '8px',
    lineHeight: '12px',
    textAlign: 'center',
  },
  sideNav: {
    backgroundColor: colors.primary,
    position: 'absolute',
    top: '4rem',
    bottom: '0',
    zIndex: 1500,
    flexGrow: 1,
    width: '10rem',
    boxShadow: '0px 3px 10px',
    opacity: 1,
    transition: 'opacity 0.5s',
  },
  navLink: {
    margin: 0,
    padding: '0.5rem 1.5rem',
    textAlign: 'left',
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
}

export const SignedInNavBar = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        profileLoadingSub: null,
        subscriptions: [],
        sideNavVisible: false
      };
    }

    onToggleSideNav() {
      this.setState(previousState => ({sideNavVisible: !previousState.sideNavVisible}))
    }

    render() {
      return <div style={styles.headerContainer}>
        <div style={styles.sidenavToggle}>
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
          <img
            src={this.props.headerImg}
            style={styles.headerImage}
            //routerLink='/'
          />
          {
            this.props.shouldShowDisplayTag
              ? <div style={styles.displayTag}>
                {this.props.displayTag}
              </div>
              : null
          }
        </div>
        <BreadCrumb/>
        {
          this.state.sideNavVisible
          && <div style={styles.sideNav}>
              <section>
                <MenuItem icon="user">{this.props.givenName} {this.props.familyName}</MenuItem>
                <MenuItem icon="home" solid={true}>Home</MenuItem>
                <MenuItem icon="applications" solid={true}>Your Workspaces</MenuItem>
                <MenuItem icon="star" solid={true}>Featured Workspaces</MenuItem>
                <MenuItem icon="help" solid={true}>User Support</MenuItem>
              </section>
          </div>
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

