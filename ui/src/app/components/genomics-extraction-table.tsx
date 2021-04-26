import {
  faBan,
  faCheckCircle, faClipboard,
  faEllipsisV,
  faExclamationTriangle,
  faLocationArrow, faTrash
} from '@fortawesome/free-solid-svg-icons';
import {faSyncAlt} from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

import {FlexRow} from 'app/components/flex';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {withCurrentWorkspace} from 'app/utils';
import {formatUsd} from 'app/utils/numbers';
import {GenomicExtractionJob, TerraJobStatus, WorkspaceAccessLevel} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';
import {useEffect, useState} from 'react';
import {CSSTransition, SwitchTransition} from 'react-transition-group';
import {MenuItem} from "./buttons";
import {faLocationCircle} from "@fortawesome/pro-solid-svg-icons";
import {WorkspacePermissionsUtil} from "../utils/workspace-permissions";
import canWrite = WorkspacePermissionsUtil.canWrite;
import {WorkspaceData} from "../utils/workspace-data";

const getIconConfigForStatus = (status: TerraJobStatus) => {
  if (status === TerraJobStatus.RUNNING) {
    return {
      icon: faSyncAlt,
      iconTooltip: 'Processing extraction',
      style: {
        color: colors.success,
        animationName: 'spin',
        animationDuration: '5000ms',
        animationIterationCount: 'infinite',
        animationTimingFunction: 'linear'
      }
    };
  } else if (status === TerraJobStatus.SUCCEEDED) {
    return {
      icon: faCheckCircle,
      style: {
        color: colors.success
      }
    };
  } else if (status === TerraJobStatus.FAILED) {
    return {
      icon: faExclamationTriangle,
      iconTooltip: 'This extraction has failed. Please try again from the dataset\'s page.',
      style: {
        color: colors.danger
      }
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

const GenomicsExtractionMenu = ({job, workspace}) => {
  return <PopupTrigger
    side='bottom'
    closeOnClick
    content={
     <React.Fragment>
       <MenuItem
           faIcon={faLocationCircle}
           disabled
           onClick={() => {}}
       >
        View Path
       </MenuItem>
       <MenuItem
           faIcon={faClipboard}
           disabled
           onClick={() => {}}
       >
         Copy Snippet
       </MenuItem>
       <hr/>
       <MenuItem
           faIcon={faBan}
           disabled={job.status !== TerraJobStatus.RUNNING || !canWrite(workspace.accessLevel)}
           onClick={() => {
             dataSetApi().abortExtract(workspace.namespace, workspace.id, job.genomicExtractionJobId)
           }}
           tooltip={
             job.status !== TerraJobStatus.RUNNING
               ? "Extraction job is not currently running"
               : !canWrite(workspace.accessLevel)
                 ? "You do not have permission to modify this workspace"
                 : ""
           }
       >
         Abort Extraction
       </MenuItem>
       <hr/>
       <MenuItem
         faIcon={faTrash}
         disabled
         onClick={() => {}}
       >
         Delete Extract
       </MenuItem>
     </React.Fragment>
    }
  >
    <FontAwesomeIcon
        icon={faEllipsisV}
        style={{
          color: colors.accent,
          fontSize: '.7rem',
          marginLeft: 0,
          paddingRight: 0,
          display: 'block'
        }}
    />
  </PopupTrigger>
}

const MissingCell = () => <span style={{fontSize: '.4rem'}}>&mdash;</span>;

const mapJobToTableRow = (job: GenomicExtractionJob, workspace: WorkspaceData) => {
  const iconConfig = getIconConfigForStatus(job.status);
  const durationMoment = job.completionTime && moment.duration(moment(job.completionTime).diff(moment(job.submissionDate)));

  return {
    datasetName: job.datasetName,
    datasetNameDisplay:
      <span style={{opacity: job.status === TerraJobStatus.RUNNING ? .5 : 1}}>
        {job.datasetName}
      </span>,
    statusOrdinal: // The true ordering doesn't matter so much as having RUNNING and FAILED be at both ends of the order
      job.status === TerraJobStatus.RUNNING ? 0 :
        job.status === TerraJobStatus.SUCCEEDED ? 1 :
          job.status === TerraJobStatus.FAILED ? 2 : Number.MAX_SAFE_INTEGER,
    statusDisplay: <TooltipTrigger content={iconConfig.iconTooltip}>
      <div> {/*This div wrapper is needed so the tooltip doesn't move around with the spinning icon*/}
        <FontAwesomeIcon
          icon={iconConfig.icon}
          style={{
            ...iconConfig.style,
            fontSize: '.7rem',
            marginLeft: '.4rem',
            display: 'block'
          }}/>
      </div>
    </TooltipTrigger>,
    dateStarted: job.submissionDate,
    dateStartedDisplay: formatDatetime(job.submissionDate),
    duration: durationMoment && durationMoment.asSeconds(),
    durationDisplay: !!durationMoment ? formatDuration(durationMoment) : <MissingCell/>,
    cost: job.cost,
    costDisplay: job.cost === null ? // !!job.cost doesn't work here because 0 is a valid value
      <MissingCell/> : formatUsd(job.cost),
    menuJsx: <GenomicsExtractionMenu job={job} workspace={workspace}/>,
  };
};

const EmptyTableMessage = () => <TextColumn style={{fontSize: '0.5rem', paddingTop: '0.5rem'}}>
  <span>This will be the location to find any extracted genomics files you may need for your research.</span>
  <span>Genomic extractions can be created once you have a dataset that contains genomics data.</span>
</TextColumn>;

const FailedRequestMessage = () => <div style={{textAlign: 'center'}}>
  <FlexRow style={{
    display: 'inline-flex',
    alignItems: 'center',
    margin: '2rem auto'
  }}>
    <FontAwesomeIcon
      icon={faExclamationTriangle}
      style={{
        color: colors.danger,
        fontSize: '.8rem'
      }}/>
    <TextColumn style={{textAlign: 'left', marginLeft: '0.5rem', marginBottom: 0}}>
      <span>
        Failed to retrieve genomic extraction jobs.
      </span>
        <span>
        Please try again or contact <a href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
      </span>
    </TextColumn>
  </FlexRow>
</div>;

export const GenomicsExtractionTable = fp.flow(withCurrentWorkspace())(({workspace}) => {
  const [extractionJobs, setExtractionJobs] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    dataSetApi().getGenomicExtractionJobs(workspace.namespace, workspace.id)
      .then(resp => setExtractionJobs(resp.jobs))
      .finally(() => setIsLoading(false));
  }, [workspace]);

  const requestFailed = !isLoading && extractionJobs === null;

  return <div id='extraction-data-table-container' className='extraction-data-table-container'>
    <div className='slim-scroll-bar'>
      <SwitchTransition>
        <CSSTransition
          key={isLoading}
          classNames='switch-transition'
          addEndListener={(node, done) => node.addEventListener('transitionend', done, false)}>
          {
            isLoading
              ? <Spinner style={{display: 'block', margin: '3rem auto'}}/>
              : requestFailed
                ? <FailedRequestMessage/>
                : <DataTable autoLayout
                         emptyMessage={<EmptyTableMessage/>}
                         sortField={extractionJobs.length !== 0 ? 'dateStarted' : ''}
                         sortOrder={-1}
                         value={extractionJobs.map(job => mapJobToTableRow(job, workspace))}
                         style={{marginLeft: '0.5rem', marginRight: '0.5rem'}}>
                    <Column header='Dataset Name'
                            field='datasetNameDisplay'
                            sortable sortField='datasetName'
                            style={{
                              maxWidth: '8rem',
                              textOverflow: 'ellipsis',
                              overflow: 'hidden',
                              whiteSpace: 'nowrap'}}/>
                    <Column header='Status'
                            field='statusDisplay'
                            sortable sortField='statusOrdinal'/>
                    <Column header='Date Started'
                            field='dateStartedDisplay'
                            sortable sortField='dateStarted'/>
                    <Column header='Cost'
                            field='costDisplay'
                            sortable sortField='cost'/>
                    <Column header='Duration'
                            field='durationDisplay'
                            sortable sortField='duration'/>
                    <Column header='' field='menuJsx' />
                  </DataTable>
          }
        </CSSTransition>
      </SwitchTransition>
    </div>
  </div>;
});

