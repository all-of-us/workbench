import * as React from 'react';

import jupyterLogo from 'assets/images/jupyter.png';
import rStudioLogo from 'assets/images/RStudio.png';

import { UIAppType } from './utils';

export const AppLogo = (props: { appType: UIAppType }) => {
  const logos = new Map([
    [UIAppType.JUPYTER, jupyterLogo],
    [UIAppType.RSTUDIO, rStudioLogo],
  ]);

  return (
    <img
      src={logos.get(props.appType)}
      alt={props.appType.toString()}
      style={{ marginRight: '1em' }}
    />
  );
};
