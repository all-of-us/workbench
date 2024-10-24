import * as React from 'react';
import { CSSProperties } from 'react';

import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  explanation: {
    color: colors.primary,
    fontSize: 12,
  },
});

const bypassedText =
  'Researchers affiliated with this institution are not subject to the expiration ' +
  'of their initial credits after the standard time period. They remain subject to the exhaustion ' +
  'of the dollar amount of their credits.';

const standardText =
  'Researchers affiliated with this institution are subject to the expiration ' +
  'of their initial credits after the standard time period (unless bypassed on an individual basis) as well as the exhaustion ' +
  'of the dollar amount of their credits.';

export const InstitutionExpirationBypassExplanation = (props: {
  bypassed: boolean;
  style?: CSSProperties;
}) => {
  return (
    <p style={{ ...styles.explanation, ...props.style }}>
      {props.bypassed ? bypassedText : standardText}
    </p>
  );
};
