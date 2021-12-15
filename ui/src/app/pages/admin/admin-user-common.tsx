// this is a temporary file to assist with the migration from (class-based) AdminUser to (functional) AdminUserProfile
// for RW-7536

import * as React from 'react';
import {CSSProperties} from 'react';
import {Link} from 'react-router-dom';

import {reactStyles} from 'app/utils';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {Profile} from 'generated/fetch';
import {serverConfigStore} from 'app/utils/stores';
import {userAdminApi} from 'app/services/swagger-fetch-clients';
import {ClrIcon} from 'app/components/icons';

export const styles = reactStyles({
  semiBold: {
    fontWeight: 600
  },
  backgroundColorDark: {
    backgroundColor: colorWithWhiteness(colors.primary, .95)
  },
  textInput: {
    width: '17.5rem',
    opacity: '100%',
  },
  textInputContainer: {
    marginTop: '1rem'
  },
  fadeBox: {
    margin: 'auto',
    paddingTop: '1rem',
    width: '96.25%',
    minWidth: '1232px',
    color: colors.primary
  },
  header: {
    color: colors.primary,
    fontSize: '18px',
    fontWeight: 600,
    padding: '1em',
  },
  auditLink: {
    color: colors.accent,
    fontSize: '16px',
    fontWeight: 500,
  },
});

export const adminGetProfile = async(usernameWithoutGsuiteDomain: string): Promise<Profile> => {
  const {gsuiteDomain} = serverConfigStore.get().config;
  return userAdminApi().getUserByUsername(usernameWithoutGsuiteDomain + '@' + gsuiteDomain);
}

export const UserAdminTableLink = () => <Link to='/admin/users'>
  <ClrIcon
    shape='arrow'
    size={37}
    style={{
      backgroundColor: colorWithWhiteness(colors.accent, .85),
      color: colors.accent,
      borderRadius: '18px',
      transform: 'rotate(270deg)'
    }}
  />
</Link>;

export const UserAuditLink = (props: {usernameWithoutDomain: string, style?: CSSProperties, children}) => <Link
  style={props.style}
  to={`/admin/user-audit/${props.usernameWithoutDomain}`}
  target='_blank'
>
  {props.children}
</Link>
