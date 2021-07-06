import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {AuthorityGuardedAction, hasAuthorityForAction} from 'app/utils/authorities';
import {navigate, navigateSignOut, signInStore} from 'app/utils/navigation';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {withRouter} from "react-router";

const styles = reactStyles({
  flex: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sideNav: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
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
  sideNavItem: {
    width: '100%',
    margin: 0,
    paddingLeft: '1rem',
    textAlign: 'left',
    textTransform: 'none',
    height: '2rem',
    color: colors.white,
  },
  sideNavItemActive: {
    backgroundColor: colorWithWhiteness(colors.primary, .2),
    fontWeight: 'bold',
  },
  sideNavItemHover: {
    backgroundColor: colorWithWhiteness(colors.primary, .2),
  },
  sideNavItemDisabled: {
    color: colors.disabled,
    cursor: 'auto',
  },
  navIcon: {
    marginRight: '12px'
  },
  noIconMargin: {
    marginLeft: '33px'
  },
  profileImage: {
    // Negative margin is kind of bad, but otherwise I'd need throw conditionals in
    // the margin of the entire sidenav for this one thing
    marginLeft: '-4px',
    marginRight: '8px',
    borderRadius: '100px',
    height: '29px',
    width: '29px',
  },
  dropdownIcon: {
    marginRight: '8px',
    transform: 'rotate(180deg)',
    transition: 'transform 0.5s',
  },
  dropdownIconOpen: {
    transform: 'rotate(0deg)',
  }
});

// TODO: React-router's withRouter doesn't update unless the parent component updates.
// so we do this instead until we can turn this into a function component and use withLocation
const bannerAdminActive = () => {
  return window.location.pathname === '/admin/banner';
}

const userAdminActive = () => {
  return window.location.pathname === '/admin/user';
}

const userAuditActive = () => {
  return window.location.pathname.startsWith('/admin/user-audit');
}

const workspaceAdminActive = () => {
  return window.location.pathname.startsWith('/admin/workspaces');
}

const workspaceAuditActive = () => {
  return window.location.pathname.startsWith('/admin/workspace-audit');
}

const homeActive = () => {
  return window.location.pathname === '';
}

const libraryActive = () => {
  return window.location.pathname === '/library';
}

const workspacesActive = () => {
  return window.location.pathname === '/workspaces';
}

const profileActive = () => {
  return window.location.pathname === '/profile';
}

interface SideNavItemProps {
  icon?: string;
  hasProfileImage?: boolean;
  content: string;
  parentOnClick?: Function;
  onToggleSideNav: Function;
  href?: string;
  containsSubItems?: boolean;
  active?: boolean;
  disabled?: boolean;
}

interface SideNavItemState {
  hovering: boolean;
  subItemsOpen: boolean;
}

export class SideNavItem extends React.Component<SideNavItemProps, SideNavItemState> {
  constructor(props) {
    super(props);
    this.state = {
      hovering: false,
      subItemsOpen: false,
    };
  }

  iconSize = 21;

  onClick() {
    if (this.props.href && !this.props.disabled) {
      this.props.onToggleSideNav();
      navigate([this.props.href]);
    }
    if (this.props.containsSubItems) {
      this.setState((previousState) => ({subItemsOpen: !previousState.subItemsOpen}));
    }
  }

  closeSubItems() {
    if (this.props.containsSubItems) {
      this.setState({subItemsOpen: false});
    }
  }

  getStyles(active, hovering, disabled) {
    let sideNavItemStyles = {
      ...styles.flex,
      ...styles.sideNavItem
    };
    if (disabled) {
      // We want to short-circuit in this case.
      return {...sideNavItemStyles, ...styles.sideNavItemDisabled};
    }
    if (active) {
      sideNavItemStyles = {...sideNavItemStyles, ...styles.sideNavItemActive};
    }
    if (hovering) {
      sideNavItemStyles = {...sideNavItemStyles, ...styles.sideNavItemHover};
    }
    return sideNavItemStyles;
  }

  render() {
    return <Clickable
      // data-test-id is the text within the SideNavItem, with whitespace removed
      // and appended with '-menu-item'
      data-test-id={this.props.content.toString().replace(/\s/g, '') + '-menu-item'}
      style={this.getStyles(this.props.active, this.state.hovering, this.props.disabled)}
      onClick={() => {
        if (this.props.parentOnClick && !this.props.disabled) {
          this.props.parentOnClick();
        }
        this.onClick();
      }}
      onMouseEnter={() => this.setState({hovering: true})}
      onMouseLeave={() => this.setState({hovering: false})}
    >
      <div
        style={{...styles.flex,
          flex: '1 0 auto'
        }}
      >
        <span
          style={
            this.props.icon || this.props.hasProfileImage
              ? {...styles.flex}
              : {...styles.noIconMargin}
          }
        >
          {
            this.props.icon && <ClrIcon
              shape={this.props.icon}
              className={'is-solid'}
              style={styles.navIcon}
              size={this.iconSize}
            />
          }
          {
            this.props.hasProfileImage && <img
              src={signInStore.getValue().profileImage}
              style={styles.profileImage}
            />
          }
          {this.props.content}
        </span>
        {
          this.props.containsSubItems
          && <ClrIcon
            shape='angle'
            style={
              this.state.subItemsOpen
                ? {...styles.dropdownIcon, ...styles.dropdownIconOpen}
                : styles.dropdownIcon
            }
            size={this.iconSize}
          />
        }
      </div>
    </Clickable>;
  }
}

export interface SideNavProps {
  profile: Profile;
  onToggleSideNav: Function;
}

export interface SideNavState {
  showAdminOptions: boolean;
  showUserOptions: boolean;
  adminRef: React.RefObject<SideNavItem>;
  userRef: React.RefObject<SideNavItem>;
}

export class SideNav extends React.Component<SideNavProps, SideNavState> {
  constructor(props) {
    super(props);
    this.state = {
      showAdminOptions: false,
      showUserOptions: false,
      adminRef: React.createRef(),
      userRef: React.createRef(),
    };
  }

  onToggleUser() {
    this.setState(previousState => ({showUserOptions: !previousState.showUserOptions}));
  }

  onToggleAdmin() {
    this.setState(previousState => ({showAdminOptions: !previousState.showAdminOptions}));
  }

  redirectToZendesk() {
    window.open(supportUrls.helpCenter, '_blank');
  }

  openContactWidget() {
    openZendeskWidget(
      this.props.profile.givenName,
      this.props.profile.familyName,
      this.props.profile.username,
      this.props.profile.contactEmail,
    );
  }

  signOut() {
    signInStore.getValue().signOut();
    navigateSignOut();
  }

  render() {
    const {profile} = this.props;
    return <div style={styles.sideNav}>
      <SideNavItem
        hasProfileImage={true}
        content={`${profile.givenName} ${profile.familyName}`}
        parentOnClick={() => this.onToggleUser()}
        onToggleSideNav={() => this.props.onToggleSideNav()}
        containsSubItems={true}
        ref={this.state.userRef}
      />
      {
        this.state.showUserOptions && <SideNavItem
          content={'Profile'}
          onToggleSideNav={() => this.props.onToggleSideNav()}
          href='/profile'
          active={profileActive()}
        />
      }
      {
        this.state.showUserOptions && <SideNavItem
          content={'Sign Out'}
          onToggleSideNav={() => this.props.onToggleSideNav()}
          parentOnClick={() => this.signOut()}
        />
      }
      <SideNavItem
        icon='home'
        content='Home'
        onToggleSideNav={() => this.props.onToggleSideNav()}
        href='/'
        active={homeActive()}
      />
      <SideNavItem
        icon='applications'
        content='Your Workspaces'
        onToggleSideNav={() => this.props.onToggleSideNav()}
        href={'/workspaces'}
        active={workspacesActive()}
        disabled={!hasRegisteredAccess(profile.accessTierShortNames)}
      />
      <SideNavItem
        icon='star'
        content='Featured Workspaces'
        onToggleSideNav={() => this.props.onToggleSideNav()}
        href={'/library'}
        active={libraryActive()}
        disabled={!hasRegisteredAccess(profile.accessTierShortNames)}
      />
      <SideNavItem
        icon='help'
        content={'User Support Hub'}
        onToggleSideNav={() => this.props.onToggleSideNav()}
        parentOnClick={() => this.redirectToZendesk()}
        disabled={!hasRegisteredAccess(profile.accessTierShortNames)}
      />
      <SideNavItem
        icon='envelope'
        content={'Contact Us'}
        onToggleSideNav={() => this.props.onToggleSideNav()}
        parentOnClick={() => this.openContactWidget()}
      />
      {hasAuthorityForAction(profile, AuthorityGuardedAction.SHOW_ADMIN_MENU) && <SideNavItem
                icon='user'
                content='Admin'
                parentOnClick={() => this.onToggleAdmin()}
                onToggleSideNav={() => this.props.onToggleSideNav()}
                containsSubItems={true}
                ref={this.state.adminRef}
        />
      }
      {
        hasAuthorityForAction(profile, AuthorityGuardedAction.USER_ADMIN) && this.state.showAdminOptions && <SideNavItem
          content={'User Admin'}
          onToggleSideNav={() => this.props.onToggleSideNav()}
          href={'/admin/user'}
          active={userAdminActive()}
        />
      }
      {
        hasAuthorityForAction(profile, AuthorityGuardedAction.USER_AUDIT) && this.state.showAdminOptions && <SideNavItem
            content={'User Audit'}
            onToggleSideNav={() => this.props.onToggleSideNav()}
            href={'/admin/user-audit/'}
            active={userAuditActive()}
        />
      }
      {
        hasAuthorityForAction(profile, AuthorityGuardedAction.SERVICE_BANNER) && this.state.showAdminOptions && <SideNavItem
            content={'Service Banners'}
            onToggleSideNav={() => this.props.onToggleSideNav()}
            href={'/admin/banner'}
            active={bannerAdminActive()}
        />
      }
      {
        hasAuthorityForAction(profile, AuthorityGuardedAction.WORKSPACE_ADMIN) && this.state.showAdminOptions && <SideNavItem
            content={'Workspaces'}
            onToggleSideNav={() => this.props.onToggleSideNav()}
            href={'admin/workspaces'}
            active={workspaceAdminActive()}
        />
      }
      {
        hasAuthorityForAction(profile, AuthorityGuardedAction.WORKSPACE_AUDIT) && this.state.showAdminOptions && <SideNavItem
            content={'Workspace Audit'}
            onToggleSideNav={() => this.props.onToggleSideNav()}
            href={'/admin/workspace-audit/'}
            active={workspaceAuditActive()}
        />
      }
      {
        hasAuthorityForAction(profile, AuthorityGuardedAction.INSTITUTION_ADMIN) && this.state.showAdminOptions && <SideNavItem
            content={'Institution Admin'}
            onToggleSideNav={() => this.props.onToggleSideNav()}
            href={'admin/institution'}
            active={workspaceAdminActive()}
        />
      }
    </div>;
  }
};
