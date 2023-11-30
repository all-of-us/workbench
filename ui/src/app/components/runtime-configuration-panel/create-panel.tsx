import * as React from 'react';

import { Profile, RuntimeStatus, Workspace } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { EnvironmentInformedActionPanel } from 'app/components/common-env-conf-panels/environment-informed-action-panel';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexRow } from 'app/components/flex';
import { RuntimeSummary } from 'app/components/runtime-summary';
import { ComputeType } from 'app/utils/machines';
import { runtimePresets } from 'app/utils/runtime-presets';
import { AnalysisConfig, PanelContent } from 'app/utils/runtime-utils';

export interface CreatePanelProps {
  analysisConfig: AnalysisConfig;
  creatorFreeCreditsRemaining: number;
  onClose: () => void;
  profile: Profile;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  runtimeStatus: RuntimeStatus;
  setPanelContent: (panelContent: PanelContent) => void;
  workspace: Workspace;
}
export const CreatePanel = ({
  analysisConfig,
  creatorFreeCreditsRemaining,
  onClose,
  profile,
  requestAnalysisConfig,
  runtimeCanBeCreated,
  runtimeStatus,
  setPanelContent,
  workspace,
}: CreatePanelProps) => {
  const displayName =
    analysisConfig.computeType === ComputeType.Dataproc
      ? runtimePresets.hailAnalysis.displayName
      : runtimePresets.generalAnalysis.displayName;

  return (
    <>
      <div data-test-id='runtime-create-panel' style={styles.controlSection}>
        <EnvironmentInformedActionPanel
          {...{
            creatorFreeCreditsRemaining,
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
      </FlexRow>
    </>
  );
};
