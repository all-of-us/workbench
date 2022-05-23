import * as React from 'react';
import * as fp from 'lodash/fp';

import { withProfileErrorModal } from 'app/components/with-error-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';

const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    return <div>Cheese</div>;
  }
);

export default DemographicSurvey;
