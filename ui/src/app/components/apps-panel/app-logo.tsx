import * as React from 'react';
import { CSSProperties } from 'react';

import { appAssets, UIAppType } from './utils';

export const AppLogo = (props: {
  appType: UIAppType;
  style: CSSProperties;
}) => {
  const { logo } = appAssets.find((aa) => aa.appType === props.appType);
  return (
    <img
      src={logo}
      alt={props.appType}
      aria-label={props.appType}
      style={props.style}
    />
  );
};
