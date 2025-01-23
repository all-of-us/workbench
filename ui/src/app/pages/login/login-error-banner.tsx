import * as React from 'react';

import { ErrorMessage } from 'app/components/messages';
import { AoU } from 'app/components/text-wrappers';

export const LOGIN_ERROR_BANNER = () => (
  <div
    style={{
      fontWeight: 800,
      fontSize: 'large',
      padding: '1rem',
      lineHeight: '2rem',
    }}
  >
    <ErrorMessage>
      <>
        <div>
          Some of <AoU /> Researcher Workbench's supporting services will be
          down for maintenance on Sunday (January 26th) from 10-11AM ET.
        </div>
        <div>
          Users will not be able to access Cromwell, Jupyter, RStudio, or SAS
          during that time, but all ongoing jobs will not be interrupted.
        </div>
      </>
    </ErrorMessage>
  </div>
);
