import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {AuthorityGuardedAction, hasAuthorityForAction} from 'app/utils/authorities';
import {navigateSignOut, useNavigation} from 'app/utils/navigation';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {useState} from 'react';
import {signOut} from "../utils/authentication";
import {getProfilePictureSrc} from "../utils/profile-picture";

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

// TODO RW-7006: Ideally, we would use useLocation to get the path and pass it in to these functions.
// However, this component is currently rendered outside of the React router, so useLocation won't work.
const bannerAdminActive = () =>  {
  return window.location.pathname === '/admin/banner';
};

const userAdminActive = () =>  {
  return window.location.pathname.startsWith('/admin/user');
};

const userAuditActive = () =>  {
  return window.location.pathname.startsWith('/admin/user-audit');
};

const workspaceAdminActive = () =>  {
  return window.location.pathname.startsWith('/admin/workspaces');
};

const workspaceAuditActive = () =>  {
  return window.location.pathname.startsWith('/admin/workspace-audit');
};

const homeActive = () =>  {
  return window.location.pathname === '/';
};

const libraryActive = () =>  {
  return window.location.pathname === '/library';
};

const workspacesActive = () =>  {
  return window.location.pathname === '/workspaces';
};

const profileActive = () =>  {
  return window.location.pathname === '/profile';
};

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
  const [navigate, ] = useNavigation();

  const iconSize = 21;

  const onClick = () => {
    if (props.href && !props.disabled) {
      props.onToggleSideNav();
      navigate([props.href]);
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
    <div
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
  </Clickable>;
};

export interface SideNavProps {
  profile: Profile;
  onToggleSideNav: Function;
}

export const SideNav = (props: SideNavProps) => {
  const [showAdminOptions, setShowAdminOptions] = useState(false);
  const [showUserOptions, setShowUserOptions] = useState(false);

  const onToggleAdmin = () => setShowAdminOptions(!showAdminOptions);

  const onToggleUser = () => setShowUserOptions(!showUserOptions);

  const {profile, onToggleSideNav} = props;

  const openContactWidget = () => {
    openZendeskWidget(
      profile.givenName,
      profile.familyName,
      profile.username,
      profile.contactEmail
    );
  };

  return <div style={styles.sideNav}>
    <SideNavItem
        hasProfileImage={true}
        content={`${profile.givenName} ${profile.familyName}`}
        parentOnClick={() => onToggleUser()}
        onToggleSideNav={() => onToggleSideNav()}
        containsSubItems={true}
    />
    {
      showUserOptions && <SideNavItem
          content={'Profile'}
          onToggleSideNav={() => onToggleSideNav()}
          href='/profile'
          active={profileActive()}
      />
    }
    {
      showUserOptions && <SideNavItem
          content={'Sign Out'}
          onToggleSideNav={() => onToggleSideNav()}
          parentOnClick={() => {
            signOut();
            navigateSignOut();
          }}
      />
    }
    <SideNavItem
        icon='home'
        content='Home'
        onToggleSideNav={() => onToggleSideNav()}
        href='/'
        active={homeActive()}
    />
    <SideNavItem
        icon='applications'
        content='Your Workspaces'
        onToggleSideNav={() => onToggleSideNav()}
        href={'/workspaces'}
        active={workspacesActive()}
        disabled={!hasRegisteredAccess(profile.accessTierShortNames)}
    />
    <SideNavItem
        icon='star'
        content='Featured Workspaces'
        onToggleSideNav={() => onToggleSideNav()}
        href={'/library'}
        active={libraryActive()}
        disabled={!hasRegisteredAccess(profile.accessTierShortNames)}
    />
    <SideNavItem
        icon='help'
        content={'User Support Hub'}
        onToggleSideNav={() => onToggleSideNav()}
        parentOnClick={() => window.open(supportUrls.helpCenter, '_blank')}
        disabled={!hasRegisteredAccess(profile.accessTierShortNames)}
    />
    <SideNavItem
        icon='envelope'
        content={'Contact Us'}
        onToggleSideNav={() => onToggleSideNav()}
        parentOnClick={() => openContactWidget()}
    />
    {hasAuthorityForAction(profile, AuthorityGuardedAction.SHOW_ADMIN_MENU) && <SideNavItem
        icon='user'
        content='Admin'
        parentOnClick={() => onToggleAdmin()}
        onToggleSideNav={() => onToggleSideNav()}
        containsSubItems={true}
    />
    }
    {
      hasAuthorityForAction(profile, AuthorityGuardedAction.USER_ADMIN) && showAdminOptions && <SideNavItem
          content={'User Admin'}
          onToggleSideNav={() => onToggleSideNav()}
          href={'/admin/user'}
          active={userAdminActive()}
      />
    }
    {
      hasAuthorityForAction(profile, AuthorityGuardedAction.USER_AUDIT) && showAdminOptions && <SideNavItem
          content={'User Audit'}
          onToggleSideNav={() => onToggleSideNav()}
          href={'/admin/user-audit/'}
          active={userAuditActive()}
      />
    }
    {
      hasAuthorityForAction(profile, AuthorityGuardedAction.SERVICE_BANNER) && showAdminOptions && <SideNavItem
          content={'Service Banners'}
          onToggleSideNav={() => onToggleSideNav()}
          href={'/admin/banner'}
          active={bannerAdminActive()}
      />
    }
    {
      hasAuthorityForAction(profile, AuthorityGuardedAction.WORKSPACE_ADMIN) && showAdminOptions && <SideNavItem
          content={'Workspaces'}
          onToggleSideNav={() => onToggleSideNav()}
          href={'admin/workspaces'}
          active={workspaceAdminActive()}
      />
    }
    {
      hasAuthorityForAction(profile, AuthorityGuardedAction.WORKSPACE_AUDIT) && showAdminOptions && <SideNavItem
          content={'Workspace Audit'}
          onToggleSideNav={() => onToggleSideNav()}
          href={'/admin/workspace-audit/'}
          active={workspaceAuditActive()}
      />
    }
    {
      hasAuthorityForAction(profile, AuthorityGuardedAction.INSTITUTION_ADMIN) && showAdminOptions && <SideNavItem
          content={'Institution Admin'}
          onToggleSideNav={() => onToggleSideNav()}
          href={'admin/institution'}
          active={workspaceAdminActive()}
      />
    }
  </div>;
};
