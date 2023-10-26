import * as React from 'react';

import { PersistentDiskRequest, Runtime } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button, LinkButton } from 'app/components/buttons';
import { DeletePersistentDiskButton } from 'app/components/common-env-conf-panels/delete-persistent-disk-button';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { AnalysisConfig, PanelContent } from 'app/utils/runtime-utils';

export interface CustomizePanelFooterProps {
  disableControls: boolean;
  runtimeCanBeCreated: boolean;
  runtimeCanBeUpdated: boolean;
  runtimeExists: boolean;
  unattachedPdExists: boolean;
  analysisConfig: AnalysisConfig;
  currentRuntime: Runtime;
  existingAnalysisConfig: AnalysisConfig;
  gcePersistentDisk: PersistentDiskRequest;
  onClose: () => void;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  setPanelContent: (pc: PanelContent) => void;
}
export const CustomizePanelFooter = ({
  disableControls,
  runtimeCanBeCreated,
  runtimeCanBeUpdated,
  runtimeExists,
  unattachedPdExists,
  analysisConfig,
  currentRuntime,
  existingAnalysisConfig,
  gcePersistentDisk,
  onClose,
  requestAnalysisConfig,
  setPanelContent,
}: CustomizePanelFooterProps) => {
  const unattachedDiskNeedsRecreate =
    unattachedPdExists &&
    analysisConfig.diskConfig.detachable &&
    (gcePersistentDisk.size > analysisConfig.diskConfig.size ||
      gcePersistentDisk.diskType !== analysisConfig.diskConfig.detachableType);

  return unattachedPdExists ? (
    <FlexRow
      style={{
        justifyContent: 'space-between',
        marginTop: '1.125rem',
      }}
    >
      <DeletePersistentDiskButton
        onClick={() => setPanelContent(PanelContent.DeleteUnattachedPd)}
      />
      {unattachedDiskNeedsRecreate ? (
        <Button
          aria-label='Next'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            setPanelContent(PanelContent.DeleteUnattachedPdAndCreate);
          }}
        >
          Next
        </Button>
      ) : (
        <Button
          aria-label='Create'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          Create
        </Button>
      )}
    </FlexRow>
  ) : (
    <FlexRow
      style={{
        justifyContent: 'space-between',
        marginTop: '1.125rem',
      }}
    >
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
      {cond<React.ReactNode>(
        [
          runtimeExists,
          () => (
            <Button
              aria-label='Next'
              disabled={!runtimeCanBeUpdated}
              onClick={() => {
                if (
                  existingAnalysisConfig.diskConfig.detachable &&
                  !analysisConfig.diskConfig.detachable
                ) {
                  setPanelContent(PanelContent.ConfirmUpdateWithDiskDelete);
                } else {
                  setPanelContent(PanelContent.ConfirmUpdate);
                }
              }}
            >
              Next
            </Button>
          ),
        ],
        [
          unattachedDiskNeedsRecreate,
          () => (
            <Button
              aria-label='Next'
              disabled={!runtimeCanBeCreated}
              onClick={() => {
                setPanelContent(PanelContent.DeleteUnattachedPdAndCreate);
              }}
            >
              Next
            </Button>
          ),
        ],
        [
          currentRuntime?.errors && currentRuntime.errors.length > 0,
          () => (
            <Button
              aria-label='Try Again'
              disabled={!runtimeCanBeCreated}
              onClick={() => {
                requestAnalysisConfig(analysisConfig);
                onClose();
              }}
            >
              Try Again
            </Button>
          ),
        ],
        () => (
          <Button
            aria-label='Create'
            disabled={!runtimeCanBeCreated}
            onClick={() => {
              requestAnalysisConfig(analysisConfig);
              onClose();
            }}
          >
            Create
          </Button>
        )
      )}
    </FlexRow>
  );
};
