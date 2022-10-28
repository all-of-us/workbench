import * as React from 'react';
import { CSSProperties } from 'react';
import * as fp from 'lodash/fp';
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';
import { faCircle } from '@fortawesome/free-solid-svg-icons/faCircle';
import { faLock } from '@fortawesome/free-solid-svg-icons/faLock';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  Criteria,
  GenomicExtractionJob,
  RuntimeStatus,
  TerraJobStatus,
} from 'generated/fetch';

import colors from 'app/styles/colors';
import { DEFAULT, reactStyles, switchCase } from 'app/utils';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import { CompoundRuntimeOpStore, RuntimeStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import { supportUrls } from 'app/utils/zendesk';
import arrowLeft from 'assets/icons/arrow-left-regular.svg';
import runtime from 'assets/icons/thunderstorm-solid.svg';
import times from 'assets/icons/times-light.svg';
import moment from 'moment/moment';

import { RouteLink } from './app-router';
import { FlexRow } from './flex';

const styles = reactStyles({
  asyncOperationStatusIcon: {
    width: '.5rem',
    height: '.5rem',
    zIndex: 2,
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.25rem',
  },
  statusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .1rem .1rem auto',
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
  criteriaCount: {
    position: 'absolute',
    height: '0.8rem',
    width: '0.8rem',
    top: '1rem',
    left: '0.55rem',
    textAlign: 'center',
    backgroundColor: colors.danger,
    borderRadius: '50%',
    display: 'inline-block',
    fontSize: '0.4rem',
  },
});

export interface IconConfig {
  id: string;
  disabled: boolean;
  faIcon: IconDefinition;
  label: string;
  showIcon: () => boolean;
  style: CSSProperties;
  tooltip: string;
  hasContent: true;
}

export const proIcons = {
  arrowLeft: arrowLeft,
  runtime: runtime,
  times: times,
};

const displayRuntimeIcon = (
  icon: IconConfig,
  workspace: WorkspaceData,
  store: RuntimeStore,
  compoundRuntimeOps: CompoundRuntimeOpStore
) => {
  let status = store?.runtime?.status;
  if (
    (!status || status === RuntimeStatus.Deleted) &&
    workspace.namespace in compoundRuntimeOps
  ) {
    // If a compound operation is still pending, and we're transitioning
    // through the "Deleted" phase of the runtime, we want to keep showing
    // an activity spinner. Avoids an awkward UX during a delete/create cycle.
    // There also be some lag during the runtime creation flow between when
    // the compound operation starts, and the runtime is set in the store; for
    // this reason use Creating rather than Deleting here.
    status = RuntimeStatus.Creating;
  }

  // We always want to show the thunderstorm icon.
  // For most runtime statuses (Deleting and Unknown currently excepted), we will show a small
  // overlay icon in the bottom right of the tab showing the runtime status.
  return (
    <FlexRow
      style={{
        height: '100%',
        alignItems: 'center',
        justifyContent: 'space-around',
      }}
    >
      <img
        data-test-id={'help-sidebar-icon-' + icon.id}
        src={proIcons[icon.id]}
        style={{ ...icon.style, position: 'absolute' }}
      />
      <FlexRow
        data-test-id='runtime-status-icon-container'
        style={styles.statusIconContainer}
      >
        {(() => {
          const errIcon = (
            <FontAwesomeIcon
              icon={faCircle}
              style={{
                ...styles.asyncOperationStatusIcon,
                ...styles.runtimeStatusIconOutline,
                color: colors.asyncOperationStatus.error,
              }}
            />
          );

          if (store.loadingError) {
            if (store.loadingError instanceof ComputeSecuritySuspendedError) {
              return (
                <FontAwesomeIcon
                  icon={faLock}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    color: colors.asyncOperationStatus.stopped,
                  }}
                />
              );
            }
            return errIcon;
          }
          switch (status) {
            case RuntimeStatus.Creating:
            case RuntimeStatus.Starting:
            case RuntimeStatus.Updating:
              return (
                <FontAwesomeIcon
                  icon={faSyncAlt}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.rotate,
                    color: colors.asyncOperationStatus.starting,
                  }}
                />
              );
            case RuntimeStatus.Stopped:
              return (
                <FontAwesomeIcon
                  icon={faCircle}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.runtimeStatusIconOutline,
                    color: colors.asyncOperationStatus.stopped,
                  }}
                />
              );
            case RuntimeStatus.Running:
              return (
                <FontAwesomeIcon
                  icon={faCircle}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.runtimeStatusIconOutline,
                    color: colors.asyncOperationStatus.running,
                  }}
                />
              );
            case RuntimeStatus.Stopping:
            case RuntimeStatus.Deleting:
              return (
                <FontAwesomeIcon
                  icon={faSyncAlt}
                  style={{
                    ...styles.asyncOperationStatusIcon,
                    ...styles.rotate,
                    color: colors.asyncOperationStatus.stopping,
                  }}
                />
              );
            case RuntimeStatus.Error:
              return errIcon;
          }
        })()}
      </FlexRow>
    </FlexRow>
  );
};

const withinPastTwentyFourHours = (epoch: number) => {
  const completionTimeMoment = moment(epoch);
  const twentyFourHoursAgo = moment().subtract(1, 'days');
  return completionTimeMoment.isAfter(twentyFourHoursAgo);
};

const displayFontAwesomeIcon = (
  icon: IconConfig,
  criteria: Array<Selection>,
  concept: Array<Criteria>
) => {
  return (
    <React.Fragment>
      {icon.id === 'criteria' && criteria && criteria.length > 0 && (
        <span data-test-id='criteria-count' style={styles.criteriaCount}>
          {criteria.length}
        </span>
      )}
      {icon.id === 'concept' && concept && concept.length > 0 && (
        <span data-test-id='concept-count' style={styles.criteriaCount}>
          {concept.length}
        </span>
      )}
      <FontAwesomeIcon
        data-test-id={'help-sidebar-icon-' + icon.id}
        icon={icon.faIcon}
        style={icon.style}
      />
    </React.Fragment>
  );
};

const displayExtractionIcon = (
  icon: IconConfig,
  genomicExtractionJobs,
  criteria: Array<Selection>,
  concept: Array<Criteria>
) => {
  const jobsByStatus = fp.groupBy('status', genomicExtractionJobs);
  let status;
  // If any jobs are currently active, show the 'sync' icon corresponding to their status.
  if (jobsByStatus[TerraJobStatus.RUNNING]) {
    status = TerraJobStatus.RUNNING;
  } else if (jobsByStatus[TerraJobStatus.ABORTING]) {
    status = TerraJobStatus.ABORTING;
  } else if (
    jobsByStatus[TerraJobStatus.SUCCEEDED] ||
    jobsByStatus[TerraJobStatus.FAILED] ||
    jobsByStatus[TerraJobStatus.ABORTED]
  ) {
    // Otherwise, show the status of the most recent completed job, if it was completed within the past 24h.
    const completedJobs = fp.flatten([
      jobsByStatus[TerraJobStatus.SUCCEEDED] || [],
      jobsByStatus[TerraJobStatus.FAILED] || [],
      jobsByStatus[TerraJobStatus.ABORTED] || [],
    ]);
    const mostRecentCompletedJob = fp.flow(
      fp.filter((job: GenomicExtractionJob) =>
        withinPastTwentyFourHours(job.completionTime)
      ),
      // This could be phrased as fp.sortBy('completionTime') but it confuses the compile time type checker
      fp.sortBy((job) => job.completionTime),
      fp.reverse,
      fp.head
    )(completedJobs);
    if (mostRecentCompletedJob) {
      status = mostRecentCompletedJob.status;
    }
  }

  // We always want to show the DNA icon.
  // When there are running or recently completed  jobs, we will show a small overlay icon in
  // the bottom right of the tab showing the job status.
  return (
    <FlexRow
      style={{
        height: '100%',
        alignItems: 'center',
        justifyContent: 'space-around',
      }}
    >
      {displayFontAwesomeIcon(icon, criteria, concept)}
      <FlexRow
        data-test-id='extraction-status-icon-container'
        style={styles.statusIconContainer}
      >
        {switchCase(
          status,
          [
            TerraJobStatus.RUNNING,
            () => (
              <FontAwesomeIcon
                icon={faSyncAlt}
                style={{
                  ...styles.asyncOperationStatusIcon,
                  ...styles.rotate,
                  color: colors.asyncOperationStatus.starting,
                }}
              />
            ),
          ],
          [
            TerraJobStatus.ABORTING,
            () => (
              <FontAwesomeIcon
                icon={faSyncAlt}
                style={{
                  ...styles.asyncOperationStatusIcon,
                  ...styles.rotate,
                  color: colors.asyncOperationStatus.stopping,
                }}
              />
            ),
          ],
          [
            TerraJobStatus.FAILED,
            () => (
              <FontAwesomeIcon
                icon={faCircle}
                style={{
                  ...styles.asyncOperationStatusIcon,
                  color: colors.asyncOperationStatus.error,
                }}
              />
            ),
          ],
          [
            TerraJobStatus.SUCCEEDED,
            () => (
              <FontAwesomeIcon
                icon={faCircle}
                style={{
                  ...styles.asyncOperationStatusIcon,
                  color: colors.asyncOperationStatus.succeeded,
                }}
              />
            ),
          ],
          [
            TerraJobStatus.ABORTED,
            () => (
              <FontAwesomeIcon
                icon={faCircle}
                style={{
                  ...styles.asyncOperationStatusIcon,
                  color: colors.asyncOperationStatus.stopped,
                }}
              />
            ),
          ]
        )}
      </FlexRow>
    </FlexRow>
  );
};

interface TempProps {
  workspace: WorkspaceData;
  criteria: Array<Selection>;
  concept?: Array<Criteria>;
  runtimeStore: RuntimeStore;
  compoundRuntimeOps: CompoundRuntimeOpStore;
  genomicExtractionJobs: GenomicExtractionJob[];
}
export const displayIcon = (icon: IconConfig, props: TempProps) => {
  const {
    workspace,
    runtimeStore,
    compoundRuntimeOps,
    genomicExtractionJobs,
    criteria,
    concept,
  } = props;
  const terminalRoute = `/workspaces/${workspace.namespace}/${workspace.id}/terminals`;
  return switchCase(
    icon.id,
    [
      'dataDictionary',
      () => (
        <a href={supportUrls.dataDictionary} target='_blank'>
          <FontAwesomeIcon
            data-test-id={'help-sidebar-icon-' + icon.id}
            icon={icon.faIcon}
            style={icon.style}
          />
        </a>
      ),
    ],
    [
      'runtime',
      () =>
        displayRuntimeIcon(icon, workspace, runtimeStore, compoundRuntimeOps),
    ],
    [
      'terminal',
      () => (
        <RouteLink path={terminalRoute}>
          <FontAwesomeIcon
            data-test-id={'help-sidebar-icon-' + icon.id}
            icon={icon.faIcon}
            style={icon.style}
          />
        </RouteLink>
      ),
    ],
    [
      'genomicExtractions',
      () =>
        displayExtractionIcon(icon, genomicExtractionJobs, criteria, concept),
    ],
    [
      DEFAULT,
      () =>
        icon.faIcon === null ? (
          <img
            data-test-id={'help-sidebar-icon-' + icon.id}
            src={proIcons[icon.id]}
            style={icon.style}
          />
        ) : (
          displayFontAwesomeIcon(icon, criteria, concept)
        ),
    ]
  );
};
