import {faCheckCircle, faEllipsisV, faExclamationTriangle} from '@fortawesome/free-solid-svg-icons';
import {faSyncAlt} from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

import {withCurrentWorkspaceContext} from 'app/utils';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';
import {useContext, useEffect, useState} from 'react';
import {CSSTransition, SwitchTransition} from 'react-transition-group';
import {GenomicExtractionJob, TerraJobStatus} from '../../generated/fetch';
import {dataSetApi} from '../services/swagger-fetch-clients';
import colors from '../styles/colors';
import {formatUsd} from '../utils/numbers';
import {Spinner} from './spinners';

function getIconConfigForStatus(status: TerraJobStatus) {
  if (status === TerraJobStatus.RUNNING) {
    return {
      icon: faSyncAlt,
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
    statusJsx: <FontAwesomeIcon
      icon={iconConfig.icon}
      style={{
        ...iconConfig.style,
        fontSize: '.7rem',
        marginLeft: '.4rem',
        display: 'block'
      }}/>,
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

function mapJobsToTableRows(jobs: Array<GenomicExtractionJob>) {
  return jobs.map(job => mapJobToTableRow(job));
  // return mockData();
}

const [workspaceWrapper, workspaceContext] = withCurrentWorkspaceContext();

export const GenomicsExtractionTable = fp.flow(workspaceWrapper)(() => {
  const workspace = useContext(workspaceContext);
  const [extractionJobs, setExtractionJobs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    dataSetApi().getGenomicExtractionJobs(workspace.namespace, workspace.id).then(resp => {
      setExtractionJobs(resp.jobs.concat(mockApiData()));
      setIsLoading(false);
    });
  }, [workspace]);

  console.log(extractionJobs);

  return <div id='extraction-data-table-container'>
    <SwitchTransition>
      <CSSTransition
        key={isLoading}
        classNames="switch-transition-container"
        addEndListener={(node, done) => node.addEventListener('transitionend', done, false)}>
          {isLoading
            ? <Spinner style={{display: 'block', margin: '3rem auto'}}/>
            : <DataTable value={mapJobsToTableRows(extractionJobs)} autoLayout sortField='dateStarted' sortOrder={-1}>
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
;;
