import * as React from 'react';
import { CSSProperties } from 'react';
import * as fp from 'lodash/fp';
import { faEdit } from '@fortawesome/free-regular-svg-icons';
import {
  faBook,
  faFolderOpen,
  faInbox,
  faInfoCircle,
  IconDefinition,
} from '@fortawesome/free-solid-svg-icons';
import { faCircle } from '@fortawesome/free-solid-svg-icons/faCircle';
import { faDna } from '@fortawesome/free-solid-svg-icons/faDna';
import { faLock } from '@fortawesome/free-solid-svg-icons/faLock';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { faTerminal } from '@fortawesome/free-solid-svg-icons/faTerminal';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  CdrVersionTiersResponse,
  Criteria,
  GenomicExtractionJob,
  RuntimeStatus,
  TerraJobStatus,
} from 'generated/fetch';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { DEFAULT, reactStyles, switchCase } from 'app/utils';
import { getCdrVersion } from 'app/utils/cdr-versions';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import {
  CompoundRuntimeOpStore,
  RuntimeStore,
  serverConfigStore,
} from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';
import { supportUrls } from 'app/utils/zendesk';
import arrowLeft from 'assets/icons/arrow-left-regular.svg';
import runtime from 'assets/icons/thunderstorm-solid.svg';
import times from 'assets/icons/times-light.svg';
import moment from 'moment/moment';

import { RouteLink } from './app-router';
import { FlexRow } from './flex';
import { TooltipTrigger } from './popups';

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
  icon: {
    background: colorWithWhiteness(colors.primary, 0.48),
    color: colors.white,
    display: 'table-cell',
    height: '46px',
    width: '45px',
    borderBottom: `1px solid ${colorWithWhiteness(colors.primary, 0.4)}`,
    cursor: 'pointer',
    textAlign: 'center',
    verticalAlign: 'middle',
  },
});

const iconStyles = reactStyles({
  active: {
    ...styles.icon,
    background: colorWithWhiteness(colors.primary, 0.55),
  },
  disabled: {
    ...styles.icon,
    cursor: 'not-allowed',
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
  hasContent: boolean;
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
) => (
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

interface DisplayIconProps {
  workspace: WorkspaceData;
  criteria: Array<Selection>;
  concept?: Array<Criteria>;
  runtimeStore: RuntimeStore;
  compoundRuntimeOps: CompoundRuntimeOpStore;
  genomicExtractionJobs: GenomicExtractionJob[];
}
const displayIcon = (icon: IconConfig, props: DisplayIconProps) => {
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

export const showCriteriaIcon = (pageKey: string, criteria: Array<Selection>) =>
  pageKey === 'cohortBuilder' && !!criteria;
export const showConceptIcon = (pageKey: string) => pageKey === 'conceptSets';

type IconKey =
  | 'criteria'
  | 'concept'
  | 'help'
  | 'notebooksHelp'
  | 'dataDictionary'
  | 'annotations'
  | 'runtime'
  | 'terminal'
  | 'genomicExtractions';

interface IconConfigProps {
  pageKey: string;
  criteria: Array<Selection>;
  runtimeStore: RuntimeStore;
  runtimeTooltip: (text: string) => string;
}
const iconConfig = (iconKey: IconKey, props: IconConfigProps): IconConfig => {
  const { pageKey, criteria, runtimeStore, runtimeTooltip } = props;
  return {
    criteria: {
      id: 'criteria',
      disabled: false,
      faIcon: faInbox,
      label: 'Selected Criteria',
      showIcon: () => showCriteriaIcon(pageKey, criteria),
      style: { fontSize: '21px' },
      tooltip: 'Selected Criteria',
      hasContent: true,
    },
    concept: {
      id: 'concept',
      disabled: false,
      faIcon: faInbox,
      label: 'Selected Concepts',
      showIcon: () => showConceptIcon(pageKey),
      style: { fontSize: '21px' },
      tooltip: 'Selected Concepts',
      hasContent: true,
    },
    help: {
      id: 'help',
      disabled: false,
      faIcon: faInfoCircle,
      label: 'Help Icon',
      showIcon: () => true,
      style: { fontSize: '21px' },
      tooltip: 'Help Tips',
      hasContent: true,
    },
    notebooksHelp: {
      id: 'notebooksHelp',
      disabled: false,
      faIcon: faFolderOpen,
      label: 'Storage Icon',
      showIcon: () => true,
      style: { fontSize: '21px' },
      tooltip: 'Workspace Storage',
      hasContent: true,
    },
    dataDictionary: {
      id: 'dataDictionary',
      disabled: false,
      faIcon: faBook,
      label: 'Data Dictionary Icon',
      showIcon: () => true,
      style: { color: colors.white, fontSize: '20px', marginTop: '5px' },
      tooltip: 'Data Dictionary',
      hasContent: false,
    },
    annotations: {
      id: 'annotations',
      disabled: false,
      faIcon: faEdit,
      label: 'Annotations Icon',
      showIcon: () => pageKey === 'reviewParticipantDetail',
      style: { fontSize: '20px', marginLeft: '3px' },
      tooltip: 'Annotations',
      hasContent: true,
    },
    runtime: {
      id: 'runtime',
      disabled: !!runtimeStore.loadingError,
      faIcon: null,
      label: 'Cloud Icon',
      showIcon: () => true,
      style: { height: '22px', width: '22px' },
      tooltip: runtimeTooltip('Cloud Analysis Environment'),
      hasContent: true,
    },
    terminal: {
      id: 'terminal',
      disabled: !!runtimeStore.loadingError,
      faIcon: faTerminal,
      label: 'Terminal Icon',
      showIcon: () => true,
      style: { height: '22px', width: '22px' },
      tooltip: runtimeTooltip('Cloud Analysis Terminal'),
      hasContent: false,
    },
    genomicExtractions: {
      id: 'genomicExtractions',
      disabled: false,
      faIcon: faDna,
      label: 'Genomic Extraction',
      showIcon: () => true,
      // position: absolute is so the status icon won't push the DNA icon to the left.
      style: {
        height: '22px',
        width: '22px',
        marginTop: '0.25rem',
        position: 'absolute',
      } as CSSProperties,
      tooltip: 'Genomic Extraction History',
      hasContent: true,
    },
  }[iconKey];
};

interface HelpSidebarIconsProps extends IconConfigProps {
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  compoundRuntimeOps: CompoundRuntimeOpStore;
  genomicExtractionJobs: GenomicExtractionJob[];
  activeIcon: string;
  onIconClick: (icon: IconConfig) => void;
}
export const HelpSidebarIcons = (props: HelpSidebarIconsProps) => {
  const { workspace, cdrVersionTiersResponse, activeIcon, onIconClick } = props;

  const defaultIcons: IconKey[] = [
    'criteria',
    'concept',
    'help',
    'notebooksHelp',
    'dataDictionary',
    'annotations',
  ];
  const keys: IconKey[] = defaultIcons.filter((key) =>
    iconConfig(key, props).showIcon()
  );

  if (WorkspacePermissionsUtil.canWrite(workspace.accessLevel)) {
    keys.push('runtime', 'terminal');
  }

  if (
    serverConfigStore.get().config.enableGenomicExtraction &&
    getCdrVersion(workspace, cdrVersionTiersResponse).hasWgsData
  ) {
    keys.push('genomicExtractions');
  }

  const icons = keys.map((key) => iconConfig(key, props));

  return (
    <>
      {icons.map((icon, i) => (
        <div key={i} style={{ display: 'table' }}>
          <TooltipTrigger content={<div>{icon.tooltip}</div>} side='left'>
            <div
              style={
                activeIcon === icon.id
                  ? iconStyles.active
                  : icon.disabled
                  ? iconStyles.disabled
                  : styles.icon
              }
              onClick={() => {
                if (icon.hasContent && !icon.disabled) {
                  onIconClick(icon);
                }
              }}
            >
              {displayIcon(icon, props)}
            </div>
          </TooltipTrigger>
        </div>
      ))}
    </>
  );
};
