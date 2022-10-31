import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { RuntimeSummary } from 'app/components/runtime-summary';
import { ComputeType } from 'app/utils/machines';
import { runtimePresets } from 'app/utils/runtime-presets';
import { PanelContent } from 'app/utils/runtime-utils';

import { CostInfo } from './cost-info';
import { StartStopRuntimeButton } from './start-stop-runtime-button';

export const CreatePanel = ({
  creatorFreeCreditsRemaining,
  profile,
  setPanelContent,
  workspace,
  analysisConfig,
}) => {
  const displayName =
    analysisConfig.computeType === ComputeType.Dataproc
      ? runtimePresets.hailAnalysis.displayName
      : runtimePresets.generalAnalysis.displayName;

  return (
    <div data-test-id='runtime-create-panel' style={styles.controlSection}>
      <FlexRow style={styles.costPredictorWrapper}>
        <StartStopRuntimeButton
          workspaceNamespace={workspace.namespace}
          googleProject={workspace.googleProject}
        />
        <CostInfo
          runtimeChanged={false}
          analysisConfig={analysisConfig}
          currentUser={profile.username}
          workspace={workspace}
          creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
        />
      </FlexRow>
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
