import * as React from 'react';

import { appAssets, UIAppType } from './utils';

export const AppLogo = (props: { appType: UIAppType }) => {
  const { logo } = appAssets.find((aa) => aa.appType === props.appType);
  return (
    <img
      src={logo}
      alt={props.appType.toString()}
      style={{ marginRight: '1em' }}
    />
  );
};
