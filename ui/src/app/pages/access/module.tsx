import * as React from 'react';
import { ReactNode, useState } from 'react';

import { AccessModule, AccessModuleStatus, Profile } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { ArrowRight } from 'app/components/icons';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { cond } from 'app/utils';
import {
  getAccessModuleConfig,
  getStatusText,
  isCompliant,
} from 'app/utils/access-utils';

import { styles } from './data-access-requirements';
import { ModuleBox } from './module-box';
import { ModuleIcon } from './module-icon';
import { Refresh } from './refresh';

const Next = () => (
  <FlexRow style={styles.nextElement}>
    <span data-test-id='next-module-cta' style={styles.nextText}>
      NEXT
    </span>{' '}
    <ArrowRight style={styles.nextIcon} />
  </FlexRow>
);

export const Module = (props: {
  active: boolean;
  children?: ReactNode;
  clickable: boolean;
  eligible: boolean;
  moduleAction: Function;
  moduleName: AccessModule;
  profile: Profile;
  spinnerProps: WithSpinnerOverlayProps;
  status: AccessModuleStatus;
}) => {
  const {
    active,
    children,
    clickable,
    eligible,
    moduleAction,
    moduleName,
    profile,
    spinnerProps,
    status,
  } = props;
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

  const { DARTitleComponent, refreshAction, isEnabledInEnvironment } =
    getAccessModuleConfig(moduleName);

  return isEnabledInEnvironment ? (
    <FlexRow data-test-id={`module-${moduleName}`}>
      <FlexRow style={styles.moduleCTA}>
        {cond(
          [
            clickable && showRefresh && !!refreshAction,
            () => (
              <Refresh
                refreshAction={refreshAction}
                showSpinner={spinnerProps.showSpinner}
              />
            ),
          ],
          [active, () => <Next />]
        )}
      </FlexRow>
      <ModuleBox
        clickable={clickable}
        action={() => {
          setShowRefresh(true);
          moduleAction();
        }}
      >
        <ModuleIcon
          moduleName={moduleName}
          eligible={eligible}
          completedOrBypassed={isCompliant(status)}
        />
        <FlexColumn>
          <div
            data-test-id={`module-${moduleName}-${
              clickable ? 'clickable' : 'unclickable'
            }-text`}
            style={
              clickable
                ? styles.clickableModuleText
                : styles.backgroundModuleText
            }
          >
            <DARTitleComponent
              profile={profile}
              afterInitialClick={showRefresh}
            />
          </div>
          {isCompliant(status) && (
            <div style={styles.moduleDate}>{getStatusText(status)}</div>
          )}
        </FlexColumn>
      </ModuleBox>
      {children}
    </FlexRow>
  ) : null;
};
