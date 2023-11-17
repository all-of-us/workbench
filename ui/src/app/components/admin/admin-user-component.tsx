import React, { useEffect, useState } from 'react';

import { AdminUserLink } from './admin-user-link';
import { Profile } from "generated/fetch";
import { userAdminApi } from "app/services/swagger-fetch-clients";

export const adminGetProfile = async (
  usernameWithDomain: string
): Promise<Profile> => {
  return userAdminApi().getUserByUsername(usernameWithDomain);
};

export function getUsernameWithoutDomain(sourceUserEmail: string) {
  const [username] = sourceUserEmail.split('@');
  return username;
}

export const AdminUserComponent = ({ userWithDomain }) => {
  const [userDisabled, setUserDisabled] = useState(false);
  const [sourceUserEmail, setSourceUserEmail] = useState('');

  const username = getUsernameWithoutDomain(userWithDomain);

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
