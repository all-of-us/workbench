import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';

import {adminGetProfile, UserAdminTableLink, UserAuditLink, styles} from './admin-user-common';
import {FadeBox} from 'app/components/containers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {MatchParams} from 'app/utils/stores';
import {CaretRight} from 'app/components/icons';
import {FlexRow} from 'app/components/flex';

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  const {usernameWithoutGsuiteDomain} = useParams<MatchParams>();
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    const onMount = async() => {
      setProfile(await adminGetProfile(usernameWithoutGsuiteDomain));
      spinnerProps.hideSpinner();
    }
    onMount();
  }, []);

  return <FadeBox style={styles.fadeBox}>
    <FlexRow style={{alignItems: 'center'}}>
      <UserAdminTableLink/>
      <span style={styles.header}>User Profile Information</span>
      <UserAuditLink style={styles.auditLink} usernameWithoutDomain={usernameWithoutGsuiteDomain}>AUDIT <CaretRight/></UserAuditLink>
    </FlexRow>

    <div>username is {usernameWithoutGsuiteDomain}</div>,
    <div>profile is {profile? JSON.stringify(profile) : 'not loaded'}</div>
  </FadeBox>;
}
