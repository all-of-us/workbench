import {faBan, faEllipsisV, faTrash} from '@fortawesome/free-solid-svg-icons';

import {faLocationCircle} from '@fortawesome/pro-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity} from 'app/styles/colors';
import {switchCase} from 'app/utils';
import {genomicExtractionStore, updateGenomicExtractionStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {GenomicExtractionJob, TerraJobStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {useState} from 'react';
import * as React from 'react';
import {MenuItem} from './buttons';
import {CopySnippetModal} from './copy-snippet-modal';
import {PopupTrigger} from './popups';

const styles = {
  hr: {
    border: '0',
    borderTop: '1px solid ' + addOpacity(colors.dark, 0.2),
    borderBottom: '1px solid ' + colors.white,
    marginTop: '0',
    marginBottom: '0'
  },
  menuItem: {
    height: null,
    paddingTop: '0.25rem',
    paddingBottom: '0.25rem'
  }
};

interface Props {
  job: GenomicExtractionJob;
  workspace: WorkspaceData;
}

export const GenomicsExtractionMenu = ({job, workspace}: Props) => {
  const isRunning = job.status === TerraJobStatus.RUNNING;
  const canWrite = WorkspacePermissionsUtil.canWrite(workspace.accessLevel);
  const tooltip = switchCase({r: isRunning, w: canWrite},
      [{r: true, w: true}, () => ''],
      [{r: true, w: false}, () => 'You do not have permission to modify this workspace'],
      [{r: false, w: true}, () => 'Extraction job is not currently running'],
      [{r: false, w: false}, () => 'Extraction job is not currently running']
  );

  const [modalState, setModalState] = useState(false);

  return (
    <React.Fragment>
      <PopupTrigger
        side='bottom-left'
        closeOnClick
        content={
          <React.Fragment>
            <MenuItem
              style={styles.menuItem}
              faIcon={faLocationCircle}
              disabled={!(job.status === TerraJobStatus.SUCCEEDED && job.outputDir)}
              onClick={() => {
                console.log('hi?');
                setModalState(true);
              }}
            >
              View Path
            </MenuItem>
            <hr style={styles.hr}/>
            <MenuItem
              style={styles.menuItem}
              faIcon={faBan}
              disabled={!isRunning || !canWrite}
              onClick={() => {
                dataSetApi().abortGenomicExtractionJob(workspace.namespace, workspace.id, job.genomicExtractionJobId.toString());
                const workspaceJobs = genomicExtractionStore.get()[workspace.namespace];
                const abortedJobIndex = fp.findIndex(j => j.genomicExtractionJobId === job.genomicExtractionJobId, workspaceJobs);
                workspaceJobs[abortedJobIndex].status = TerraJobStatus.ABORTING;
                updateGenomicExtractionStore(workspace.namespace, workspaceJobs);
              }}
              tooltip={tooltip}
            >
              Abort Extraction
            </MenuItem>
            <hr style={styles.hr}/>
            <MenuItem
              style={styles.menuItem}
              faIcon={faTrash}
              disabled
              onClick={() => {}}
            >
              Delete Extract
            </MenuItem>

          </React.Fragment>}
      >
        <FontAwesomeIcon
          icon={faEllipsisV}
          style={{
            color: colors.accent,
            fontSize: '.7rem',
            marginLeft: 0,
            paddingRight: 0,
            display: 'block',
            cursor: 'pointer'
          }}
        />
      </PopupTrigger>
      {modalState &&
        <CopySnippetModal
          title={`GCS Path for ${job.datasetName} VCFs`}
          copyText={job.outputDir}
          closeFunction={() => setModalState(false)}
        />
      }
    </React.Fragment>
  );
};
