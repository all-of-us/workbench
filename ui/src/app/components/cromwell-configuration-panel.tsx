import * as React from 'react';
import * as fp from 'lodash/fp';

import { AppType, Runtime, RuntimeStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningMessage } from 'app/components/messages';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { Spinner } from 'app/components/spinners';
import {
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import {
  ComputeType,
  validLeoDataprocMasterMachineTypes,
  validLeoGceMachineTypes,
} from 'app/utils/machines';
import { applyPresetOverride } from 'app/utils/runtime-presets';
import {
  AnalysisDiff,
  diffsToUpdateMessaging,
  getAnalysisConfigDiffs,
  isVisible,
  maybeWithExistingDisk,
  toAnalysisConfig,
  UpdateMessaging,
  useCustomRuntime,
  useRuntimeStatus,
  withAnalysisConfigDefaults,
} from 'app/utils/runtime-utils';
import { diskStore, runtimeStore, useStore } from 'app/utils/stores';

import { CostPredictor } from './cost-predictor';

const { useState, useEffect } = React;

const PanelMain = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withUserProfile()
)(
  ({
    cdrVersionTiersResponse,
    workspace,
    profileState,
    creatorFreeCreditsRemaining,
  }) => {
    const { profile } = profileState;
    const { namespace, cdrVersionId, googleProject } = workspace;

    const { hasWgsData: allowDataproc } = findCdrVersion(
      cdrVersionId,
      cdrVersionTiersResponse
    ) || { hasWgsData: false };

    const { gcePersistentDisk } = useStore(diskStore);
    let [{ currentRuntime, pendingRuntime }, setRuntimeRequest] =
      useCustomRuntime(namespace, gcePersistentDisk);

    // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
    if (currentRuntime && currentRuntime.status === RuntimeStatus.Deleted) {
      currentRuntime = applyPresetOverride(
        // The attached disk information is lost for deleted runtimes. In any case,
        // by default we want to offer that the user reattach their existing disk,
        // if any and if the configuration allows it.
        maybeWithExistingDisk(currentRuntime, gcePersistentDisk)
      );
    }

    const [status, setRuntimeStatus] = useRuntimeStatus(
      namespace,
      googleProject
    );

    // Prioritize the "pendingRuntime", if any. When an update is pending, we want
    // to render the target runtime details, which  may not match the current runtime.
    const existingRuntime =
      pendingRuntime || currentRuntime || ({} as Partial<Runtime>);
    const existingAnalysisConfig = toAnalysisConfig(
      existingRuntime,
      gcePersistentDisk
    );

    const [analysisConfig, setAnalysisConfig] = useState(
      withAnalysisConfigDefaults(existingAnalysisConfig, gcePersistentDisk)
    );

    const validMainMachineTypes =
      analysisConfig.computeType === ComputeType.Standard
        ? validLeoGceMachineTypes
        : validLeoDataprocMasterMachineTypes;
    // The compute type affects the set of valid machine types, so revert to the
    // default machine type if switching compute types would invalidate the main
    // machine type choice.
    useEffect(() => {
      if (
        !validMainMachineTypes.find(
          ({ name }) => name === analysisConfig.machine.name
        )
      ) {
        setAnalysisConfig({
          ...analysisConfig,
          machine: existingAnalysisConfig.machine,
        });
      }
    }, [analysisConfig.computeType]);

    const runtimeExists = (status && isVisible(status)) || !!pendingRuntime;

    let configDiffs: AnalysisDiff[] = [];
    let updateMessaging: UpdateMessaging;
    if (runtimeExists) {
      configDiffs = getAnalysisConfigDiffs(
        existingAnalysisConfig,
        analysisConfig
      );
      updateMessaging = diffsToUpdateMessaging(configDiffs);
    }

    return (
      <FlexColumn style={{ height: '100%' }}>
        <div
          data-test-id='runtime-create-panel'
          style={{ ...styles.controlSection, marginTop: '1rem' }}
        >
          <CostPredictor
            {...{
              creatorFreeCreditsRemaining,
              profile,
              workspace,
              analysisConfig,
            }}
            status={null}
            onPause={Promise.resolve()}
            onResume={Promise.resolve()}
            appType={AppType.CROMWELL}
          />
          <WarningMessage>
            This cost is only for running the Cromwell Engine, there will be
            additional cost for interactions with the workflow.
            <a href='#' style={{ marginLeft: '0.25rem' }}>
              Learn more{' '}
            </a>
            <i
              className='pi pi-external-link'
              style={{
                marginLeft: '0.25rem',
                fontSize: '0.75rem',
                color: '#6fb4ff',
                cursor: 'pointer',
              }}
            />
          </WarningMessage>
        </div>
        <div style={{ ...styles.controlSection, marginTop: '1rem' }}>
          <FlexRow style={{ alignItems: 'center' }}>
            <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
              Cromwell version: 76
            </div>
            <div
              style={{
                marginRight: '0.5rem',
                backgroundColor: '#ededed',
                color: '#7b828e',
                padding: '1rem 2rem 1rem 0.5rem',
                borderRadius: '0.5rem',
              }}
            >
              4 CPUS, 15GB RAM, 120GB disk
            </div>
            <a href='#' style={{ marginLeft: '0.25rem' }}>
              Learn more{' '}
            </a>
            <i
              className='pi pi-external-link'
              style={{
                marginLeft: '0.25rem',
                fontSize: '0.75rem',
                color: '#6fb4ff',
                cursor: 'pointer',
              }}
            />
          </FlexRow>
        </div>
        <div style={{ ...styles.controlSection, marginTop: '1rem' }}>
          <ol>
            <li>
              <a href='#'>How to run Cromwell in All of Us workbench?</a>
            </li>
            <li>
              <a href='#'>Cromwell documentation</a>
            </li>
            <li>
              <a href='#'>Workflow and WDL</a>
            </li>
            <li>
              <a href='#'>Running and Autopause</a>
            </li>
            <li>
              <a href='#'>Storage options</a>
            </li>
          </ol>
        </div>
        <FlexRow style={{ justifyContent: 'flex-end', marginTop: '1rem' }}>
          <Button>Start</Button>
        </FlexRow>
        <div style={{ margin: 'auto 0 2rem 0' }}>
          <div style={{ fontWeight: 'bold' }}>Next Steps:</div>
          <div>
            You can interact with the workflow by using the Cromshell in Jupyter
            Terminal or Jupyter notebook
          </div>
        </div>
      </FlexColumn>
    );
  }
);

export const CromwellConfigurationPanel = ({
  onClose = () => {},
  initialPanelContent = null,
  creatorFreeCreditsRemaining = null,
}) => {
  const { runtimeLoaded } = useStore(runtimeStore);

  if (!runtimeLoaded) {
    return <Spinner style={{ width: '100%', marginTop: '7.5rem' }} />;
  }

  return (
    <FlexColumn style={{ height: '100%' }}>
      <div>
        A cloud environment consists of application configuration, cloud compute
        and persistent disk(s). This is the server version of Cromwell only. You
        will need to create a Jupyter terminal environment in order to interact
        with the workflow.
      </div>
      <PanelMain
        {...{ onClose, initialPanelContent, creatorFreeCreditsRemaining }}
      />
    </FlexColumn>
  );
};
