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
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { faTerminal } from '@fortawesome/free-solid-svg-icons/faTerminal';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  AppStatus,
  AppType,
  CdrVersionTiersResponse,
  ConfigResponse,
  Criteria,
  GenomicExtractionJob,
  TerraJobStatus,
} from 'generated/fetch';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { DEFAULT, reactStyles, switchCase } from 'app/utils';
import { getCdrVersion } from 'app/utils/cdr-versions';
import { ComputeSecuritySuspendedError } from 'app/utils/runtime-utils';
import {
  runtimeStore,
  serverConfigStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';
import { supportUrls } from 'app/utils/zendesk';
import thunderstorm from 'assets/icons/thunderstorm-solid.svg';
import moment from 'moment/moment';

import { RouteLink } from './app-router';
import { AppStatusIcon } from './app-status-icon';
import { appAssets, showAppsPanel, UIAppType } from './apps-panel/utils';
import { FlexRow } from './flex';
import { TooltipTrigger } from './popups';
import { RuntimeStatusIcon } from './runtime-status-icon';

const styles = reactStyles({
  asyncOperationStatusIcon: {
    width: '.75rem',
    height: '.75rem',
    zIndex: 2,
  },
  statusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .15rem .15rem auto',
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
  criteriaCount: {
    position: 'absolute',
    height: '1.2rem',
    width: '1.2rem',
    top: '1.5rem',
    left: '0.825rem',
    textAlign: 'center',
    backgroundColor: colors.danger,
    borderRadius: '50%',
    display: 'inline-block',
    fontSize: '0.6rem',
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

export type SidebarIconId =
  | 'criteria'
  | 'concept'
  | 'help'
  | 'notebooksHelp'
  | 'dataDictionary'
  | 'annotations'
  | 'apps'
  | 'runtimeConfig'
  | 'cromwellConfig'
  | 'terminal'
  | 'genomicExtractions';

export interface IconConfig {
  id: SidebarIconId;
  disabled: boolean;
  faIcon: IconDefinition;
  label: string;
  showIcon: () => boolean;
  style: CSSProperties;
  tooltip: string;
  hasContent: boolean;
}

const displayAppStatusIcon = (
  icon: IconConfig,
  workspaceNamespace: string,
  userSuspended: boolean,
  config: ConfigResponse,
  status: AppStatus,
  appType: UIAppType
) => {
  const appTypeAssets = appAssets.find((aa) => aa.appType === appType);

  const containerStyle: CSSProperties = {
    height: '100%',
    alignItems: 'center',
    justifyContent: 'space-around',
  };
  const iconStyle: CSSProperties = { width: '36px', position: 'absolute' };

  return (
    <FlexRow style={containerStyle}>
      <img
        src={appTypeAssets?.icon}
        alt={icon?.label}
        style={iconStyle}
        data-test-id={'help-sidebar-icon-' + icon?.id}
      />
      <AppStatusIcon
        {...{ workspaceNamespace, userSuspended }}
        appStatus={status}
        style={styles.statusIconContainer}
      />
    </FlexRow>
  );
};

const displayRuntimeStatusIcon = (
  icon: IconConfig,
  workspaceNamespace: string,
  userSuspended: boolean,
  config: ConfigResponse
) => {
  const jupyterAssets = appAssets.find(
    (aa) => aa.appType === UIAppType.JUPYTER
  );

  const containerStyle: CSSProperties = {
    height: '100%',
    alignItems: 'center',
    justifyContent: 'space-around',
  };
  const iconStyle: CSSProperties = showAppsPanel(config)
    ? { width: '36px', position: 'absolute' }
    : { width: '22px', position: 'absolute' };

  const iconSrc = showAppsPanel(config) ? jupyterAssets.icon : thunderstorm;

  // We always want to show the thunderstorm or Jupyter icon.
  // For most runtime statuses (Deleting and Unknown currently excepted), we will show a small
  // overlay icon in the bottom right of the tab showing the runtime status.
  return (
    <FlexRow style={containerStyle}>
      <img
        src={iconSrc}
        alt={icon.label}
        style={iconStyle}
        data-test-id={'help-sidebar-icon-' + icon.id}
      />
      <RuntimeStatusIcon
        {...{ workspaceNamespace, userSuspended }}
        style={styles.statusIconContainer}
      />
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
  genomicExtractionJobs: GenomicExtractionJob[];
  userSuspended: boolean;
  icon: IconConfig;
}
const DisplayIcon = (props: DisplayIconProps) => {
  const {
    workspace,
    genomicExtractionJobs,
    criteria,
    concept,
    userSuspended,
    icon,
  } = props;
  const { config } = useStore(serverConfigStore);
  const { userApps } = useStore(userAppsStore);
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
      'apps',
      () => (
        <FlexRow
          style={{
            height: '100%',
            alignItems: 'center',
            justifyContent: 'space-around',
          }}
        >
          <img
            alt={icon.id}
            data-test-id={'help-sidebar-icon-' + icon.id}
            src={thunderstorm}
            style={{ ...icon.style, position: 'absolute' }}
          />
        </FlexRow>
      ),
    ],
    [
      'runtimeConfig',
      () =>
        displayRuntimeStatusIcon(
          icon,
          workspace.namespace,
          userSuspended,
          config
        ),
    ],
    [
      'cromwellConfig',
      () =>
        displayAppStatusIcon(
          icon,
          workspace.namespace,
          userSuspended,
          config,
          userApps?.find((app) => app.appType === AppType.CROMWELL)?.status,
          UIAppType.CROMWELL
        ),
    ],
    [
      'terminal',
      () => (
        <RouteLink
          path={`/workspaces/${workspace.namespace}/${workspace.id}/terminals`}
        >
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
            alt={icon.label}
            data-test-id={'help-sidebar-icon-' + icon.id}
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

const runtimeTooltip = (
  baseTooltip: string,
  loadingError: Error,
  userSuspended: boolean
): string => {
  const suspendedMessage = `Security suspended: ${baseTooltip}`;

  if (userSuspended) {
    return suspendedMessage;
  }

  if (loadingError) {
    if (loadingError instanceof ComputeSecuritySuspendedError) {
      return suspendedMessage;
    }
    return `${baseTooltip} (unknown error)`;
  }

  return baseTooltip;
};

interface IconConfigProps {
  iconId: SidebarIconId;
  pageKey: string;
  criteria: Array<Selection>;
  loadingError: Error;
  userSuspended: boolean;
}
const iconConfig = (props: IconConfigProps): IconConfig => {
  const { iconId, pageKey, criteria, loadingError, userSuspended } = props;

  const disableEnvironmentSidebarIcons = !!loadingError || userSuspended;

  // TODO: not sure why the iconKey needs to be converted to string here
  const config: { [iconKey: string]: IconConfig } = {
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
    apps: {
      id: 'apps',
      disabled: disableEnvironmentSidebarIcons,
      faIcon: null,
      label: 'Cloud Icon',
      showIcon: () => true,
      style: { height: '22px', width: '22px' },
      tooltip: runtimeTooltip('Applications', loadingError, userSuspended),
      hasContent: true,
    },
    runtimeConfig: {
      id: 'runtimeConfig',
      disabled: disableEnvironmentSidebarIcons,
      faIcon: null,
      label: 'Jupyter Icon',
      showIcon: () => true,
      style: { width: '36px' },
      tooltip: runtimeTooltip(
        'Cloud Analysis Environment',
        loadingError,
        userSuspended
      ),
      hasContent: true,
    },
    cromwellConfig: {
      id: 'cromwellConfig',
      disabled: disableEnvironmentSidebarIcons,
      faIcon: null,
      label: 'Cromwell Icon',
      showIcon: () => true,
      style: { width: '36px' },
      tooltip: runtimeTooltip(
        'Cromwell Cloud Environment',
        loadingError,
        userSuspended
      ),
      hasContent: true,
    },
    terminal: {
      id: 'terminal',
      disabled: disableEnvironmentSidebarIcons,
      faIcon: faTerminal,
      label: 'Terminal Icon',
      showIcon: () => true,
      style: { height: '22px', width: '22px' },
      tooltip: runtimeTooltip(
        'Cloud Analysis Terminal',
        loadingError,
        userSuspended
      ),
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
        marginTop: '0.375rem',
        position: 'absolute',
      } as CSSProperties,
      tooltip: 'Genomic Extraction History',
      hasContent: true,
    },
  };

  return config[iconId];
};

interface HelpSidebarIconsProps {
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  genomicExtractionJobs: GenomicExtractionJob[];
  activeIcon: string;
  onIconClick: (icon: IconConfig) => void;
  pageKey: string;
  criteria: Array<Selection>;
  userSuspended: boolean;
}
export const HelpSidebarIcons = (props: HelpSidebarIconsProps) => {
  const {
    workspace,
    cdrVersionTiersResponse,
    activeIcon,
    onIconClick,
    pageKey,
    criteria,
    userSuspended,
  } = props;
  const { loadingError } = useStore(runtimeStore);
  const { enableGenomicExtraction } = serverConfigStore.get().config;
  const { config } = useStore(serverConfigStore);
  const defaultIcons: SidebarIconId[] = [
    'criteria',
    'concept',
    'help',
    'notebooksHelp',
    'dataDictionary',
    'annotations',
  ];
  const keys: SidebarIconId[] = defaultIcons.filter((iconId) =>
    iconConfig({
      iconId,
      pageKey,
      criteria,
      loadingError,
      userSuspended,
    }).showIcon()
  );

  if (WorkspacePermissionsUtil.canWrite(workspace.accessLevel)) {
    if (showAppsPanel(config)) {
      keys.push('apps');
    }
    if (config.enableCromwellGKEApp) {
      keys.push('cromwellConfig');
    }
    keys.push('runtimeConfig', 'terminal');
  }

  if (
    enableGenomicExtraction &&
    getCdrVersion(workspace, cdrVersionTiersResponse).hasWgsData
  ) {
    keys.push('genomicExtractions');
  }

  const icons = keys.map((iconId) =>
    iconConfig({ iconId, pageKey, criteria, loadingError, userSuspended })
  );

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
              <DisplayIcon {...{ ...props, icon }} />
            </div>
          </TooltipTrigger>
        </div>
      ))}
    </>
  );
};
