import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {navigate, navigateSignOut, signInStore} from 'app/utils/navigation';
import {openZendeskWidget} from 'app/utils/zendesk';
import {environment} from 'environments/environment';
import * as React from 'react';

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

class SideNavItem extends React.Component<SideNavItemProps, SideNavItemState> {
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
        if (this.props.parentOnClick) {
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
  homeActive: boolean;
  workspacesActive: boolean;
  libraryActive: boolean;
  profileActive: boolean;
  userAdminActive: boolean;
  hasDataAccess: boolean;
  hasAccessModuleAdmin: boolean;
  aouAccountEmailAddress: string;
  contactEmailAddress: string;
  givenName: string;
  familyName: string;
  onToggleSideNav: Function;
}

export interface SideNavState {
  showUserOptions: boolean;
  showHelpOptions: boolean;
  userRef: React.RefObject<SideNavItem>;
  helpRef: React.RefObject<SideNavItem>;

}

export class SideNav extends React.Component<SideNavProps, SideNavState> {
  constructor(props) {
    super(props);
    this.state = {
      showUserOptions: false,
      showHelpOptions: false,
      userRef: React.createRef(),
      helpRef: React.createRef(),
    };
  }

  onToggleUser() {
    this.setState({showHelpOptions: false});
    this.state.helpRef.current.closeSubItems();
    this.setState(previousState => ({showUserOptions: !previousState.showUserOptions}));
  }

  onToggleHelp() {
    this.setState({showUserOptions: false});
    this.state.userRef.current.closeSubItems();
    this.setState(previousState => ({showHelpOptions: !previousState.showHelpOptions}));
  }

  redirectToZendesk() {
    window.open(environment.zendeskHelpCenterUrl, '_blank');
  }

  openContactWidget() {
    openZendeskWidget(
      this.props.givenName,
      this.props.familyName,
      this.props.aouAccountEmailAddress,
      this.props.contactEmailAddress,
    );
  }

  signOut() {
    signInStore.getValue().signOut();
    navigateSignOut();
  }

  render() {
    return <div style={styles.sideNav}>
      <SideNavItem
        hasProfileImage={true}
        content={`${this.props.givenName} ${this.props.familyName}`}
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
          active={this.props.profileActive}
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
        active={this.props.homeActive}
      />
      <SideNavItem
        icon='applications'
        content='Your Workspaces'
        onToggleSideNav={() => this.props.onToggleSideNav()}
        href={'/workspaces'}
        active={this.props.workspacesActive}
        disabled={!this.props.hasDataAccess}
      />
      <SideNavItem
        icon='star'
        content='Featured Workspaces'
        onToggleSideNav={() => this.props.onToggleSideNav()}
        href={'/library'}
        active={this.props.libraryActive}
      />
      <SideNavItem
        icon='help'
        content='User Support'
        parentOnClick={() => this.onToggleHelp()}
        onToggleSideNav={() => this.props.onToggleSideNav()}
        containsSubItems={true}
        ref={this.state.helpRef}
      />
      {
        this.state.showHelpOptions && <SideNavItem
          content={'User Forum'}
          onToggleSideNav={() => this.props.onToggleSideNav()}
          parentOnClick={() => this.redirectToZendesk()}
        />
      }
      {
        this.state.showHelpOptions && <SideNavItem
          content={'Contact Us'}
          onToggleSideNav={() => this.props.onToggleSideNav()}
          parentOnClick={() => this.openContactWidget()}
        />
      }
      {
        this.props.hasAccessModuleAdmin && <SideNavItem
          content={'User Admin'}
          onToggleSideNav={() => this.props.onToggleSideNav}
          href={'/admin/user'}
          active={this.props.userAdminActive}
        />
      }
    </div>;
  }
}
