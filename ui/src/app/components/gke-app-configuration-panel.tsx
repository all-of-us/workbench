import * as React from 'react';
import { useEffect, useState } from 'react';

import { AppType, Disk, UserAppEnvironment } from 'generated/fetch';

import {
  CromwellConfigurationPanel,
  CromwellConfigurationPanelProps,
} from 'app/components/cromwell-configuration-panel';
import {
  RStudioConfigurationPanel,
  RStudioConfigurationPanelProps,
} from 'app/components/rstudio-configuration-panel';
import { ConfirmDeleteUnattachedPD } from 'app/components/runtime-configuration-panel/confirm-delete-unattached-pd';
import { Spinner } from 'app/components/spinners';
import { appsApi, disksApi } from 'app/services/swagger-fetch-clients';
import { switchCase } from 'app/utils';
import { notificationStore } from 'app/utils/stores';
import { findDisk } from 'app/utils/user-apps-utils';

type InjectedProps =
  | 'gkeAppsInWorkspace'
  | 'disk'
  | 'onClickDeleteUnattachedPersistentDisk';

export type GkeAppConfigurationPanelProps = {
  type: AppType;
  workspaceNamespace: string;
  onClose: () => void;
} & Omit<CreateGKEAppPanelProps, InjectedProps>;

export enum GKEAppPanelContent {
  CREATE,
  DELETE_UNATTACHED_PD,
}

type CreateGKEAppPanelProps = {
  type: AppType;
} & CromwellConfigurationPanelProps &
  RStudioConfigurationPanelProps;

const CreateGKEAppPanel = ({ type, ...props }: CreateGKEAppPanelProps) =>
  switchCase(
    type,
    [AppType.CROMWELL, () => <CromwellConfigurationPanel {...props} />],
    [AppType.RSTUDIO, () => <RStudioConfigurationPanel {...props} />]
  );

export const GKEAppConfigurationPanel = ({
  type,
  workspaceNamespace,
  onClose,
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
    GKEAppPanelContent.CREATE
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

  const disk = findDisk(ownedDisksInWorkspace, type);

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

  return switchCase(
    panelContent,
    [
      GKEAppPanelContent.CREATE,
      () => (
        <CreateGKEAppPanel
          {...{
            ...props,
            type,
            gkeAppsInWorkspace,
            disk,
            onClickDeleteUnattachedPersistentDisk,
            onClose,
          }}
        />
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
    ]
  );
};
