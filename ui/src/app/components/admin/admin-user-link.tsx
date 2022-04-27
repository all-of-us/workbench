import * as React from 'react';

import { StyledRouterLink } from 'app/components/buttons';
import { usernameWithoutDomain } from 'app/utils';

export const AdminUserLink = ({ username, children, ...props }) => (
  <StyledRouterLink
    path={`/admin/users/${usernameWithoutDomain(username)}`}
    {...props}
  >
    {children}
  </StyledRouterLink>
);
