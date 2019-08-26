import * as React from 'react';
import colors from "app/styles/colors";
import {reactStyles} from "app/utils";
import {MenuItem} from "app/components/buttons";

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
    margin: 0,
    padding: '0.5rem 1.5rem',
    textAlign: 'left',
    textTransform: 'none',
    height: '2rem',
    color: colors.white,
  }
});

const SideNavMenuItem = (icon, content) => {
  return <MenuItem icon={icon} solid={true} style={styles.navLink}>
    {{content}}
  </MenuItem>
};

export interface Props {
  givenName: string;
  familyName: string;
}

export class SideNav extends React.Component<Props, {}> {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    // return <div style={styles.sideNav}>
    //   <section>
    //     <SideNavMenuItem icon="user" content={`${this.props.givenName} ${this.props.familyName}`}/>
    //     <SideNavMenuItem icon="home" content="Home"/>
    //     <SideNavMenuItem icon="applications" content="Your Workspaces"/>
    //     <SideNavMenuItem icon="star" content="Featured Workspaces"/>
    //     <SideNavMenuItem icon="help" content="User Support"/>
    //   </section>
    // </div>
    return <div style={styles.sideNav}>
      <section>
        <MenuItem icon="user">{this.props.givenName} {this.props.familyName}</MenuItem>
        <MenuItem icon="home">Home</MenuItem>
        <MenuItem icon="applications">Your Workspaces</MenuItem>
        <MenuItem icon="star">Featured Workspaces</MenuItem>
        <MenuItem icon="help">User Support</MenuItem>
      </section>
    </div>
  }
}
