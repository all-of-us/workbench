import * as React from 'react';
import { useEffect, useState } from 'react';

import { AppType, Disk, UserAppEnvironment } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { Spinner } from 'app/components/spinners';
import { appsApi, disksApi } from 'app/services/swagger-fetch-clients';
import { notificationStore } from 'app/utils/stores';
import { deleteUserApp, findDisk } from 'app/utils/user-apps-utils';

import { findApp, toUIAppType } from './apps-panel/utils';
import { ConfirmDelete } from './common-env-conf-panels/confirm-delete';
import { ConfirmDeleteEnvironmentWithPD } from './common-env-conf-panels/confirm-delete-environment-with-pd';
import { ConfirmDeleteUnattachedPD } from './common-env-conf-panels/confirm-delete-unattached-pd';
import { SASPanel } from './gke-app-configuration-panels/create-sas';
import { CromwellPanel } from './gke-app-configuration-panels/cromwell-panel';
import {
  CommonGKEAppPanelProps,
  GKEAppConfigPanelMainProps,
} from './gke-app-configuration-panels/gke-app-config-panel-main';
import { RStudioPanel } from './gke-app-configuration-panels/rstudio-panel';

type WrapperInjectedProps =
  | 'app'
  | 'disk'
  | 'onClickDeleteGkeApp'
  | 'onClickDeleteUnattachedPersistentDisk';

export type GKEAppConfigPanelWrapperProps = {
  appType: AppType;
  workspaceNamespace: string;
  onClose: () => void;
  initialPanelContent: GKEAppPanelContent | null;
} & Omit<GKEAppConfigPanelMainProps, WrapperInjectedProps>;

export enum GKEAppPanelContent {
  CREATE,
  DELETE_UNATTACHED_PD,
  DELETE_GKE_APP,
}

export const GKEAppConfigPanelWrapper = ({
  appType,
  workspaceNamespace,
  onClose,
  initialPanelContent,
  ...props
}: GKEAppConfigPanelWrapperProps) => {
  const [gkeAppsInWorkspace, setGkeAppsInWorkspace] = useState<
    UserAppEnvironment[] | undefined
  >();
  const [listAppsInWorkspaceError, setListAppsInWorkspaceError] =
    useState<Error>();

  const [ownedDisksInWorkspace, setOwnedDisksInWorkspace] = useState<
    Disk[] | undefined
  >();
  const [listOwnedDisksInWorkspaceError, setListOwnedDisksInWorkspaceError] =
    useState<Error>();

  const [panelContent, setPanelContent] = useState<GKEAppPanelContent>(
    initialPanelContent ?? GKEAppPanelContent.CREATE
  );

  useEffect(() => {
    appsApi()
      .listAppsInWorkspace(workspaceNamespace)
      .then(setGkeAppsInWorkspace)
      .catch((e) => {
        setListAppsInWorkspaceError(e);
        notificationStore.set({
          title: 'Unable to load applications',
          message:
            'An error occurred trying to load your applications. Please try again.',
        });
      });

    disksApi()
      .listOwnedDisksInWorkspace(workspaceNamespace)
      .then(setOwnedDisksInWorkspace)
      .catch((e) => {
        setListOwnedDisksInWorkspaceError(e);
        notificationStore.set({
          title: 'Unable to load persistent disks',
          message:
            'An error occurred trying to load your persistent disks. Please try again.',
        });
      });
  }, []);

  if (listAppsInWorkspaceError || listOwnedDisksInWorkspaceError) {
    return null;
  }

  // loading
  if (gkeAppsInWorkspace === undefined || ownedDisksInWorkspace === undefined) {
    return <Spinner />;
  }

  const app = findApp(gkeAppsInWorkspace, toUIAppType[appType]);
  const disk = findDisk(ownedDisksInWorkspace, appType);

  const onClickDeleteGkeApp = () =>
    setPanelContent(GKEAppPanelContent.DELETE_GKE_APP);

  const onConfirmDeleteGKEApp = async (deletePDSelected) => {
    await deleteUserApp(workspaceNamespace, app.appName, deletePDSelected);
    onClose();
  };

  const onClickDeleteUnattachedPersistentDisk = () => {
    setPanelContent(GKEAppPanelContent.DELETE_UNATTACHED_PD);
  };

  const onConfirmDeleteUnattachedPersistentDisk = async () => {
    try {
      await disksApi().deleteDisk(workspaceNamespace, disk.name);
      // Because we immediately close the panel after deleting the disk,
      // we don't need to update the list of disks. The next time the panel
      // is opened, the list of disks will be fetched again.
      onClose();
    } catch (e) {
      notificationStore.set({
        title: 'Unable to delete persistent disk',
        message:
          'An error occurred trying to delete your persistent disk. Please try again.',
      });
    }
  };

  const onCancelDeleteUnattachedPersistentDisk = () => {
    setPanelContent(GKEAppPanelContent.CREATE);
  };

  const commonProps: CommonGKEAppPanelProps = {
    ...props,
    app,
    disk,
    onClose,
    onClickDeleteGkeApp,
    onClickDeleteUnattachedPersistentDisk,
  };
  return switchCase(
    panelContent,
    [
      GKEAppPanelContent.CREATE,
      () =>
        switchCase(
          appType,
          [AppType.CROMWELL, () => <CromwellPanel {...commonProps} />],
          [AppType.RSTUDIO, () => <RStudioPanel {...commonProps} />],
          [AppType.SAS, () => <SASPanel {...commonProps} />]
        ),
    ],
    [
      GKEAppPanelContent.DELETE_UNATTACHED_PD,
      () => (
        <ConfirmDeleteUnattachedPD
          appType={toUIAppType[appType]}
          onConfirm={onConfirmDeleteUnattachedPersistentDisk}
          onCancel={onCancelDeleteUnattachedPersistentDisk}
        />
      ),
    ],
    [
      GKEAppPanelContent.DELETE_GKE_APP,
      () =>
        disk ? (
          <ConfirmDeleteEnvironmentWithPD
            onConfirm={onConfirmDeleteGKEApp}
            onCancel={onClose}
            appType={toUIAppType[app.appType]}
            usingDataproc={false}
            disk={disk}
          />
        ) : (
          <ConfirmDelete
            onCancel={onClose}
            onConfirm={() =>
              onConfirmDeleteGKEApp(false /* deletePdSelected */)
            }
          />
        ),
    ]
  );
};
