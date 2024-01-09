import * as React from 'react';
import { useEffect, useState } from 'react';

import { BillingStatus, Workspace } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { Clickable, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  cromwellConfigIconId,
  rstudioConfigIconId,
  sasConfigIconId,
  SidebarIconId,
} from 'app/components/help-sidebar-icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import {
  runtimeStore,
  serverConfigStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';
import { BILLING_ACCOUNT_DISABLED_TOOLTIP } from 'app/utils/strings';
import { maybeStartPollingForUserApps } from 'app/utils/user-apps-utils';

import { AppBanner } from './apps-panel/app-banner';
import { ExpandedApp } from './apps-panel/expanded-app';
import { findApp, getAppsByDisplayGroup, UIAppType } from './apps-panel/utils';
import { TooltipTrigger } from './popups';

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
    justifyContent: 'center',
  },
  closeButton: { marginLeft: 'auto', alignSelf: 'center' },
});

const UnexpandedApp = (props: {
  appType: UIAppType;
  onClick: Function;
  disabled: boolean;
}) => {
  const { appType, disabled, onClick } = props;
  return (
    <Clickable
      {...{ disabled, onClick }}
      data-test-id={`${appType}-unexpanded`}
      propagateDataTestId
      style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
    >
      <FlexRow style={styles.availableApp}>
        <AppBanner
          {...{ appType }}
          style={{
            ...{ marginRight: '1em', padding: '1rem' },
            ...(disabled && { filter: 'grayscale(1)' }),
          }}
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
  onClickDeleteGkeApp: (sidebarIcon: SidebarIconId) => void;
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
    ...(config.enableSasGKEApp ? [UIAppType.SAS] : []),
    UIAppType.CROMWELL,
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

  return (
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
              disabled={workspace.billingStatus === BillingStatus.INACTIVE}
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
                disabled={workspace.billingStatus === BillingStatus.INACTIVE}
              />
            ) : (
              <TooltipTrigger
                disabled={workspace.billingStatus !== BillingStatus.INACTIVE}
                content={BILLING_ACCOUNT_DISABLED_TOOLTIP}
              >
                <div
                  style={{
                    marginLeft: '1rem',
                    marginBottom: '1rem',
                  }}
                >
                  <UnexpandedApp
                    appType={availableApp.appType}
                    key={availableApp.appType}
                    disabled={
                      workspace.billingStatus === BillingStatus.INACTIVE
                    }
                    onClick={() =>
                      switchCase(
                        availableApp.appType,
                        [
                          UIAppType.CROMWELL,
                          () =>
                            setSidebarActiveIconStore.next(
                              cromwellConfigIconId
                            ),
                        ],
                        [
                          UIAppType.RSTUDIO,
                          () =>
                            setSidebarActiveIconStore.next(rstudioConfigIconId),
                        ],
                        [
                          UIAppType.SAS,
                          () => setSidebarActiveIconStore.next(sasConfigIconId),
                        ],
                        () => addToExpandedApps(availableApp.appType)
                      )
                    }
                  />
                </div>
              </TooltipTrigger>
            )
          )}
        </FlexColumn>
      )}
    </div>
  );
};
