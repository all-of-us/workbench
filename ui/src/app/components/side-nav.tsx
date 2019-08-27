import * as React from 'react';
import colors, {colorWithWhiteness} from "app/styles/colors";
import {reactStyles} from "app/utils";
import {Clickable, MenuItem} from "app/components/buttons";
import {ClrIcon} from "./icons";
import {navigate} from "../utils/navigation";

const styles = reactStyles({
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
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    margin: 0,
    padding: '1rem',
    textAlign: 'left',
    textTransform: 'none',
    height: '2rem',
    color: colors.white,
  },
  navIcon: {
    marginRight: 8
  },
  dropdownIcon: {
    transform: 'rotate(180deg)',
  }
});

interface SideNavSubItemProps {
  text: string,
}

class SideNavSubItem extends React.Component<SideNavSubItemProps> {
  constructor(props) {
    super(props)
  }

  render() {
    return <Clickable
      // data-test-id is the text within the SideNavItem, with whitespace removed
      // and appended with '-menu-item'
      data-test-id={this.props.text.toString().replace(/\s/g, '') + '-menu-sub-item'}
      style={styles.navLink}
      // hover={backgroundColor: colorWithWhiteness(colors.accent, .10)}
    >
      {this.props.text}
    </Clickable>
  }
}

interface SideNavItemProps {
  icon: string,
  content: string,
  onToggleSideNav: Function,
  href?: string,
  subItems?: Array<any>
}

interface SideNavItemState {
  dropdownOpen: boolean;
}

class SideNavItem extends React.Component<SideNavItemProps, SideNavItemState> {
  constructor(props) {
    super(props);
    this.state = {
      dropdownOpen: false
    }
  }

  onClick() {
    if (this.props.href) {
      this.props.onToggleSideNav();
      navigate([this.props.href]);
    }
    else {
      this.setState(previousState => ({dropdownOpen: !previousState.dropdownOpen}));
    }
  }

  render() {
    return <Clickable
      // data-test-id is the text within the SideNavItem, with whitespace removed
      // and appended with '-menu-item'
      data-test-id={this.props.content.toString().replace(/\s/g, '') + '-menu-item'}
      style={styles.navLink}
      onClick={(previousState) => this.onClick()}
    >
      <span>
        <ClrIcon
          shape={this.props.icon}
          className={"is-solid"}
          style={styles.navIcon}
          size={21}
        />
        {this.props.content}
      </span>
      {
        this.props.subItems != undefined
        && this.props.subItems.length > 0
        && <ClrIcon
          shape="angle"
          style={styles.dropdownIcon}
          size={21}
        />
      }
      {
        this.props.subItems != undefined
        && this.props.subItems.length > 0
        && this.state.dropdownOpen
        && <React.Fragment>
          {this.props.subItems.map(subItem => (
            <SideNavSubItem text={subItem.text} />
          ))}
        </React.Fragment>
      }
    </Clickable>
  }
}

export interface Props {
  givenName: string;
  familyName: string;
  onToggleSideNav: Function;
}

export class SideNav extends React.Component<Props, {}> {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <div style={styles.sideNav}>
      <section>
        <SideNavItem
          icon="user"
          content={`${this.props.givenName} ${this.props.familyName}`}
          onToggleSideNav={this.props.onToggleSideNav}
          subItems={[
            {text: "Profile"},
            {text: "Billing Dashboard"},
            {text: "Sign Out"},
          ]}
        />
        <SideNavItem icon="home" content="Home" onToggleSideNav={this.props.onToggleSideNav} href="/"/>
        <SideNavItem icon="applications" content="Your Workspaces" onToggleSideNav={this.props.onToggleSideNav} href={"/workspaces"}/>
        <SideNavItem icon="star" content="Featured Workspaces" onToggleSideNav={this.props.onToggleSideNav}/>
        <SideNavItem
          icon="help"
          content="User Support"
          onToggleSideNav={this.props.onToggleSideNav}
          subItems={[
            {text: "How-to Guides"},
            {text: "User Forum"},
            {text: "Contact Us"},
          ]}
        />
      </section>
    </div>
  }
}
