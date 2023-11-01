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
import {
  AnalysisConfig,
  PanelContent,
  RuntimeStatusRequest,
} from 'app/utils/runtime-utils';

export interface CreatePanelProps {
  profile: Profile;
  setPanelContent: (panelContent: PanelContent) => void;
  workspace: Workspace;
  analysisConfig: AnalysisConfig;
  creatorFreeCreditsRemaining: number;
  runtimeStatus: RuntimeStatus;
  setRuntimeStatusRequest: (runtimeStatusRequest: RuntimeStatusRequest) => void;
}
export const CreatePanel = ({
  profile,
  setPanelContent,
  workspace,
  analysisConfig,
  creatorFreeCreditsRemaining,
  runtimeStatus,
  setRuntimeStatusRequest,
}: CreatePanelProps) => {
  const displayName =
    analysisConfig.computeType === ComputeType.Dataproc
      ? runtimePresets.hailAnalysis.displayName
      : runtimePresets.generalAnalysis.displayName;

  return (
    <div data-test-id='runtime-create-panel' style={styles.controlSection}>
      <EnvironmentInformedActionPanel
        {...{
          creatorFreeCreditsRemaining,
          profile,
          workspace,
          analysisConfig,
        }}
        status={runtimeStatus}
        onPause={() => setRuntimeStatusRequest(RuntimeStatusRequest.Stop)}
        onResume={() => setRuntimeStatusRequest(RuntimeStatusRequest.Start)}
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
      <RuntimeSummary analysisConfig={analysisConfig} />
    </div>
  );
};
