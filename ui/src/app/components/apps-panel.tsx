import * as React from 'react';
import { useState } from 'react';

import { BillingStatus, Workspace } from 'generated/fetch';

import { Clickable, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { DisabledPanel } from 'app/components/runtime-configuration-panel/disabled-panel';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { isVisible } from 'app/utils/runtime-utils';
import { runtimeStore, useStore } from 'app/utils/stores';

import { AppLogo } from './apps-panel/app-logo';
import { ExpandedApp } from './apps-panel/expanded-app';
import { UIAppType } from './apps-panel/utils';

const styles = reactStyles({
  header: {
    color: colors.primary,
    height: 19,
    fontFamily: 'Montserrat',
    fontSize: 16,
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '19px',
    margin: '1em',
  },
  availableApp: {
    background: colors.white,
    marginLeft: '1em',
    marginBottom: '1em',
    justifyContent: 'center',
  },
  closeButton: { marginLeft: 'auto', alignSelf: 'center' },
});

const AvailableApp = (props: { appType: UIAppType; onClick: Function }) => {
  const { appType, onClick } = props;
  return (
    <Clickable {...{ onClick }}>
      <FlexRow style={styles.availableApp}>
        <AppLogo {...{ appType }} />
      </FlexRow>
    </Clickable>
  );
};

export const AppsPanel = (props: {
  workspace: Workspace;
  onClose: Function;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}) => {
  const { onClose } = props;
  const { runtime } = useStore(runtimeStore);

  // in display order
  const appsToDisplay = [UIAppType.JUPYTER, UIAppType.RSTUDIO];

  const appStates = [
    { appType: UIAppType.JUPYTER, expand: isVisible(runtime?.status) },
    // RStudio is not implemented yet, so we don't expand it
    { appType: UIAppType.RSTUDIO, expand: false },
  ];

  // which app(s) have the user explicitly expanded by clicking?
  const [userExpandedApps, setUserExpandedApps] = useState([]);
  const addToExpandedApps = (appType: UIAppType) =>
    setUserExpandedApps([...userExpandedApps, appType]);

  const showExpanded = (appType: UIAppType): boolean =>
    userExpandedApps.includes(appType) ||
    appStates.find((s) => s.appType === appType)?.expand;

  const showInActiveSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    showExpanded(appType) &&
    !userExpandedApps.includes(appType);
  const showActiveSection = appStates
    .map((s) => s.appType)
    .some(showInActiveSection);

  const showInAvailableSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    (userExpandedApps.includes(appType) || !showExpanded(appType));
  const showAvailableSection = appStates
    .map((s) => s.appType)
    .some(showInAvailableSection);

  const ActiveApps = () => (
    <FlexColumn>
      <FlexRow>
        <h3 style={styles.header}>Active applications</h3>
        {showActiveSection && (
          <CloseButton {...{ onClose }} style={styles.closeButton} />
        )}
      </FlexRow>
      {appsToDisplay.map((appType) => {
        return showInActiveSection(appType) ? (
          <ExpandedApp {...{ ...props, appType }} key={appType} />
        ) : null;
      })}
    </FlexColumn>
  );

  const AvailableApps = () => (
    <FlexColumn>
      <FlexRow>
        <h3 style={styles.header}>Launch other applications</h3>
        {!showActiveSection && (
          <CloseButton {...{ onClose }} style={styles.closeButton} />
        )}
      </FlexRow>
      {appsToDisplay.map((appType) => {
        return showInAvailableSection(appType) ? (
          showExpanded(appType) ? (
            <ExpandedApp {...{ ...props, appType }} key={appType} />
          ) : (
            <AvailableApp
              {...{ appType }}
              key={appType}
              onClick={() => addToExpandedApps(appType)}
            />
          )
        ) : null;
      })}
    </FlexColumn>
  );

  return props.workspace.billingStatus === BillingStatus.INACTIVE ? (
    <DisabledPanel />
  ) : (
    <div>
      {showActiveSection && <ActiveApps />}
      {showAvailableSection && <AvailableApps />}
    </div>
  );
};
