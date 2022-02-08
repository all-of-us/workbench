import * as React from 'react';
import { useEffect, useRef, useState } from 'react';

import { AccessRenewalNotificationMaybe } from 'app/pages/signed-in/access-renewal-notification';
import { Breadcrumb } from 'app/components/breadcrumb';
import { ClrIcon } from 'app/components/icons';
import { SideNav } from 'app/components/side-nav';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { profileStore, ProfileStore, useStore } from 'app/utils/stores';
import { environment } from 'environments/environment';
import logo from 'assets/images/all-of-us-logo.svg';
import { StatusAlertBannerMaybe } from 'app/components/status-alert-banner-maybe';
import { CTAvailableBannerMaybe } from 'app//components/ct-available-banner-maybe';

const styles = reactStyles({
  headerContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    boxShadow: '3px 0px 10px',
    paddingTop: '1rem',
    paddingBottom: '0.5rem',
    paddingRight: '30px',
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
    width: '155px',
    borderRadius: '2px',
    backgroundColor: colors.primary,
    color: colors.white,
    fontFamily: 'Montserrat',
    fontSize: '8px',
    lineHeight: '12px',
    textAlign: 'center',
  },
});

export interface Props {
  profileState: ProfileStore;
}

export interface State {
  sideNavVisible: boolean;
  statusAlertVisible: boolean;
  statusAlertDetails: {
    statusAlertId: number;
    title: string;
    message: string;
    link: string;
  };
  barsTransform: string;
  hovering: boolean;
  wrapperRef: React.RefObject<HTMLDivElement>;
}

const barsTransformNotRotated = 'rotate(0deg)';
const barsTransformRotated = 'rotate(90deg)';

export const NavBar = () => {
  const [showSideNav, setShowSideNav] = useState(false);
  const [barsTransform, setBarsTransform] = useState(barsTransformNotRotated);
  const [hovering, setHovering] = useState(false);
  const wrapperRef = useRef(null);
  const { profile } = useStore(profileStore);

  const onToggleSideNav = () => {
    setShowSideNav(!showSideNav);
    setBarsTransform(
      barsTransform === barsTransformNotRotated
        ? barsTransformRotated
        : barsTransformNotRotated
    );
  };

  const onClickOutside = (event) => {
    if (
      wrapperRef &&
      !wrapperRef.current.contains(event.target) &&
      showSideNav
    ) {
      onToggleSideNav();
    }
  };

  useEffect(() => {
    document.addEventListener('click', onClickOutside);

    return () => {
      document.removeEventListener('click', onClickOutside);
    };
  });

  return (
    <div style={styles.headerContainer} ref={wrapperRef}>
      <div
        style={{
          transform: barsTransform,
          display: 'inline-block',
          marginLeft: '1rem',
          transition: 'transform 0.5s',
        }}
      >
        <ClrIcon
          shape='bars'
          onClick={() => onToggleSideNav()}
          onMouseEnter={() => setHovering(true)}
          onMouseLeave={() => setHovering(false)}
          style={
            hovering
              ? { ...styles.sidenavIcon, ...styles.sidenavIconHovering }
              : { ...styles.sidenavIcon }
          }
        ></ClrIcon>
      </div>
      <div>
        <a href={'/'}>
          <img src={logo} style={styles.headerImage} />
        </a>
        {environment.shouldShowDisplayTag && (
          <div style={styles.displayTag}>{environment.displayTag}</div>
        )}
      </div>
      <Breadcrumb />
      <AccessRenewalNotificationMaybe />
      <StatusAlertBannerMaybe />
      <CTAvailableBannerMaybe />
      {showSideNav && (
        <SideNav
          profile={profile}
          // Passing the function itself deliberately, we want to be able to
          // toggle the nav whenever we click anything in it
          onToggleSideNav={onToggleSideNav}
        />
      )}
    </div>
  );
};
