import {faCheckCircle, faEllipsisV, faExclamationTriangle} from '@fortawesome/free-solid-svg-icons';
import {faSyncAlt} from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {withCurrentWorkspaceContext} from 'app/utils';
import {formatUsd} from 'app/utils/numbers';
import {WorkspaceData} from 'app/utils/workspace-data';
import {GenomicExtractionJob, TerraJobStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';
import {Context, useContext, useEffect, useState} from 'react';
import {CSSTransition, SwitchTransition} from 'react-transition-group';
import {TextColumn} from './text-column';

function getIconConfigForStatus(status: TerraJobStatus) {
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
}

function formatDatetime(timeEpoch: number) {
  const timeMoment = moment(timeEpoch);
  const isToday = moment().isSame(timeMoment, 'day');
  const momentFormat = (isToday ? '[Today]' : 'MMM D, YYYY') + ' [at] h:mm a';
  return timeMoment.format(momentFormat);
}

function formatDuration(durationMoment) {
  const hours = Math.floor(durationMoment.asHours());
  const minStr = Math.floor(durationMoment.minutes()) + ' min';

  return (hours > 0 ? hours + ' hr, ' : '') + minStr;
}

const MissingCell = () => <span style={{fontSize: '.4rem'}}>&mdash;</span>;

function mapJobToTableRow(job: GenomicExtractionJob) {
  const iconConfig = getIconConfigForStatus(job.status);
  console.log(job.datasetName);
  const durationMoment = job.completionTime && moment.duration(moment(job.completionTime).diff(moment(job.submissionDate)));

  return {
    datasetName: job.datasetName,
    datasetNameDisplay:
      <span style={{opacity: job.status === TerraJobStatus.RUNNING ? .5 : 1}}>
        {job.datasetName}
      </span>,
    status: job.status,
    statusJsx: <TooltipTrigger content={iconConfig.iconTooltip}>
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
    costDisplay: !!job.cost ? formatUsd(job.cost) : <MissingCell/>,
    menuJsx: <FontAwesomeIcon
      icon={faEllipsisV}
      style={{
        color: colors.accent,
        fontSize: '.7rem',
        marginLeft: 0,
        paddingRight: 0,
        display: 'block'
      }}/>,
  };
}

const EmptyTableMessage = () => <TextColumn style={{fontSize: '0.5rem', paddingTop: '0.5rem'}}>
  <span>This will be the location to find any extracted genomics files you may need for your research.</span>
  <span>Genomic extractions can be created once you have a dataset that contains genomics data.</span>
</TextColumn>;

const [workspaceWrapper, workspaceContext]: [any, Context<WorkspaceData>] = withCurrentWorkspaceContext();

export const GenomicsExtractionTable = fp.flow(workspaceWrapper)(() => {
  const workspace = useContext(workspaceContext);
  const [extractionJobs, setExtractionJobs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    dataSetApi().getGenomicExtractionJobs(workspace.namespace, workspace.id).then(resp => {
      setExtractionJobs(resp.jobs
        .concat(mockApiData())
      );
      setIsLoading(false);
    });
  }, [workspace]);

  return <div id='extraction-data-table-container'>
    <div className='slim-scroll-bar'>
      <SwitchTransition>
        <CSSTransition
          key={isLoading}
          classNames='switch-transition-container'
          addEndListener={(node, done) => node.addEventListener('transitionend', done, false)}>
          {isLoading
            ? <Spinner style={{display: 'block', margin: '3rem auto'}}/>
            : <DataTable autoLayout
                         emptyMessage={<EmptyTableMessage/>}
                         sortField={extractionJobs.length !== 0 ? 'dateStarted' : ''}
                         sortOrder={-1}
                         value={extractionJobs.map(job => mapJobToTableRow(job))}
                         style={{marginLeft: '0.5rem', marginRight: '0.5rem'}}
            >
              <Column field='datasetNameDisplay' header='Dataset Name' sortable sortField='datasetName'
                      style={{maxWidth: '8rem', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap'}}/>
              <Column field='statusJsx' header='Status' sortable sortField='status'/>
              <Column field='dateStartedDisplay' header='Date Started' sortable sortField='dateStarted'/>
              <Column field='costDisplay' header='Cost' sortable sortField='cost'/>
              <Column field='durationDisplay' header='Duration' sortable sortField='duration'/>
              <Column field='menuJsx' header=''/>
            </DataTable>
          }
        </CSSTransition>
      </SwitchTransition>
    </div>
  </div>;
});

function mockApiData() {
    return [{
      datasetName:  'My favorite dataset',
        status: 'RUNNING',
        submissionDate: 1618607580000
    }, {
        datasetName:  'This is a long datasettttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt',
          status: 'FAILED',
          cost: .48,
          submissionDate: 1618096380000,
          completionTime: 1618097400000, // should be 17m later
        }, {
        datasetName:  'This is another dataset',
          status: 'SUCCEEDED',
          cost: 521.20,
          submissionDate: 1612528200000,
          completionTime: 1612591320000, // should be quite a few hours after but less than 24
        }, {
        datasetName:  'Long job',
          status: 'SUCCEEDED',
          cost: 521.20,
          submissionDate: 1612591320000,
          completionTime: 1612832520000,
        }];
  }
