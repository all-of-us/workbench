import * as React from 'react';
import colors from "app/styles/colors";
import {reactStyles} from "app/utils";
import {Clickable} from "app/components/buttons";
import {ClrIcon} from "./icons";
import {navigate} from "app/utils/navigation";
import {SignInService} from "app/services/sign-in.service"

const styles = reactStyles({
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
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    margin: 0,
    paddingLeft: '1rem',
    textAlign: 'left',
    textTransform: 'none',
    height: '2rem',
    color: colors.white,
  },
  sideNavItemActive: {
    backgroundColor: '#4356a7',
    fontWeight: 'bold',
  },
  sideNavItemHover: {
    backgroundColor: '#4356a7',
  },
  sideNavItemContent: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  navIcon: {
    marginRight: '8px'
  },
  spacer: {
    marginRight: '8px',
    width: '21px'
  },
  dropdownIcon: {
    transform: 'rotate(180deg)',
  }
});

interface SideNavItemProps {
  icon?: string,
  content: string,
  onClick?: Function,
  onToggleSideNav: Function,
  href?: string,
  containsSubItems?: boolean,
  active: boolean;
}

interface SideNavItemState {
  hovering: boolean;
}

class SideNavItem extends React.Component<SideNavItemProps, SideNavItemState> {
  constructor(props) {
    super(props);
    this.state = {
      hovering: false
    }
  }

  onClick() {
    if (this.props.href) {
      this.props.onToggleSideNav();
      navigate([this.props.href]);
    }
  }

  getStyles(active, hovering) {
    let sideNavItemStyles = {...styles.sideNavItem}
    if (active) {
      sideNavItemStyles = {...sideNavItemStyles, ...styles.sideNavItemActive}
    }
    if (hovering) {
      sideNavItemStyles = {...sideNavItemStyles, ...styles.sideNavItemHover}
    }
    return sideNavItemStyles;
  }

  render() {
    return <Clickable
      // data-test-id is the text within the SideNavItem, with whitespace removed
      // and appended with '-menu-item'
      data-test-id={this.props.content.toString().replace(/\s/g, '') + '-menu-item'}
      style={this.getStyles(this.props.active, this.state.hovering)}
      onClick={() => this.props.onClick ? this.props.onClick() : this.onClick()}
      onMouseEnter={() => this.setState({hovering: true})}
      onMouseLeave={() => this.setState({hovering: false})}
    >
      <div
        style={styles.sideNavItemContent}
      >
        <span
          style={this.props.icon ? {marginLeft: '0px'} : {marginLeft: '29px'}}
        >
          {
            this.props.icon && <ClrIcon
              shape={this.props.icon}
              className={"is-solid"}
              style={styles.navIcon}
              size={21}
            />
          }
          {this.props.content}
        </span>
        {
          this.props.containsSubItems
          && <ClrIcon
            shape="angle"
            style={styles.dropdownIcon}
            size={21}
          />
        }
      </div>
    </Clickable>
  }
}

export interface SideNavProps {
  homeActive: boolean;
  workspacesActive: boolean;
  libraryActive: boolean;
  givenName: string;
  familyName: string;
  onToggleSideNav: Function;
}

export interface SideNavState {
  showUserOptions: boolean;
  showHelpOptions: boolean;
}

export class SideNav extends React.Component<SideNavProps, SideNavState> {
  constructor(props) {
    super(props);
    this.state = {
      showUserOptions: false,
      showHelpOptions: false,
    };
  }

  onToggleUser() {
    this.setState({showHelpOptions: false});
    this.setState(previousState => ({showUserOptions: !previousState.showUserOptions}));
  }

  onToggleHelp() {
    this.setState({showUserOptions: false});
    this.setState(previousState => ({showHelpOptions: !previousState.showHelpOptions}));
  }

  render() {
    return <div style={styles.sideNav}>
      <SideNavItem
        icon="user"
        content={`${this.props.givenName} ${this.props.familyName}`}
        onClick={() => this.onToggleUser()}
        onToggleSideNav={this.props.onToggleSideNav}
        containsSubItems={true}
        active={false}
      />
      {
        this.state.showUserOptions && <SideNavItem
          content={"Profile"}
          onToggleSideNav={this.props.onToggleSideNav}
          href="/profile"
          active={false}
        />
      }
      {
        this.state.showUserOptions && <SideNavItem
          content={"Billing Dashboard"}
          onToggleSideNav={this.props.onToggleSideNav}
          active={false}
        />
      }
      {
        this.state.showUserOptions && <SideNavItem
          content={"Sign Out"}
          // signOut()
          onToggleSideNav={this.props.onToggleSideNav}
          active={false}
        />
      }
      <SideNavItem
        icon="home"
        content="Home"
        onToggleSideNav={this.props.onToggleSideNav}
        href="/"
        active={this.props.homeActive}
      />
      <SideNavItem
        icon="applications"
        content="Your Workspaces"
        onToggleSideNav={this.props.onToggleSideNav}
        href={"/workspaces"}
        active={this.props.workspacesActive}
      />
      <SideNavItem
        icon="star"
        content="Featured Workspaces"
        onToggleSideNav={this.props.onToggleSideNav}
        href={"/library"}
        active={this.props.libraryActive}
      />
      <SideNavItem
        icon="help"
        content="User Support"
        onClick={() => this.onToggleHelp()}
        onToggleSideNav={this.props.onToggleSideNav}
        containsSubItems={true}
        active={false}
      />
      {
        this.state.showHelpOptions && <SideNavItem
          content={"How-to Guides"}
          onToggleSideNav={this.props.onToggleSideNav}
          active={false}
        />
      }
      {
        this.state.showHelpOptions && <SideNavItem
          content={"User Forum"}
          onToggleSideNav={this.props.onToggleSideNav}
          // openHubForum()
          href={"https://aousupporthelp.zendesk.com/hc"}
          active={false}
        />
      }
      {
        this.state.showHelpOptions && <SideNavItem
          content={"Contact Us"}
          // openZendesk()
          onToggleSideNav={this.props.onToggleSideNav}
          active={false}
        />
      }
    </div>
  }
}
