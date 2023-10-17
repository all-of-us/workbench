import * as React from 'react';

import { Runtime } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { LinkButton } from 'app/components/buttons';
import { DeletePersistentDiskButton } from 'app/components/common-env-conf-panels/delete-persistent-disk-button';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { AnalysisConfig, PanelContent } from 'app/utils/runtime-utils';

import { CreateButton } from './create-button';
import { NextUpdateButton } from './next-update-button';
import { NextWithDiskDeleteButton } from './next-with-disk-delete-button';
import { TryAgainButton } from './try-again-button';

interface Props {
  analysisConfig: AnalysisConfig;
  currentRuntime: Runtime;
  disableControls: boolean;
  existingAnalysisConfig: AnalysisConfig;
  onClose: () => void;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  runtimeCanBeUpdated: boolean;
  runtimeExists: boolean;
  setPanelContent: (pc: PanelContent) => void;
  unattachedDiskNeedsRecreate: boolean;
  unattachedPdExists: boolean;
}
export const CustomizePanelFooter = ({
  analysisConfig,
  currentRuntime,
  disableControls,
  existingAnalysisConfig,
  onClose,
  requestAnalysisConfig,
  runtimeCanBeCreated,
  runtimeCanBeUpdated,
  runtimeExists,
  setPanelContent,
  unattachedDiskNeedsRecreate,
  unattachedPdExists,
}: Props) => (
  <FlexRow
    style={{
      justifyContent: 'space-between',
      marginTop: '1.125rem',
    }}
  >
    {unattachedPdExists ? (
      <DeletePersistentDiskButton
        onClick={() => setPanelContent(PanelContent.DeleteUnattachedPd)}
      />
    ) : (
      // TODO split this out to a new DeleteEnvironmentButton
      <LinkButton
        style={{
          ...styles.deleteLink,
          ...(disableControls || !runtimeExists
            ? { color: colorWithWhiteness(colors.dark, 0.4) }
            : {}),
        }}
        aria-label='Delete Environment'
        disabled={disableControls || !runtimeExists}
        onClick={() => setPanelContent(PanelContent.DeleteRuntime)}
      >
        Delete Environment
      </LinkButton>
    )}
    {cond<JSX.Element>(
      [
        unattachedDiskNeedsRecreate,
        () => (
          <NextWithDiskDeleteButton
            {...{ runtimeCanBeCreated, setPanelContent }}
          />
        ),
      ],
      [
        runtimeExists,
        () => (
          <NextUpdateButton
            {...{
              runtimeCanBeUpdated,
              setPanelContent,
            }}
            updateYieldsUnusedDisk={
              existingAnalysisConfig.diskConfig.detachable &&
              !analysisConfig.diskConfig.detachable
            }
          />
        ),
      ],
      [
        currentRuntime?.errors && currentRuntime.errors.length > 0,
        () => (
          <TryAgainButton
            {...{
              runtimeCanBeCreated,
              analysisConfig,
              requestAnalysisConfig,
              onClose,
            }}
          />
        ),
      ],
      () => (
        <CreateButton
          {...{
            analysisConfig,
            requestAnalysisConfig,
            runtimeCanBeCreated,
            onClose,
          }}
        />
      )
    )}
  </FlexRow>
);
