import * as React from 'react';

import { Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';

import { styles } from './data-access-requirements';

export const ModuleBox = (props: {
  clickable: boolean;
  action: Function;
  children;
}) => {
  const { clickable, action, children } = props;
  return clickable ? (
    <Clickable onClick={() => action()}>
      <FlexRow style={styles.clickableModuleBox}>{children}</FlexRow>
    </Clickable>
  ) : (
    <FlexRow style={styles.backgroundModuleBox}>{children}</FlexRow>
  );
};
