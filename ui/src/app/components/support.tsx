import * as React from 'react';

import { canonicalizeUrl, isValidUrl } from 'app/utils/urls';

import { Button, StyledExternalLink } from './buttons';

export const SUPPORT_EMAIL = 'support@researchallofus.org';

export const SupportMailto = ({ label = SUPPORT_EMAIL, style = {} }) => (
  <StyledExternalLink style={style} href={`mailto:${SUPPORT_EMAIL}`}>
    {label}
  </StyledExternalLink>
);

const handleClickSupportButton = (url) => () => {
  if (url) {
    const adjustedUrl = canonicalizeUrl(url);
    if (isValidUrl(adjustedUrl)) {
      url = adjustedUrl;
    }
  }
  window.open(url ?? `mailto:${SUPPORT_EMAIL}`);
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
