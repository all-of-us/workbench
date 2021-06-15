import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {ErrorMessage, WarningMessage} from 'app/components/messages';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import {dataSetApi} from 'app/services/swagger-fetch-clients';

import {TooltipTrigger} from 'app/components/popups';
import {genomicExtractionStore, updateGenomicExtractionStore, useStore} from 'app/utils/stores';
import {DataSet, GenomicExtractionJob, TerraJobStatus} from 'generated/fetch';
import * as moment from 'moment';
import {useEffect} from 'react';

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
  const extractsForDataset = fp.filter((extract: GenomicExtractionJob) => extract.datasetName === dataSet.name, extractsForWorkspace);

  useEffect(() => {
    if (!(workspaceNamespace in genomicExtractions)) {
      dataSetApi().getGenomicExtractionJobs(workspaceNamespace, workspaceFirecloudName)
        .then(resp => updateGenomicExtractionStore(workspaceNamespace, resp.jobs))
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [workspaceNamespace]);

  const runningExtract = fp.flow(
    fp.orderBy((extract: GenomicExtractionJob) => extract.submissionDate, 'desc'),
    fp.find((extract: GenomicExtractionJob) => extract.status === TerraJobStatus.RUNNING)
  )(extractsForDataset);
  const succeededExtract = fp.flow(
    fp.orderBy((extract: GenomicExtractionJob) => extract.completionTime, 'desc'),
    fp.find((extract: GenomicExtractionJob) => extract.status === TerraJobStatus.SUCCEEDED)
  )(extractsForDataset);

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
    {runningExtract &&
      <WarningMessage iconSize={30} iconPosition={'top'} data-test-id='running-extract-warning'>
        An extraction is currently running for this dataset; it was started {TimeAgoWithVerboseTooltip(runningExtract.submissionDate)}
      </WarningMessage>
    }
    {!runningExtract && succeededExtract &&
      <WarningMessage iconSize={30} iconPosition={'top'} data-test-id='preexisting-extract-warning'>
          VCF file(s) already exist for this dataset.
          Last extracted files for this dataset: {TimeAgoWithVerboseTooltip(succeededExtract.completionTime)}.
          The file is located in the Workspace storage panel.
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
