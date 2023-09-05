import * as React from 'react';

import { AccessModule } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { CheckCircle, MinusCircle } from 'app/components/icons';
import colors from 'app/styles/colors';

import { styles } from './data-access-requirements';

export const ModuleIcon = (props: {
  moduleName: AccessModule;
  completedOrBypassed: boolean;
  eligible?: boolean;
}) => {
  const { moduleName, completedOrBypassed, eligible = true } = props;

  return (
    <div style={styles.moduleIcon}>
      {cond(
        [
          !eligible,
          () => (
            <MinusCircle
              data-test-id={`module-${moduleName}-ineligible`}
              style={{ color: colors.disabled }}
            />
          ),
        ],
        [
          eligible && completedOrBypassed,
          () => (
            <CheckCircle
              data-test-id={`module-${moduleName}-complete`}
              style={{ color: colors.success }}
            />
          ),
        ],
        [
          eligible && !completedOrBypassed,
          () => (
            <CheckCircle
              data-test-id={`module-${moduleName}-incomplete`}
              style={{ color: colors.disabled }}
            />
          ),
        ]
      )}
    </div>
  );
};
