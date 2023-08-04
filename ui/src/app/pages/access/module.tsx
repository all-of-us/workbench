import * as React from 'react';
import { ReactNode, useState } from 'react';

import { AccessModule, AccessModuleStatus, Profile } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
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

const ModuleBox = (props: {
  clickable: boolean;
  action: Function;
  children;
}) => {
  const { clickable, action, children } = props;
  return clickable ? (
    <Clickable onClick={() => action()}>
      <FlexRow style={styles.clickableModuleBox}>{children}</FlexRow>
    </Clickable>
  ) : (
    <FlexRow style={styles.backgroundModuleBox}>{children}</FlexRow>
  );
};

export const Module = (props: {
  focused: boolean;
  children?: ReactNode;
  clickable: boolean;
  eligible: boolean;
  moduleAction: Function;
  moduleName: AccessModule;
  profile: Profile;
  spinnerProps: WithSpinnerOverlayProps;
  status: AccessModuleStatus;
  style?;
}) => {
  const {
    focused,
    children,
    clickable,
    eligible,
    moduleAction,
    moduleName,
    profile,
    spinnerProps,
    status,
    style,
  } = props;
  const { showSpinner } = spinnerProps;
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

  const { DARTitleComponent, refreshAction, isEnabledInEnvironment } =
    getAccessModuleConfig(moduleName);

  return isEnabledInEnvironment ? (
    <FlexRow data-test-id={`module-${moduleName}`} {...{ style }}>
      <FlexRow style={styles.moduleCTA}>
        {cond(
          [
            (clickable || moduleName === AccessModule.ERACOMMONS) &&
              showRefresh &&
              !!refreshAction,
            () => <Refresh {...{ refreshAction, showSpinner }} />,
          ],
          [focused, () => <Next />]
        )}
      </FlexRow>
      <ModuleBox
        {...{ clickable }}
        action={() => {
          setShowRefresh(true);
          moduleAction();
        }}
      >
        <ModuleIcon
          {...{ moduleName, eligible }}
          completedOrBypassed={isCompliant(status)}
        />
        <FlexColumn style={{ flex: 1 }}>
          <div
            data-test-id={`module-${moduleName}-${
              clickable ? 'clickable' : 'unclickable'
            }-text`}
            style={{
              ...(clickable
                ? styles.clickableModuleText
                : styles.backgroundModuleText),
              ...{ fontWeight: 500 },
            }}
          >
            <DARTitleComponent
              {...{ profile }}
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
