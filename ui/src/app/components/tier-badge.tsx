import * as React from 'react';
import { CSSProperties } from 'react';

import { switchCase } from '@terra-ui-packages/core-utils';
import { AccessTierShortNames } from 'app/utils/access-tiers';

import { ControlledTierBadge, RegisteredTierBadge } from './icons';

const getTierBadge = (
  accessTierShortName: string,
  style: CSSProperties
): JSX.Element => {
  return switchCase(
    accessTierShortName,
    [
      AccessTierShortNames.Registered,
      () => <RegisteredTierBadge {...{ style }} />,
    ],
    [
      AccessTierShortNames.Controlled,
      () => <ControlledTierBadge {...{ style }} />,
    ]
  );
};

export const TierBadge = (props: {
  accessTierShortName: string;
  style?: CSSProperties;
}): JSX.Element => {
  const { accessTierShortName, style } = props;
  return getTierBadge(accessTierShortName, style);
};
