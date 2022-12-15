import * as React from 'react';

import { AccessModule } from 'generated/fetch';

import { CircleCheck, CircleMinus } from 'app/components/icons';
import colors from 'app/styles/colors';
import { cond } from 'app/utils';

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
            <CircleMinus
              data-test-id={`module-${moduleName}-ineligible`}
              style={{ color: colors.disabled }}
            />
          ),
        ],
        [
          eligible && completedOrBypassed,
          () => (
            <CircleCheck
              data-test-id={`module-${moduleName}-complete`}
              style={{ color: colors.success }}
            />
          ),
        ],
        [
          eligible && !completedOrBypassed,
          () => (
            <CircleCheck
              data-test-id={`module-${moduleName}-incomplete`}
              style={{ color: colors.disabled }}
            />
          ),
        ]
      )}
    </div>
  );
};
