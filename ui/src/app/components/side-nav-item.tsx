import {useState} from "react";
import {Clickable} from "./buttons";
import {ClrIcon} from "./icons";
import {getProfilePictureSrc} from "app/utils/profile-picture";
import * as React from "react";
import {reactStyles} from "app/utils";
import colors, {colorWithWhiteness} from "app/styles/colors";
import {RouteLink} from "./app-router";

const styles = reactStyles({
  flex: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
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

const getSideNavItemStyles = (active, hovering, disabled) => {
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
};

const SideNavItemContents = ({subItemsOpen, ...props}) => {
  const iconSize = 21;

  return <div
      style={{...styles.flex,
        flex: '1 0 auto'
      }}
  >
        <span
            style={
              props.icon || props.hasProfileImage
                  ? {...styles.flex}
                  : {...styles.noIconMargin}
            }
        >
          {
            props.icon && <ClrIcon
                shape={props.icon}
                className={'is-solid'}
                style={styles.navIcon}
                size={iconSize}
            />
          }
          {
            props.hasProfileImage && <img
                src={getProfilePictureSrc()}
                style={styles.profileImage}
            />
          }
          {props.content}
        </span>
    {
      props.containsSubItems
      && <ClrIcon
          shape='angle'
          style={
            subItemsOpen
                ? {...styles.dropdownIcon, ...styles.dropdownIconOpen}
                : styles.dropdownIcon
          }
          size={iconSize}
      />
    }
  </div>
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

export const SideNavItem = (props: SideNavItemProps) => {
  const [hovering, setHovering] = useState(false);
  const [subItemsOpen, setSubItemsOpen] = useState(false);

  const onClick = () => {
    if (props.href && !props.disabled) {
      props.onToggleSideNav();
    }
    if (props.containsSubItems) {
      setSubItemsOpen(!subItemsOpen);
    }
  };

  return <Clickable
      // data-test-id is the text within the SideNavItem, with whitespace removed
      // and appended with '-menu-item'
      data-test-id={props.content.toString().replace(/\s/g, '') + '-menu-item'}
      style={getSideNavItemStyles(props.active, hovering, props.disabled)}
      onClick={() => {
        if (props.parentOnClick && !props.disabled) {
          props.parentOnClick();
        }
        onClick();
      }}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
  >
    {props.href && !props.disabled
        ? <RouteLink path={props.href} style={{color: colors.white}}>
          <SideNavItemContents subItemsOpen={subItemsOpen} {...props}/>
        </RouteLink>
        : <SideNavItemContents subItemsOpen={subItemsOpen} {...props}/>
    }
  </Clickable>;
};
