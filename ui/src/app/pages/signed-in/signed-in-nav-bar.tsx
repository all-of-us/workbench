import {Component, Input} from "@angular/core";
import {reactStyles, ReactWrapperBase, withUserProfile} from "app/utils";
import {Subscription} from "rxjs";
import * as React from "react";
import {ClrIcon} from "app/components/icons";
import colors from "app/styles/colors";
import {Breadcrumb} from "app/components/breadcrumb";
import {SideNav} from "app/components/side-nav";
import {RefObject} from "react"

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
  sidenavIcon: {
    width: '1.5rem',
    height: '1.5rem',
    fill: colors.accent,
  },
  sidenavIconHovering: {
    cursor: 'pointer',
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
  headerImg: string;
  displayTag: string;
  shouldShowDisplayTag: boolean;
  givenName: string;
  familyName: string;
  aouAccountEmailAddress: string;
  contactEmailAddress: string;
  profileImage: string;
  sidenavToggle: boolean;
  homeActive: boolean;
  workspacesActive: boolean;
  libraryActive: boolean;
}

export interface State {
  profileLoadingSub: Subscription;
  subscriptions: Array<Subscription>;
  sideNavVisible: boolean;
  barsTransform: string;
  hovering: boolean;
  wrapperRef: RefObject<HTMLDivElement>
}

const barsTransformNotRotated = 'rotate(0deg)';
const barsTransformRotated = 'rotate(90deg)';

export const SignedInNavBar = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      // Bind the this context - this will be passed down into the actual
      // sidenav so clicks on it can close the nav
      this.onToggleSideNav = this.onToggleSideNav.bind(this);
      this.handleClickOutside = this.handleClickOutside.bind(this);
      this.state = {
        profileLoadingSub: null,
        subscriptions: [],
        sideNavVisible: false,
        barsTransform: barsTransformNotRotated,
        hovering: false,
        wrapperRef: React.createRef(),
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

    componentDidMount() {
      document.addEventListener('click', this.handleClickOutside);
    }

    componentWillUnmount() {
      document.removeEventListener('click', this.handleClickOutside);
    }

    handleClickOutside(event) {
      if (
        this.state.wrapperRef
        && !this.state.wrapperRef.current.contains(event.target)
        && this.state.sideNavVisible
      ) {
        this.onToggleSideNav();
      }
    }

    render() {
      return <div
        style={styles.headerContainer}
        ref={this.state.wrapperRef}
      >
        <div style={{
          transform: this.state.barsTransform,
          display: 'inline-block',
          marginLeft: '1rem',
          transition: 'transform 0.5s',
        }}>
          <ClrIcon
            shape='bars'
            onClick={() => this.onToggleSideNav()}
            onMouseEnter={() => this.setState({hovering: true})}
            onMouseLeave={() => this.setState({hovering: false})}
            style={this.state.hovering
              ? {...styles.sidenavIcon, ...styles.sidenavIconHovering}
              : {...styles.sidenavIcon}}
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
            homeActive={this.props.homeActive}
            workspacesActive={this.props.workspacesActive}
            libraryActive={this.props.libraryActive}
            hasDataAccess={this.props.hasDataAccess}
            aouAccountEmailAddress={this.props.aouAccountEmailAddress}
            contactEmailAddress={this.props.contactEmailAddress}
            givenName={this.props.givenName}
            familyName={this.props.familyName}
            // Passing the function itself deliberately, we want to be able to
            // toggle the nav whenever we click anything in it
            onToggleSideNav={this.onToggleSideNav}
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
  @Input('headerImg') headerImg: Props['headerImg'];
  @Input('displayTag') displayTag: Props['displayTag'];
  @Input('shouldShowDisplayTag') shouldShowDisplayTag: Props['shouldShowDisplayTag'];
  @Input('givenName') givenName: Props['givenName'];
  @Input('familyName') familyName: Props['familyName'];
  @Input('aouAccountEmailAddress') aouAccountEmailAddress: Props['aouAccountEmailAddress'];
  @Input('contactEmailAddress') contactEmailAddress: Props['contactEmailAddress'];
  @Input('homeActive') homeActive: Props['homeActive'];
  @Input('workspacesActive') workspacesActive: Props['workspacesActive'];
  @Input('libraryActive') libraryActive: Props['libraryActive'];
  constructor() {
    super(SignedInNavBar, [
      'hasDataAccess',
      'headerImg',
      'displayTag',
      'shouldShowDisplayTag',
      'givenName',
      'familyName',
      'aouAccountEmailAddress',
      'contactEmailAddress',
      'homeActive',
      'workspacesActive',
      'libraryActive',
    ]);
  }
}

