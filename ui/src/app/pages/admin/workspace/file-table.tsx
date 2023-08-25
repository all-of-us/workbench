import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { InputSwitch } from 'primereact/inputswitch';

import { FileDetail } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TextArea } from 'app/components/inputs';
import { Spinner } from 'app/components/spinners';
import { parseLocation } from 'app/pages/analysis/util';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { JUPYTER_FILE_EXT } from 'app/utils/constants';
import { reactStyles } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';
import { reactStyles } from 'app/utils';

import { FilenameCell } from './filename-cell';
import { PurpleLabel } from './workspace-info-field';

const styles = reactStyles({
  accessReasonText: {
    maxWidth: '1000px',
    height: '4.5rem',
  },
  fileDetailsTable: {
    maxWidth: '1000px',
    marginTop: '1.5rem',
  },
});

export const formatMB = (fileSize: number): string => {
  const mb = fileSize / 1000000.0;
  if (mb < 1.0) {
    return '<1';
  } else {
    return mb.toFixed(2);
  }
};

interface Props {
  workspaceNamespace: string;
  storageBucketPath: string;
}
export const FileTable = (props: Props) => {
  const { workspaceNamespace, storageBucketPath } = props;

  interface TableEntry {
    location: string;
    rawName: string;
    nameCell: JSX.Element;
    size: string;
  }

  const [fileDetail, setFileDetail] = useState<Array<FileDetail>>();
  const [tableData, setTable] = useState<Array<TableEntry>>();
  const [loading, setLoading] = useState<boolean>();
  const [showAllFiles, setShowAllFiles] = useState<boolean>(false);

  const initTable = (
    fileDetails: Array<FileDetail>,
    accessReason: string = ''
  ): Array<TableEntry> => {
    return fileDetails
      .map((file) => {
        return {
          location: parseLocation(file, storageBucketPath),
          rawName: file.name,
          nameCell: (
            <FilenameCell
              {...{ file, storageBucketPath, workspaceNamespace, accessReason }}
            />
          ),
          size: formatMB(file.sizeInBytes),
        };
      })
      .sort((a, b) => {
        const locationComparison = a.location.localeCompare(b.location);
        return locationComparison === 0
          ? a.rawName.localeCompare(b.rawName)
          : locationComparison;
      });
  };

  const updateFileAccessReason = (
    fileAccessReason: string
  ): Array<TableEntry> => {
    return initTable(fileDetail, fileAccessReason);
  };

  async function getWorkspaceFiles() {
    setLoading(true);
    const files = await workspaceAdminApi().listFiles(
      workspaceNamespace,
      !showAllFiles
    );
    setFileDetail(files);
    setTable(initTable(files));
    setLoading(false);
  }

  useEffect(() => {
    setLoading(true);
    getWorkspaceFiles().finally(() => setLoading(false));
  }, [showAllFiles]);

  return (
    <div>
      <FlexRow>
        <InputSwitch
          checked={showAllFiles}
          onChange={() => {
            setShowAllFiles(!showAllFiles);
          }}
          style={{
            marginRight: '1.5rem',
            marginTop: '1.2rem',
            marginLeft: '0.5rem',
          }}
        />
        <h4>Show All Files</h4>
      </FlexRow>
      <FlexColumn>
        {loading ? (
          <Spinner />
        ) : (
          <DataTable
            paginator
            scrollable
            data-test-id='object-details-table'
            value={tableData}
            style={styles.fileDetailsTable}
            rows={100}
            breakpoint='0px'
            paginatorTemplate='CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink'
            currentPageReportTemplate='Showing {first} to {last} of {totalRecords} entries'
          >
            <Column field='location' header='Location' />
            <Column field='nameCell' header='Filename' />
            <Column
              field='size'
              header='File size (MB)'
              style={{ textAlign: 'right' }}
            />
          </DataTable>
        )}
        <PurpleLabel>
          To preview notebooks, enter Access Reason (for auditing purposes)
        </PurpleLabel>
        <TextArea
          style={styles.accessReasonText}
          onChange={(v) => setTable(updateFileAccessReason(v))}
        />
      </FlexColumn>
    </div>
  );
};
