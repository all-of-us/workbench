import React, { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { DataSet, Domain } from 'generated/fetch';

import { environment } from 'environments/environment';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import { GenomicExtractionModal } from 'app/pages/data/data-set/genomic-extraction-modal';
import { dataSetApi } from 'app/services/swagger-fetch-clients';
import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { getAccessToken } from 'app/utils/authentication';
import { findCdrVersion } from 'app/utils/cdr-versions';
import {
  ExportResources,
  useExitActionListener,
  useExportListener,
  useNavigation,
} from 'app/utils/navigation';
import { authStore, serverConfigStore, useStore } from 'app/utils/stores';

enum ModalState {
  None,
  Create,
  Export,
  Extract,
}

export const TanagraContainer = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(({ cdrVersionTiersResponse, hideSpinner, workspace }) => {
  const iframe = useRef<HTMLIFrameElement>();
  const [navigate] = useNavigation();
  const { 0: splat } = useParams<{ 0: string }>();
  const { auth } = useStore(authStore);
  const { cdrVersionId, namespace, terraName } = workspace;
  const { bigqueryDataset } = findCdrVersion(
    cdrVersionId,
    cdrVersionTiersResponse
  );
  const [modalState, setModalState] = useState(ModalState.None);
  const [exportIds, setExportIds] = useState<ExportResources>();
  const [exportDataset, setExportDataset] = useState<DataSet>();
  // This number is passed to the iframe to trigger a reload when it increments, forcing it to use the new token
  const [localReload, setLocalReload] = useState(0);
  const tanagraUrl = `${
    serverConfigStore.get().config.tanagraBaseUrl
  }/tanagra/ui#/tanagra/underlays/aou${bigqueryDataset}/studies/${namespace}/${splat}${
    environment.tanagraLocalAuth ? `/?token=${getAccessToken()}` : ''
  }`;

  useEffect(() => {
    if (environment.tanagraLocalAuth) {
      if (localReload > 0) {
        iframe.current.src = iframe.current.src.replace(
          /token=[^&]+/,
          'token=' + getAccessToken()
        );
      }
      setLocalReload((prevState) => prevState + 1);
    } else {
      localStorage.setItem('tanagraAccessToken', getAccessToken());
      // Temp workaround to force the iframe to use the new token. Will need a long term fix for this in core Tanagra
      iframe.current.contentWindow.location.reload();
    }
  }, [auth]);

  useExitActionListener(() => {
    // Navigate to Data tab when exiting Tanagra iframe
    navigate(['workspaces', namespace, terraName, 'data']);
  });

  useExportListener(async (exportResourceIds) => {
    setExportIds(exportResourceIds);
    if (exportResourceIds.predefinedCriteria.includes('_short_read_wgs')) {
      try {
        const newDataset = await dataSetApi().createDataSet(
          workspace.namespace,
          workspace.terraName,
          {
            name: `Tanagra export test ${Date.now()}`,
            domainValuePairs: [
              {
                domain: Domain.WHOLE_GENOME_VARIANT,
                value: 'VCF Files',
              },
            ],
            conceptSetIds: [],
            cohortIds: [],
            prePackagedConceptSet: [],
          }
        );
        if (newDataset) {
          setExportDataset(newDataset);
          setModalState(ModalState.Extract);
        }
      } catch (error) {
        console.error(error);
      }
    } else {
      setExportDataset(null);
      setModalState(ModalState.Export);
    }
  });

  return (
    <div
      style={{
        height: '95vh',
      }}
    >
      <iframe
        key={localReload}
        ref={iframe}
        onLoad={() => hideSpinner()}
        style={{
          border: 0,
          height: '100%',
          width: '100%',
        }}
        src={tanagraUrl}
      />
      {!!exportIds && modalState === ModalState.Export && (
        <ExportDatasetModal
          {...{ workspace }}
          dataset={exportDataset}
          closeFunction={() => setExportIds(undefined)}
          tanagraCohortIds={exportIds.cohortIds}
          tanagraFeatureSetIds={exportIds.featureSetIds}
          tanagraAllParticipantsCohort={exportIds.allParticipantsCohort}
          tanagraHasWGS={exportIds?.predefinedCriteria?.includes(
            '_short_read_wgs'
          )}
        />
      )}
      {!!exportIds && modalState === ModalState.Extract && (
        <GenomicExtractionModal
          dataSet={exportDataset}
          workspaceNamespace={namespace}
          workspaceTerraName={terraName}
          title={'Would you like to extract genomic variant data as VCF files?'}
          cancelText={'Skip'}
          confirmText={'Extract & Continue'}
          closeFunction={() => setModalState(ModalState.Export)}
          tanagraCohortIds={exportIds.cohortIds}
          tanagraFeatureSetIds={exportIds.featureSetIds}
          tanagraAllParticipantsCohort={exportIds.allParticipantsCohort}
          tanagraEnabled
        />
      )}
    </div>
  );
});
