import * as React from 'react';
import { useEffect, useState } from 'react';

import { BillingStatus, Workspace } from 'generated/fetch';

import { Clickable, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { DisabledPanel } from 'app/components/runtime-configuration-panel/disabled-panel';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import {
  runtimeStore,
  serverConfigStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';
import { maybeStartPollingForUserApps } from 'app/utils/user-apps-utils';

import { AppLogo } from './apps-panel/app-logo';
import { ExpandedApp } from './apps-panel/expanded-app';
import { findApp, getAppsByDisplayGroup, UIAppType } from './apps-panel/utils';

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
    <Clickable
      {...{ onClick }}
      data-test-id={`${appType}-unexpanded`}
      propagateDataTestId
    >
      <FlexRow style={styles.availableApp}>
        <AppLogo
          {...{ appType }}
          style={{ marginRight: '1em', padding: '1rem' }}
        />
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
  // all GKE apps (not Jupyter)
  const { userApps } = useStore(userAppsStore);

  // in display order
  const appsToDisplay = [
    UIAppType.JUPYTER,
    ...(config.enableRStudioGKEApp ? [UIAppType.RSTUDIO] : []),
    ...(config.enableCromwellGKEApp ? [UIAppType.CROMWELL] : []),
  ];

  useEffect(() => {
    maybeStartPollingForUserApps(workspace.namespace);
  }, []);

  const [activeApps, availableApps] = getAppsByDisplayGroup(
    runtime,
    userApps,
    appsToDisplay
  );

  // which app(s) have the user explicitly expanded by clicking?
  const [userExpandedApps, setUserExpandedApps] = useState([]);
  const addToExpandedApps = (appType: UIAppType) =>
    setUserExpandedApps([...userExpandedApps, appType]);
  const showActiveSection = activeApps.length > 0;

  return props.workspace.billingStatus === BillingStatus.INACTIVE ? (
    <DisabledPanel />
  ) : (
    <div data-test-id='apps-panel'>
      {activeApps.length > 0 && (
        <FlexColumn>
          <FlexRow>
            <h3 style={styles.header}>Active applications</h3>
            <CloseButton {...{ onClose }} style={styles.closeButton} />
          </FlexRow>
          {activeApps.map((activeApp) => (
            <ExpandedApp
              {...{ ...props }}
              appType={activeApp.appType}
              key={activeApp.appType}
              initialUserAppInfo={findApp(userApps, activeApp.appType)}
            />
          ))}
        </FlexColumn>
      )}
      {availableApps.length > 0 && (
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
          {availableApps.map((availableApp) =>
            userExpandedApps.includes(availableApp.appType) ? (
              <ExpandedApp
                {...{ ...props }}
                appType={availableApp.appType}
                key={availableApp.appType}
                initialUserAppInfo={findApp(userApps, availableApp.appType)}
              />
            ) : (
              <UnexpandedApp
                appType={availableApp.appType}
                key={availableApp.appType}
                onClick={() => {
                  if (availableApp.appType === UIAppType.CROMWELL) {
                    setSidebarActiveIconStore.next('cromwellConfig');
                  } else {
                    addToExpandedApps(availableApp.appType);
                  }
                }}
              />
            )
          )}
        </FlexColumn>
      )}
    </div>
  );
};
