import * as React from 'react';

import { StyledExternalLink } from './buttons';

export const SUPPORT_EMAIL = 'support@researchallofus.org';

export const SupportMailto = ({ label = SUPPORT_EMAIL, style = {} }) => (
  <StyledExternalLink style={style} href={`mailto:${SUPPORT_EMAIL}`}>
    {label}
  </StyledExternalLink>
);
