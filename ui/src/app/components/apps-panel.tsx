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

const UnexpandedApp = (props: { appType: UIAppType; onClick: Function }) => {
  const { appType, onClick } = props;
  return (
    <Clickable {...{ onClick }}>
      <FlexRow style={styles.availableApp}>
        <AppLogo {...{ appType }} style={{ marginRight: '1em' }} />
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
    {
      appType: UIAppType.JUPYTER,
      expandable: true,
      shouldExpandByDefault: isVisible(runtime?.status),
    },
    // RStudio is not implemented yet, so we don't expand it
    {
      appType: UIAppType.RSTUDIO,
      expandable: false,
      shouldExpandByDefault: false,
    },
  ];

  // which app(s) have the user explicitly expanded by clicking?
  const [userExpandedApps, setUserExpandedApps] = useState([]);
  const addToExpandedApps = (appType: UIAppType) =>
    setUserExpandedApps([...userExpandedApps, appType]);

  // show apps that have shouldExpand = true in the Active section
  // all will be shown in expanded mode
  const showInActiveSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    appStates.find((s) => s.appType === appType)?.shouldExpandByDefault;
  const showActiveSection = appsToDisplay.some(showInActiveSection);

  // show apps that have shouldExpand = false in the Available section
  // by default they will be shown in Unexpanded mode
  // BUT some of these may be userExpandedApps, which are shown in Expanded mode
  const showInAvailableSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    !appStates.find((s) => s.appType === appType)?.shouldExpandByDefault;
  const showAvailableSection = appsToDisplay.some(showInAvailableSection);

  const ActiveApps = (sectionProps: { showCloseButtonHere: boolean }) => (
    <FlexColumn>
      <FlexRow>
        <h3 style={styles.header}>Active applications</h3>
        {sectionProps.showCloseButtonHere && (
          <CloseButton {...{ onClose }} style={styles.closeButton} />
        )}
      </FlexRow>
      {appsToDisplay.map(
        (appType) =>
          showInActiveSection(appType) && (
            <ExpandedApp {...{ ...props, appType }} key={appType} />
          )
      )}
    </FlexColumn>
  );

  const AvailableApps = (sectionProps: { showCloseButtonHere: boolean }) => (
    <FlexColumn>
      <FlexRow>
        <h3 style={styles.header}>Launch other applications</h3>
        {sectionProps.showCloseButtonHere && (
          <CloseButton {...{ onClose }} style={styles.closeButton} />
        )}
      </FlexRow>
      {appsToDisplay.map(
        (appType) =>
          showInAvailableSection(appType) &&
          (userExpandedApps.includes(appType) ? (
            <ExpandedApp {...{ ...props, appType }} key={appType} />
          ) : (
            <UnexpandedApp
              {...{ appType }}
              key={appType}
              onClick={() =>
                appStates.find((s) => s.appType === appType)?.expandable &&
                addToExpandedApps(appType)
              }
            />
          ))
      )}
    </FlexColumn>
  );

  return props.workspace.billingStatus === BillingStatus.INACTIVE ? (
    <DisabledPanel />
  ) : (
    <div>
      {showActiveSection && (
        <ActiveApps showCloseButtonHere={showActiveSection} />
      )}
      {showAvailableSection && (
        <AvailableApps showCloseButtonHere={!showActiveSection} />
      )}
    </div>
  );
};
