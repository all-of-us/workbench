import * as React from 'react';
import { useEffect, useState } from 'react';

import { BillingStatus, UserAppEnvironment, Workspace } from 'generated/fetch';

import { Clickable, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { DisabledPanel } from 'app/components/runtime-configuration-panel/disabled-panel';
import { appsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { isVisible } from 'app/utils/runtime-utils';
import { runtimeStore, serverConfigStore, useStore } from 'app/utils/stores';

import { AppLogo } from './apps-panel/app-logo';
import { ExpandedApp } from './apps-panel/expanded-app';
import { findApp, shouldShowApp, UIAppType } from './apps-panel/utils';

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
    <Clickable {...{ onClick }} data-test-id={`${appType}-unexpanded`}>
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
  const { onClose, workspace } = props;
  const { runtime } = useStore(runtimeStore);
  const { config } = useStore(serverConfigStore);

  // in display order
  const appsToDisplay = [
    UIAppType.JUPYTER,
    ...(config.enableRStudioGKEApp ? [UIAppType.RSTUDIO] : []),
    ...(config.enableCromwellGKEApp ? [UIAppType.CROMWELL] : []),
  ];

  // all GKE apps (not Jupyter)
  const [userApps, setUserApps] = useState<UserAppEnvironment[]>();
  useEffect(() => {
    appsApi().listAppsInWorkspace(workspace.namespace).then(setUserApps);
  }, []);

  const appStates = appsToDisplay.map((appType) => {
    return {
      appType,
      initializeAsExpanded:
        appType === UIAppType.JUPYTER
          ? isVisible(runtime?.status)
          : shouldShowApp(findApp(userApps, appType)),
    };
  });

  // which app(s) have the user explicitly expanded by clicking?
  const [userExpandedApps, setUserExpandedApps] = useState([]);
  const addToExpandedApps = (appType: UIAppType) =>
    setUserExpandedApps([...userExpandedApps, appType]);

  // show apps that have shouldExpand = true in the Active section
  // all will be shown in expanded mode
  const showInActiveSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    appStates.find((s) => s.appType === appType)?.initializeAsExpanded;
  const showActiveSection = appsToDisplay.some(showInActiveSection);

  // show apps that have shouldExpand = false in the Available section
  // by default they will be shown in Unexpanded mode
  // BUT some of these may be userExpandedApps, which are shown in Expanded mode
  const showInAvailableSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    !appStates.find((s) => s.appType === appType)?.initializeAsExpanded;
  const showAvailableSection = appsToDisplay.some(showInAvailableSection);

  return props.workspace.billingStatus === BillingStatus.INACTIVE ? (
    <DisabledPanel />
  ) : (
    <div>
      {showActiveSection && (
        <FlexColumn>
          <FlexRow>
            <h3 style={styles.header}>Active applications</h3>
            <CloseButton {...{ onClose }} style={styles.closeButton} />
          </FlexRow>
          {appsToDisplay.map(
            (appType) =>
              showInActiveSection(appType) && (
                <ExpandedApp
                  {...{ ...props, appType }}
                  key={appType}
                  initialUserAppInfo={findApp(userApps, appType)}
                />
              )
          )}
        </FlexColumn>
      )}
      {showAvailableSection && (
        <FlexColumn>
          <FlexRow>
            <h3 style={styles.header}>
              {showActiveSection
                ? 'Launch other applications'
                : 'Launch applications'}
            </h3>
            {
              // only show the close button in the Available section if there is no Active section
              !showActiveSection && (
                <CloseButton {...{ onClose }} style={styles.closeButton} />
              )
            }
          </FlexRow>
          {appsToDisplay.map(
            (appType) =>
              showInAvailableSection(appType) &&
              (userExpandedApps.includes(appType) ? (
                <ExpandedApp
                  {...{ ...props, appType }}
                  key={appType}
                  initialUserAppInfo={findApp(userApps, appType)}
                />
              ) : (
                <UnexpandedApp
                  {...{ appType }}
                  key={appType}
                  onClick={() => addToExpandedApps(appType)}
                />
              ))
          )}
        </FlexColumn>
      )}
    </div>
  );
};
