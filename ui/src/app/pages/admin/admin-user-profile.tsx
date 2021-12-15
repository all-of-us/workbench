import React, {useEffect} from 'react';

import {styles} from './admin-user-common';
import {FadeBox} from 'app/components/containers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);
  return <FadeBox style={styles.fadeBox}>temp</FadeBox>;
}
