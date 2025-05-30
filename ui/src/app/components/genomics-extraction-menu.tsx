import * as React from 'react';
import { useState } from 'react';
import { faBan, faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { GenomicExtractionJob, TerraJobStatus } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { MenuItem } from 'app/components/buttons';
import { CopySnippetModal } from 'app/components/copy-snippet-modal';
import { PopupTrigger } from 'app/components/popups';
import { dataSetApi } from 'app/services/swagger-fetch-clients';
import colors, { addOpacity } from 'app/styles/colors';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

import { FlexRow } from './flex';
import { LocationArrowIcon } from './icons';

const styles = {
  hr: {
    border: '0',
    borderTop: '1px solid ' + addOpacity(colors.dark, 0.2),
    borderBottom: '1px solid ' + colors.white,
    marginTop: '0',
    marginBottom: '0',
  },
  menuItem: {
    height: null,
    paddingTop: '0.375rem',
    paddingBottom: '0.375rem',
  },
};

export interface GenomicsExtractionMenuProps {
  job: GenomicExtractionJob;
  workspace: WorkspaceData;
  onMutate: () => void;
}

export const GenomicsExtractionMenu = ({
  job,
  workspace,
  onMutate,
}: GenomicsExtractionMenuProps) => {
  const isRunning = job.status === TerraJobStatus.RUNNING;
  const canWrite = WorkspacePermissionsUtil.canWrite(workspace.accessLevel);
  const tooltip = cond(
    [isRunning && canWrite, () => ''],
    [
      isRunning && !canWrite,
      () => 'You do not have permission to modify this workspace',
    ],
    [!isRunning, () => 'Extraction job is not currently running']
  );

  const [showModal, setShowModal] = useState(false);

  return (
    <React.Fragment>
      <PopupTrigger
        side='bottom-left'
        closeOnClick
        content={
          <React.Fragment>
            <MenuItem
              style={{ padding: '0.375rem 0.5rem' }} // different styling because we use a different icon type here
              disabled={
                !(job.status === TerraJobStatus.SUCCEEDED && job.outputDir)
              }
              onClick={() => setShowModal(true)}
            >
              <FlexRow>
                <LocationArrowIcon
                  style={{
                    height: '13px',
                    marginTop: '5px',
                    marginRight: '3px',
                  }}
                />
                <div>View Path</div>
              </FlexRow>
            </MenuItem>
            <hr style={styles.hr} />
            <MenuItem
              style={styles.menuItem}
              faIcon={faBan}
              disabled={!isRunning || !canWrite}
              onClick={async () => {
                await dataSetApi().abortGenomicExtractionJob(
                  workspace.namespace,
                  workspace.terraName,
                  job.genomicExtractionJobId.toString()
                );
                onMutate();
              }}
              tooltip={tooltip}
            >
              Abort Extraction
            </MenuItem>
          </React.Fragment>
        }
      >
        <FontAwesomeIcon
          icon={faEllipsisV}
          title='Genomic Extractions Action Menu'
          style={{
            color: colors.accent,
            fontSize: '1.05rem',
            marginLeft: 0,
            paddingRight: 0,
            display: 'block',
            cursor: 'pointer',
          }}
        />
      </PopupTrigger>
      {showModal && (
        <CopySnippetModal
          title={`GCS Path for ${job.datasetName} VCFs`}
          copyText={job.outputDir}
          closeFunction={() => setShowModal(false)}
        />
      )}
    </React.Fragment>
  );
};
