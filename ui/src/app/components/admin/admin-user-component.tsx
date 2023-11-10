import React, { useEffect, useState } from 'react';

import { adminGetProfile } from 'app/pages/admin/admin-egress-audit';

import { AdminUserLink } from './admin-user-link';

export const AdminUserComponent = ({ username, userWithDomain }) => {
  const [userDisabled, setUserDisabled] = useState(false);
  const [sourceUserEmail, setSourceUserEmail] = useState('');

  const handleGetProfile = async (usernameWithDomain) => {
    try {
      const userProfile = await adminGetProfile(usernameWithDomain);
      setUserDisabled(userProfile.disabled);
      setSourceUserEmail(userProfile.username);
    } catch (error) {
      console.error('Error fetching user profile: ', error);
    }
  };

  useEffect(() => {
    handleGetProfile(userWithDomain);
  }, []);

  return (
    <div>
      {userDisabled ? (
        <div>
          <span style={{ color: 'red' }}>(Disabled) </span>
          <AdminUserLink {...{ username }}>{sourceUserEmail}</AdminUserLink>
        </div>
      ) : (
        <AdminUserLink {...{ username }}>{sourceUserEmail}</AdminUserLink>
      )}
    </div>
  );
};
