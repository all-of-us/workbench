import * as React from 'react';
import * as fp from 'lodash/fp';

import { DataSet, GenomicExtractionJob, TerraJobStatus } from 'generated/fetch';

import { Button, StyledExternalLink } from 'app/components/buttons';
import { ErrorMessage, WarningMessage } from 'app/components/messages';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { TextColumn } from 'app/components/text-column';
import { dataSetApi } from 'app/services/swagger-fetch-clients';
import { useGenomicExtractionJobs } from 'app/utils/stores';
import { supportUrls } from 'app/utils/zendesk';
import moment from 'moment';

const { useState } = React;

const TimeAgoWithVerboseTooltip = (epoch) => {
  return (
    <TooltipTrigger content={moment(epoch).format('MMMM Do YYYY, h:mm:ss a')}>
      <span
        style={{
          textDecoration: 'underline',
          textDecorationStyle: 'dotted',
        }}
      >
        {moment(epoch).fromNow()}
      </span>
    </TooltipTrigger>
  );
};

interface Props {
  dataSet?: DataSet;
  workspaceNamespace: string;
  workspaceTerraName: string;
  closeFunction: Function;
  title?: string;
  cancelText?: string;
  confirmText?: string;
  tanagraCohortIds?: string[];
  tanagraFeatureSetIds?: string[];
  tanagraAllParticipantsCohort?: boolean;
  tanagraEnabled?: boolean;
}

export const GenomicExtractionModal = ({
  dataSet,
  workspaceNamespace,
  workspaceTerraName,
  closeFunction,
  title,
  cancelText,
  confirmText,
  tanagraCohortIds,
  tanagraFeatureSetIds,
  tanagraAllParticipantsCohort,
  tanagraEnabled,
}: Props) => {
  const [launching, setLaunching] = useState(false);
  const [error, setError] = useState<{ status: number; message: string }>(null);
  const isClientError = error && 400 <= error.status && error.status < 500;

  const { data: jobs, mutate } = useGenomicExtractionJobs(
    workspaceNamespace,
    workspaceTerraName
  );
  const mostRecentExtract: GenomicExtractionJob = fp.flow(
    fp.filter(
      (extract: GenomicExtractionJob) =>
        extract.datasetName === (dataSet?.name ?? 'Tanagra export')
    ),
    // This, incidentally to the implementation of orderBy, puts falsey values at the front...
    // ... which is actually what we want, but it's kind of bad to rely on implementation detail
    fp.orderBy(
      (extract: GenomicExtractionJob) => extract.completionTime,
      'desc'
    ),
    fp.head
  )(jobs || []);

  const loading = !jobs || launching;

  const runningExtract =
    mostRecentExtract && mostRecentExtract.status === TerraJobStatus.RUNNING;
  const succeededExtract =
    mostRecentExtract && mostRecentExtract.status === TerraJobStatus.SUCCEEDED;
  const failedExtract =
    mostRecentExtract && mostRecentExtract.status === TerraJobStatus.FAILED;

  const onExtractClick = async () => {
    setLaunching(true);
    try {
      const job = tanagraEnabled
        ? await dataSetApi().extractTanagraGenomicData(
            workspaceNamespace,
            workspaceTerraName,
            {
              cohortIds: tanagraCohortIds ?? [],
              featureSetIds: tanagraFeatureSetIds ?? [],
              datasetId: dataSet?.id,
              allParticipants: tanagraAllParticipantsCohort,
            }
          )
        : await dataSetApi().extractGenomicData(
            workspaceNamespace,
            workspaceTerraName,
            dataSet.id
          );
      mutate(fp.concat(jobs, job));
      closeFunction();
    } catch (e) {
      const errJson = (await e.json().catch(() => {})) || {};
      setError({
        status: e.status,
        message: errJson.message || 'unknown error',
      });
    }
    setLaunching(false);
  };

  return (
    <Modal loading={loading}>
      <ModalTitle style={{ marginBottom: '0' }}>
        {title || 'Launch VCF extraction'}
      </ModalTitle>
      <ModalBody>
        <TextColumn style={{ gap: '0.75rem' }}>
          <span>
            Extraction will generate VCF files for the participants in this
            dataset which you can use in your analysis environment.
          </span>
          <span>
            Genomic data extraction will run in background and you’ll be
            notified when files are ready for analysis.
          </span>
          <span>
            <span style={{ fontWeight: 600 }}>
              Note: VCF extraction will incur cloud cost{' '}
            </span>
            . Extraction typically costs{' '}
            <span style={{ fontWeight: 600 }}>$.02 per extracted sample</span>,
            but costs may vary.{' '}
            <StyledExternalLink
              href={supportUrls.genomicExtraction}
              target='_blank'
            >
              Find out more about genomic extraction and costs
            </StyledExternalLink>
            .
          </span>
        </TextColumn>
      </ModalBody>
      {(() => {
        if (error) {
          return (
            <ErrorMessage
              iconSize={30}
              iconPosition={'top'}
              data-test-id='extract-error'
            >
              Failed to launch extraction: {error.message}.
            </ErrorMessage>
          );
        } else if (runningExtract || succeededExtract || failedExtract) {
          return (
            <WarningMessage
              iconSize={30}
              iconPosition={'top'}
              data-test-id='extract-warning'
            >
              {runningExtract && (
                <React.Fragment>
                  An extraction is currently running for this dataset; it was
                  started{' '}
                  {TimeAgoWithVerboseTooltip(mostRecentExtract.submissionDate)}.
                </React.Fragment>
              )}
              {succeededExtract && (
                <React.Fragment>
                  VCF file(s) already exist for this dataset. Last extracted
                  files for this dataset:{' '}
                  {TimeAgoWithVerboseTooltip(mostRecentExtract.completionTime)}.
                  Details can be found in the Genomic Extraction History panel.
                </React.Fragment>
              )}
              {failedExtract && (
                <React.Fragment>
                  Last time a VCF extract was attempted for this workflow, it
                  failed. The workflow failed{' '}
                  {TimeAgoWithVerboseTooltip(mostRecentExtract.completionTime)}.
                </React.Fragment>
              )}
            </WarningMessage>
          );
        } else {
          return <React.Fragment />;
        }
      })()}
      <ModalFooter>
        <Button type='secondary' onClick={() => closeFunction()}>
          {cancelText || 'Cancel'}
        </Button>
        <Button
          data-test-id='extract-button'
          disabled={loading || isClientError}
          style={{ marginLeft: '0.75rem' }}
          onClick={onExtractClick}
        >
          {confirmText || 'Extract'}
        </Button>
      </ModalFooter>
    </Modal>
  );
};
