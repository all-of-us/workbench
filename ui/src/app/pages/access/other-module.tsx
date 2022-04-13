import * as React from 'react';
import { useState } from 'react';

import { AccessModule, AccessModuleStatus } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  getAccessModuleConfig,
  getStatusText,
  isCompliant,
  redirectToNiH,
} from 'app/utils/access-utils';

import { styles } from './data-access-requirements';
import { ModuleBox } from './module-box';
import { ModuleIcon } from './module-icon';
import { Refresh } from './refresh';

export const OtherModule = (props: {
  clickable: boolean;
  eligible: boolean;
  moduleName: AccessModule;
  spinnerProps: WithSpinnerOverlayProps;
  status: AccessModuleStatus;
}) => {
  const { clickable, eligible, moduleName, spinnerProps, status } = props;
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

  const { DARTitleComponent, refreshAction, isEnabledInEnvironment } =
    getAccessModuleConfig(moduleName);
  return isEnabledInEnvironment ? (
    <FlexRow data-test-id={`module-${moduleName}`}>
      <FlexRow style={styles.moduleCTA}>
        {showRefresh && refreshAction && (
          <Refresh
            refreshAction={refreshAction}
            showSpinner={spinnerProps.showSpinner}
          />
        )}
      </FlexRow>
      <ModuleBox
        clickable={clickable}
        action={() => {
          setShowRefresh(true);
          redirectToNiH();
        }}
      >
        <ModuleIcon
          moduleName={moduleName}
          eligible={eligible}
          completedOrBypassed={isCompliant(status)}
        />
        <FlexColumn>
          <div
            style={
              clickable
                ? styles.clickableModuleText
                : styles.backgroundModuleText
            }
          >
            <DARTitleComponent />
          </div>
          {isCompliant(status) && (
            <div style={styles.moduleDate}>{getStatusText(status)}</div>
          )}
        </FlexColumn>
      </ModuleBox>
    </FlexRow>
  ) : null;
};
