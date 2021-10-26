import * as React from "react";

import {Button, StyledExternalLink} from "./buttons";

export const SUPPORT_EMAIL = 'support@researchallofus.org';

export const SupportMailto = ({label = SUPPORT_EMAIL, style = {}}) =>
  <StyledExternalLink style={style} href={`mailto:${SUPPORT_EMAIL}`}>{label}</StyledExternalLink>;

export const SupportButton = ({label = SUPPORT_EMAIL, style = {}}) =>
  <Button style={style} onClick={() => window.open(`mailto:${SUPPORT_EMAIL}`)}>{label}</Button>;
