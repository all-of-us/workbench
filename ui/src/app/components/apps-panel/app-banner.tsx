import * as React from 'react';
import { CSSProperties } from 'react';

import { appAssets, UIAppType } from '../../utils/user-apps-utils';

export const AppBanner = (props: {
  appType: UIAppType;
  style: CSSProperties;
}) => {
  const { banner } = appAssets.find((aa) => aa.appType === props.appType);
  return (
    <img
      src={banner}
      alt={props.appType}
      aria-label={props.appType}
      style={props.style}
    />
  );
};
