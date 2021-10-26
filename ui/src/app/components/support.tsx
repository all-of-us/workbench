import * as React from "react";

import {StyledExternalLink} from "./buttons";

export const SUPPORT_EMAIL = 'support@researchallofus.org';

export const SupportMailto = () => <StyledExternalLink href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</StyledExternalLink>;
