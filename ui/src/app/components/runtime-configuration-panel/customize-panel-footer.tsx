import * as React from 'react';

import { PersistentDiskRequest, Runtime } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button, LinkButton } from 'app/components/buttons';
import { DeletePersistentDiskButton } from 'app/components/common-env-conf-panels/delete-persistent-disk-button';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { AnalysisConfig } from 'app/utils/analysis-config';
import { canDeleteRuntime } from 'app/utils/runtime-utils';

import { PanelContent } from './utils';

export interface CustomizePanelFooterProps {
  disableControls: boolean;
  runtimeCanBeCreated: boolean;
  runtimeCannotBeCreatedExplanation?: string;
  runtimeCanBeUpdated: boolean;
  runtimeCannotBeUpdatedExplanation?: string;
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
  runtimeCannotBeCreatedExplanation,
  runtimeCanBeUpdated,
  runtimeCannotBeUpdatedExplanation,
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
          ...(disableControls || !canDeleteRuntime(currentRuntime?.status)
            ? { color: colorWithWhiteness(colors.dark, 0.4) }
            : {}),
        }}
        aria-label='Delete Environment'
        disabled={disableControls || !canDeleteRuntime(currentRuntime?.status)}
        onClick={() => setPanelContent(PanelContent.DeleteRuntime)}
      >
        Delete Environment
      </LinkButton>
      {cond<React.ReactNode>(
        [
          runtimeExists,
          () => (
            <TooltipTrigger
              disabled={runtimeCanBeUpdated}
              content={runtimeCannotBeUpdatedExplanation}
            >
              <div>
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
              </div>
            </TooltipTrigger>
          ),
        ],
        [
          currentRuntime?.errors &&
            currentRuntime.errors.length > 0 &&
            runtimeCanBeCreated,
          () => (
            <Button
              aria-label='Try Again'
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
          <TooltipTrigger
            disabled={runtimeCanBeCreated}
            content={runtimeCannotBeCreatedExplanation}
          >
            <div>
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
            </div>
          </TooltipTrigger>
        )
      )}
    </FlexRow>
  );
};
