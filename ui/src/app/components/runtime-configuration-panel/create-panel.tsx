import * as React from 'react';

import { Profile, RuntimeStatus, Workspace } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeSummary } from 'app/components/runtime-summary';
import { EnvironmentInformedActionPanel } from 'app/lab/components/common-env-conf-panels/environment-informed-action-panel';
import { AnalysisConfig } from 'app/utils/analysis-config';
import { ComputeType } from 'app/utils/machines';
import { runtimePresets } from 'app/utils/runtime-presets';

import { PanelContent } from './utils';

export interface CreatePanelProps {
  analysisConfig: AnalysisConfig;
  creatorInitialCreditsRemaining: number;
  onClose: () => void;
  profile: Profile;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  runtimeCannotBeCreatedExplanation?: string;
  runtimeStatus: RuntimeStatus;
  setPanelContent: (panelContent: PanelContent) => void;
  workspace: Workspace;
}
export const CreatePanel = ({
  analysisConfig,
  creatorInitialCreditsRemaining,
  onClose,
  profile,
  requestAnalysisConfig,
  runtimeCanBeCreated,
  runtimeCannotBeCreatedExplanation,
  runtimeStatus,
  setPanelContent,
  workspace,
}: CreatePanelProps) => {
  const displayName =
    analysisConfig.computeType === ComputeType.Dataproc
      ? runtimePresets().hailAnalysis.displayName
      : runtimePresets().generalAnalysis.displayName;

  return (
    <>
      <div data-test-id='runtime-create-panel' style={styles.controlSection}>
        <EnvironmentInformedActionPanel
          {...{
            creatorInitialCreditsRemaining,
            profile,
            workspace,
            analysisConfig,
          }}
          status={runtimeStatus}
          appType={UIAppType.JUPYTER}
        />
        <FlexRow
          style={{ justifyContent: 'space-between', alignItems: 'center' }}
        >
          <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
            Recommended Environment for {displayName}
          </h3>
          <Button
            type='secondarySmall'
            onClick={() => setPanelContent(PanelContent.Customize)}
            aria-label='Customize'
          >
            Customize
          </Button>
        </FlexRow>
        <RuntimeSummary {...{ analysisConfig }} />
      </div>
      <FlexRow style={{ justifyContent: 'flex-end', marginTop: '1.5rem' }}>
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
      </FlexRow>
    </>
  );
};
