import * as React from 'react';
import { useEffect, useState } from 'react';

import { AppType, Disk, UserAppEnvironment } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { Spinner } from 'app/components/spinners';
import { appsApi, disksApi } from 'app/services/swagger-fetch-clients';
import { notificationStore } from 'app/utils/stores';
import { deleteUserApp, findDisk } from 'app/utils/user-apps-utils';

import { findApp, toUIAppType } from './apps-panel/utils';
import { CreateCromwell } from './gke-app-configuration-panels/create-cromwell';
import { CreateGkeAppProps } from './gke-app-configuration-panels/create-gke-app';
import { CreateRStudio } from './gke-app-configuration-panels/create-rstudio';
import { ConfirmDelete } from './runtime-configuration-panel/confirm-delete';
import { ConfirmDeleteEnvironmentWithPD } from './runtime-configuration-panel/confirm-delete-environment-with-pd';
import { ConfirmDeleteUnattachedPD } from './runtime-configuration-panel/confirm-delete-unattached-pd';

type InjectedProps = 'app' | 'disk' | 'onClickDeleteUnattachedPersistentDisk';

export type GkeAppConfigurationPanelProps = {
  appType: AppType;
  workspaceNamespace: string;
  onClose: () => void;
  initialPanelContent: GKEAppPanelContent | null;
} & Omit<CreateGkeAppProps, InjectedProps>;

export enum GKEAppPanelContent {
  CREATE,
  DELETE_UNATTACHED_PD,
  DELETE_GKE_APP,
}

export const GKEAppConfigurationPanel = ({
  appType,
  workspaceNamespace,
  onClose,
  initialPanelContent,
  ...props
}: GkeAppConfigurationPanelProps) => {
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

  const onConfirmDeleteGKEApp = async (deletePDSelected) => {
    await deleteUserApp(workspaceNamespace, app.appName, deletePDSelected);
    onClose();
  };

  return switchCase(
    panelContent,
    [
      GKEAppPanelContent.CREATE,
      () =>
        switchCase(
          appType,
          [
            AppType.CROMWELL,
            () => (
              <CreateCromwell
                {...{
                  ...props,
                  app,
                  disk,
                  onClickDeleteUnattachedPersistentDisk,
                  onClose,
                }}
              />
            ),
          ],
          [
            AppType.RSTUDIO,
            () => (
              <CreateRStudio
                {...{
                  ...props,
                  app,
                  disk,
                  onClickDeleteUnattachedPersistentDisk,
                  onClose,
                }}
              />
            ),
          ]
        ),
    ],
    [
      GKEAppPanelContent.DELETE_UNATTACHED_PD,
      () => (
        <ConfirmDeleteUnattachedPD
          onConfirm={onConfirmDeleteUnattachedPersistentDisk}
          onCancel={onCancelDeleteUnattachedPersistentDisk}
        />
      ),
    ],
    [
      GKEAppPanelContent.DELETE_GKE_APP,
      () =>
        // currently (July 2023) we do not expose detaching PDs to users, but it's possible to get into
        // an error state where the disk is missing, so we need to account for this
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
