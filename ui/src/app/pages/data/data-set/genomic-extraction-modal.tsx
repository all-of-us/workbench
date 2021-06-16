import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';

import {Button} from 'app/components/buttons';
import {ErrorMessage, WarningMessage} from 'app/components/messages';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import {dataSetApi} from 'app/services/swagger-fetch-clients';

import {TooltipTrigger} from 'app/components/popups';
import {genomicExtractionStore, updateGenomicExtractionStore, useStore} from 'app/utils/stores';
import {DataSet, GenomicExtractionJob, TerraJobStatus} from 'generated/fetch';
import * as moment from 'moment';

const {useState} = React;

const TimeAgoWithVerboseTooltip = (epoch) => {
  return <TooltipTrigger content={moment(epoch).format('MMMM Do YYYY, h:mm:ss a')}>
    <span style={{
      'textDecoration': 'underline',
      'textDecorationStyle': 'dotted'
    }}>
      {moment(epoch).fromNow()}
    </span>
  </TooltipTrigger>;
};

interface Props {
  dataSet: DataSet;
  workspaceNamespace: string;
  workspaceFirecloudName: string;
  closeFunction: Function;
  title?: string;
  cancelText?: string;
  confirmText?: string;
}

export const GenomicExtractionModal = ({
    dataSet, workspaceNamespace, workspaceFirecloudName, closeFunction, title, cancelText, confirmText}: Props) => {
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(true);

  const genomicExtractions = useStore(genomicExtractionStore);
  const extractsForWorkspace = genomicExtractions && genomicExtractions[workspaceNamespace] || [];
  const sortedExtractsForDataset: GenomicExtractionJob[] = fp.flow(
      fp.filter((extract: GenomicExtractionJob) => extract.datasetName === dataSet.name),
      // This, incidentally to the implementation of orderBy, puts falsey values at the front...
      // ... which is actually what we want, but it's kind of bad to rely on implementation detail
      fp.orderBy((extract: GenomicExtractionJob) => extract.completionTime, 'desc')
  )(extractsForWorkspace);

  useEffect(() => {
    if (!(workspaceNamespace in genomicExtractions)) {
      dataSetApi().getGenomicExtractionJobs(workspaceNamespace, workspaceFirecloudName)
        .then(resp => updateGenomicExtractionStore(workspaceNamespace, resp.jobs))
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [workspaceNamespace]);

  const mostRecentExtract = fp.head(sortedExtractsForDataset);
  const runningExtract = mostRecentExtract && mostRecentExtract.status === TerraJobStatus.RUNNING;
  const succeededExtract = mostRecentExtract && mostRecentExtract.status === TerraJobStatus.SUCCEEDED;
  const failedExtract = mostRecentExtract && mostRecentExtract.status === TerraJobStatus.FAILED;

  return <Modal loading={loading}>
    <ModalTitle style={{marginBottom: '0'}}>{ title || 'Launch VCF extraction' }</ModalTitle>
    <ModalBody>
      <TextColumn style={{gap: '0.5rem'}}>
        <span>
          Extraction will generate VCF files for the participants in this
          dataset which you can use in your analysis environment.
        </span>
        <span>
          Genomic data extraction will run in background and
          you’ll be notified when files are ready for analysis.
          VCF extraction will incur cloud compute costs.
        </span>
      </TextColumn>
    </ModalBody>
    {(runningExtract || succeededExtract || failedExtract) &&
      <WarningMessage iconSize={30} iconPosition={'top'} data-test-id='extract-warning'>
        {runningExtract && <React.Fragment>
          An extraction is currently running for this dataset; it was started {TimeAgoWithVerboseTooltip(mostRecentExtract.submissionDate)}.
        </React.Fragment>}
        {succeededExtract && <React.Fragment>
          VCF file(s) already exist for this dataset.
          Last extracted files for this dataset: {TimeAgoWithVerboseTooltip(mostRecentExtract.completionTime)}.
          The file is located in the Workspace storage panel.
        </React.Fragment>}
        {failedExtract && <React.Fragment>
          Last time a VCF extract was attempted for this workflow, it failed.
          The workflow failed {TimeAgoWithVerboseTooltip(mostRecentExtract.completionTime)}.
        </React.Fragment>}
      </WarningMessage>
    }
    {error &&
     <ErrorMessage iconSize={16}>
       Failed to launch extraction, please try again.
     </ErrorMessage>}
    <ModalFooter>
      <Button type='secondary' onClick={() => closeFunction()}>
        { cancelText || 'Cancel' }
      </Button>
      <Button data-test-id='extract-button'
              disabled={loading}
              style={{marginLeft: '0.5rem'}}
              onClick={async() => {
                setLoading(true);
                try {
                  const job = await dataSetApi().extractGenomicData(
                    workspaceNamespace, workspaceFirecloudName, dataSet.id);
                  updateGenomicExtractionStore(
                    workspaceNamespace,
                    fp.concat(
                      genomicExtractionStore.get()[workspaceNamespace] || [],
                      job
                    )
                  );
                  closeFunction();
                } catch (e) {
                  setError(true);
                }
                setLoading(false);
              }}>
        { confirmText || 'Extract' }
      </Button>
    </ModalFooter>
  </Modal>;
};
