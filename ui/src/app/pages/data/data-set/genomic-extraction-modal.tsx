import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {ErrorMessage} from 'app/components/messages';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import {dataSetApi} from 'app/services/swagger-fetch-clients';

import {genomicExtractionStore, updateGenomicExtractionStore} from 'app/utils/stores';
import {DataSet} from 'generated/fetch';

const {useState} = React;

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
  const [loading, setLoading] = useState(false);
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
