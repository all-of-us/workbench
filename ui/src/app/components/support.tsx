import * as React from 'react';

import { getCustomOrDefaultUrl } from 'app/utils/urls';

import { Button, StyledExternalLink } from './buttons';

export const SUPPORT_EMAIL = 'support@researchallofus.org';

export const SupportMailto = ({ label = SUPPORT_EMAIL, style = {} }) => (
  <StyledExternalLink style={style} href={`mailto:${SUPPORT_EMAIL}`}>
    {label}
  </StyledExternalLink>
);

const handleClickSupportButton = (url) => () => {
  const adjustedUrl = getCustomOrDefaultUrl(url, `mailto:${SUPPORT_EMAIL}`);
  window.open(adjustedUrl);
};
export const SupportButton = ({
  label = SUPPORT_EMAIL,
  url = '',
  style = {},
}) => (
  <Button style={style} onClick={handleClickSupportButton(url)}>
    {label}
  </Button>
);
