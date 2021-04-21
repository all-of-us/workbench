import * as React from 'react';

import {Button} from 'app/components/buttons';
import {ErrorMessage} from 'app/components/messages';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {dataSetApi} from 'app/services/swagger-fetch-clients';

import {DataSet} from 'generated/fetch';

const {useState} = React;


interface Props {
  dataSet: DataSet;
  workspaceNamespace: string;
  workspaceFirecloudName: string;
  closeFunction: Function;
}

export const GenomicExtractionModal = ({
    dataSet, workspaceNamespace, workspaceFirecloudName, closeFunction}: Props) => {
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(false);
  return <Modal loading={loading}>
    <ModalTitle>Launch VCF extraction</ModalTitle>
    <ModalBody>
      Genomic data extraction will run in background and
      you’ll be notified when files are ready for analysis.
      VCF extraction will incur cloud compute costs.
    </ModalBody>
    {error &&
     <ErrorMessage iconSize={16}>
       Failed to launch extraction, please try again.
     </ErrorMessage>}
    <ModalFooter>
      <Button type='secondary' onClick={() => closeFunction()}>Cancel</Button>
      <Button data-test-id='extract-button'
              disabled={loading}
              style={{marginLeft: '0.5rem'}}
              onClick={async() => {
                setLoading(true);
                try {
                  await dataSetApi().extractGenomicData(
                    workspaceNamespace, workspaceFirecloudName, dataSet.id);
                  closeFunction();
                } catch (e) {
                  setError(true);
                }
                setLoading(false);
              }}>
        Extract
      </Button>
    </ModalFooter>
  </Modal>;
};
