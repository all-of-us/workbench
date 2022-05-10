import * as React from 'react';

import { StyledRouterLink } from 'app/components/buttons';
import { displayDateWithoutHours } from 'app/utils/dates';

import { styles } from './profile-styles';

interface Props {
  signedDate: number;
}
export const SignedDuccPanel = (props: Props) => (
  <div style={styles.panel} data-test-id='signed-ducc-panel'>
    <div style={styles.title}>Data User Code of Conduct</div>
    <hr style={{ ...styles.verticalLine }} />
    <div style={styles.panelBody}>
      <div>Signed On</div>
      <div>{displayDateWithoutHours(props.signedDate)}</div>
      <StyledRouterLink path='/signed-ducc'>
        View Signed Data User Code of Conduct
      </StyledRouterLink>
    </div>
  </div>
);
