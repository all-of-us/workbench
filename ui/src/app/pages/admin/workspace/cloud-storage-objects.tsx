import * as React from 'react';

import { AdminWorkspaceCloudStorageCounts } from 'generated/fetch';

import colors from 'app/styles/colors';

import { FileDetailsTable, formatMB } from './file-table';
import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspaceNamespace: string;
  cloudStorage: AdminWorkspaceCloudStorageCounts;
}
export const CloudStorageObjects = ({
  workspaceNamespace,
  cloudStorage,
}: Props) => {
  const {
    storageBucketPath,
    notebookFileCount,
    nonNotebookFileCount,
    storageBytesUsed,
  } = cloudStorage;
  return (
    <>
      <h3>Cloud Storage Objects</h3>
      <div className='cloud-storage-objects' style={{ marginTop: '1.5rem' }}>
        <div
          style={{
            color: colors.warning,
            fontWeight: 'bold',
            maxWidth: '1000px',
          }}
        >
          NOTE: if there are more than ~1000 files in the bucket, these counts
          and the table below may be incomplete because we process only a single
          page of storage list results.
        </div>
        <WorkspaceInfoField labelText='GCS bucket path'>
          {storageBucketPath}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='# of Workbench-managed notebook files'>
          {notebookFileCount}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='# of other files'>
          {nonNotebookFileCount}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='Storage used (MB)'>
          {formatMB(storageBytesUsed)}
        </WorkspaceInfoField>
      </div>
      <FileDetailsTable
        {...{ workspaceNamespace }}
        bucket={storageBucketPath}
      />
    </>
  );
};
