import * as React from 'react';
import { CSSTransition, SwitchTransition } from 'react-transition-group';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import {
  faBan,
  faCheckCircle,
  faExclamationTriangle,
} from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { GenomicExtractionJob, TerraJobStatus } from 'generated/fetch';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import { FlexRow } from 'app/components/flex';
import { GenomicsExtractionMenu } from 'app/components/genomics-extraction-menu';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { TextColumn } from 'app/components/text-column';
import colors from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { useGenomicExtractionJobs } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import moment from 'moment';

import { SupportMailto } from './support';

const styles = {
  spinStyles: {
    animationName: 'spin',
    animationDuration: '5000ms',
    animationIterationCount: 'infinite',
    animationTimingFunction: 'linear',
  },
};

const getIconConfigForStatus = (status: TerraJobStatus) => {
  if (status === TerraJobStatus.RUNNING) {
    return {
      icon: faSyncAlt,
      iconTooltip: 'Processing extraction',
      style: {
        color: colors.success,
        ...styles.spinStyles,
      },
    };
  } else if (status === TerraJobStatus.SUCCEEDED) {
    return {
      icon: faCheckCircle,
      style: {
        color: colors.success,
      },
    };
  } else if (status === TerraJobStatus.FAILED) {
    return {
      icon: faExclamationTriangle,
      iconTooltip:
        "This extraction has failed. Please try again from the dataset's page.",
      style: {
        color: colors.danger,
      },
    };
  } else if (status === TerraJobStatus.ABORTING) {
    return {
      icon: faSyncAlt,
      iconTooltip: 'Aborting this extraction. This may take a few minutes.',
      style: {
        color: colors.warning,
        ...styles.spinStyles,
      },
    };
  } else if (status === TerraJobStatus.ABORTED) {
    return {
      icon: faBan,
      iconTooltip: 'This extraction was aborted by the user.',
      style: {
        color: colors.warning,
      },
    };
  }
};

const formatDatetime = (timeEpoch: number) => {
  const timeMoment = moment(timeEpoch);
  const isToday = moment().isSame(timeMoment, 'day');
  const momentFormat = (isToday ? '[Today]' : 'MMM D, YYYY') + ' [at] h:mm a';
  return timeMoment.format(momentFormat);
};

const formatDuration = (durationMoment) => {
  const hours = Math.floor(durationMoment.asHours());
  const minStr = Math.floor(durationMoment.minutes()) + ' min';

  return (hours > 0 ? hours + ' hr, ' : '') + minStr;
};

const MissingCell = () => <span style={{ fontSize: '.6rem' }}>&mdash;</span>;

const mapJobToTableRow = (
  job: GenomicExtractionJob,
  workspace: WorkspaceData,
  onMutate: () => void
) => {
  const iconConfig = getIconConfigForStatus(job.status);
  const durationMoment =
    job.completionTime &&
    moment.duration(
      moment(job.completionTime).diff(moment(job.submissionDate))
    );

  return {
    datasetName: job.datasetName,
    datasetNameDisplay: (
      <span
        style={{ opacity: job.status === TerraJobStatus.RUNNING ? 0.5 : 1 }}
      >
        {job.datasetName}
      </span>
    ),
    // The true ordering doesn't matter so much as having RUNNING and FAILED be at both ends of the order
    statusOrdinal: switchCase<any, number>(
      job.status,
      [TerraJobStatus.RUNNING, () => 0],
      [TerraJobStatus.SUCCEEDED, () => 1],
      [TerraJobStatus.ABORTED, () => 2],
      [TerraJobStatus.ABORTING, () => 3],
      [TerraJobStatus.FAILED, () => 4],
      [DEFAULT, () => Number.MAX_SAFE_INTEGER]
    ),
    statusDisplay: (
      <TooltipTrigger content={iconConfig.iconTooltip}>
        <div>
          {' '}
          {/* This div wrapper is needed so the tooltip doesn't move around with the spinning icon*/}
          <FontAwesomeIcon
            icon={iconConfig.icon}
            style={{
              ...iconConfig.style,
              fontSize: '1.05rem',
              marginLeft: '.6rem',
              display: 'block',
            }}
          />
        </div>
      </TooltipTrigger>
    ),
    dateStarted: job.submissionDate,
    dateStartedDisplay: formatDatetime(job.submissionDate),
    duration: durationMoment?.asSeconds(),
    durationDisplay: !!durationMoment ? (
      formatDuration(durationMoment)
    ) : (
      <MissingCell />
    ),
    size: job.vcfSizeMb,
    sizeDisplay:
      job.vcfSizeMb === null ? (
        <MissingCell />
      ) : (
        (job.vcfSizeMb / 1000).toFixed(1) + 'GB'
      ),
    menuJsx: <GenomicsExtractionMenu {...{ job, workspace, onMutate }} />,
  };
};

const EmptyTableMessage = () => (
  <TextColumn style={{ fontSize: '0.75rem', paddingTop: '0.75rem' }}>
    <span>
      This will be the location to find any extracted genomics files you may
      need for your research.
    </span>
    <span>
      Genomic extractions can be created once you have a dataset that contains
      genomics data.
    </span>
  </TextColumn>
);

const FailedRequestMessage = () => (
  <div style={{ textAlign: 'center' }}>
    <FlexRow
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        margin: '3rem auto',
      }}
    >
      <FontAwesomeIcon
        icon={faExclamationTriangle}
        style={{
          color: colors.danger,
          fontSize: '1.2rem',
        }}
      />
      <TextColumn
        style={{ textAlign: 'left', marginLeft: '0.75rem', marginBottom: 0 }}
      >
        <span>Failed to retrieve genomic extraction jobs.</span>
        <span>
          Please try again or contact <SupportMailto />.
        </span>
      </TextColumn>
    </FlexRow>
  </div>
);

export const GenomicsExtractionTable = fp.flow(withCurrentWorkspace())(
  ({ workspace }) => {
    const {
      data: jobs,
      error,
      mutate,
    } = useGenomicExtractionJobs(workspace.namespace, workspace.id);

    return (
      <div
        id='extraction-data-table-container'
        className='extraction-data-table-container'
      >
        <div className='slim-scroll-bar'>
          {
            // This adds a fade between the loading state and the results page.
          }
          <SwitchTransition>
            <CSSTransition<undefined>
              key={jobs?.toString() ?? 'noJobs'}
              classNames='switch-transition'
              addEndListener={(node, done) => {
                node.addEventListener('transitionend', done, false);
              }}
            >
              {!jobs ? (
                <Spinner style={{ display: 'block', margin: '4.5rem auto' }} />
              ) : error ? (
                <FailedRequestMessage />
              ) : (
                <DataTable
                  emptyMessage={<EmptyTableMessage />}
                  sortField={!jobs || jobs.length > 0 ? 'dateStarted' : ''}
                  sortOrder={-1}
                  breakpoint='0px'
                  value={jobs.map((job) =>
                    mapJobToTableRow(job, workspace, () => {
                      mutate();
                    })
                  )}
                  style={{ marginLeft: '0.75rem', marginRight: '0.75rem' }}
                >
                  <Column
                    header='Dataset Name'
                    field='datasetNameDisplay'
                    sortable
                    sortField='datasetName'
                    style={{
                      maxWidth: '12rem',
                      textOverflow: 'ellipsis',
                      overflow: 'hidden',
                      whiteSpace: 'nowrap',
                    }}
                  />
                  <Column
                    header='Status'
                    field='statusDisplay'
                    sortable
                    sortField='statusOrdinal'
                  />
                  <Column
                    header='Date Started'
                    field='dateStartedDisplay'
                    sortable
                    sortField='dateStarted'
                  />
                  <Column
                    header='Size'
                    field='sizeDisplay'
                    sortable
                    sortField='size'
                  />
                  <Column
                    header='Duration'
                    field='durationDisplay'
                    sortable
                    sortField='duration'
                  />
                  <Column header='' field='menuJsx' />
                </DataTable>
              )}
            </CSSTransition>
          </SwitchTransition>
        </div>
      </div>
    );
  }
);
