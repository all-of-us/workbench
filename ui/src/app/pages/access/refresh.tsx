import * as React from 'react';

import { Button } from 'app/components/buttons';
import { Repeat } from 'app/components/icons';

import { styles } from './data-access-requirements';

export const Refresh = (props: {
  showSpinner: Function;
  refreshAction: Function;
}) => {
  const { showSpinner, refreshAction } = props;
  return (
    <Button
      type='primary'
      style={styles.refreshButton}
      onClick={async () => {
        showSpinner();
        await refreshAction();
        location.reload(); // also hides spinner
      }}
    >
      <Repeat style={styles.refreshIcon} /> Refresh
    </Button>
  );
};
