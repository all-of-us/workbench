import { useEffect, useState } from 'react';
import * as React from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { FileDetail } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckBox, TextArea } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { PurpleLabel } from 'app/pages/admin/workspace/admin-workspace';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { cond, reactStyles } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';

const MAX_NOTEBOOK_READ_SIZE_BYTES = 5 * 1000 * 1000; // see NotebooksServiceImpl

const NOTEBOOKS_DIRECTORY = 'notebooks';
const NOTEBOOKS_SUFFIX = '.ipynb';

const parseLocation = (file: FileDetail, bucket: string): string => {
  const prefixLength = bucket.length;
  const start = prefixLength + 1; // slash after bucket name
  const suffixPos = file.path.lastIndexOf(file.name);
  const end = suffixPos - 1; // slash before filename

  return file.path.substring(start, end);
};

const styles = reactStyles({
  accessReasonText: {
    maxWidth: '1000px',
    height: '4.5rem',
  },
  previewButton: {
    marginLeft: '20px',
    height: '1.5rem',
  },
  fileDetailsTable: {
    maxWidth: '1000px',
    marginTop: '1.5rem',
  },
});
interface NameCellProps {
  file: FileDetail;
  bucket: string;
  workspaceNamespace: string;
  accessReason: string;
}

const formatMB = (fileSize: number): string => {
  const mb = fileSize / 1000000.0;
  if (mb < 1.0) {
    return '<1';
  } else {
    return mb.toFixed(2);
  }
};
const NameCell = (props: NameCellProps) => {
  const [navigate] = useNavigation();
  const { file, bucket, workspaceNamespace, accessReason } = props;
  const filename = file.name.trim();

  const filenameSpan = () => <span>{filename}</span>;

  const fileTooLarge = () => (
    <FlexRow>
      {filenameSpan()}
      <TooltipTrigger
        content={`Files larger than ${formatMB(
          MAX_NOTEBOOK_READ_SIZE_BYTES
        )} MB are too large to preview`}
      >
        <Button style={styles.previewButton} disabled={true}>
          Preview
        </Button>
      </TooltipTrigger>
    </FlexRow>
  );

  const navigateToPreview = () =>
    navigate(
      ['admin', 'workspaces', workspaceNamespace, encodeURIComponent(filename)],
      { queryParams: { accessReason: accessReason } }
    );

  const fileWithPreviewButton = () => (
    <FlexRow>
      {filenameSpan()}
      <TooltipTrigger
        content='Please enter an access reason below'
        disabled={accessReason?.trim()}
      >
        <Button
          style={styles.previewButton}
          disabled={!accessReason?.trim()}
          onClick={navigateToPreview}
        >
          Preview
        </Button>
      </TooltipTrigger>
    </FlexRow>
  );

  // remove first check after RW-5626
  const isNotebook =
    NOTEBOOKS_DIRECTORY === parseLocation(file, bucket) &&
    filename.endsWith(NOTEBOOKS_SUFFIX);
  const isTooLargeNotebook =
    isNotebook && file.sizeInBytes > MAX_NOTEBOOK_READ_SIZE_BYTES;

  // if (tooLarge()) fileTooLarge();
  // else if (isNotebook()) fileWithPreviewButton();
  // else filenameSpan();
  return cond(
    [isTooLargeNotebook, fileTooLarge],
    [isNotebook, fileWithPreviewButton],
    filenameSpan
  );
};
interface FileDetailsProps {
  workspaceNamespace: string;
  bucket: string;
}

export const FileDetailsTable = (props: FileDetailsProps) => {
  const { workspaceNamespace, bucket } = props;

  interface TableEntry {
    location: string;
    rawName: string;
    nameCell: JSX.Element;
    size: string;
  }

  const [fileDetail, setFileDetail] = useState<Array<FileDetail>>();
  const [tableData, setTable] = useState<Array<TableEntry>>();
  const [loading, setLoading] = useState<boolean>();
  const [showOnlyAppFiles, setShowOnlyAppFiles] = useState<boolean>(true);

  const initTable = (
    fileDetails: Array<FileDetail>,
    accessReason: string = ''
  ): Array<TableEntry> => {
    return fileDetails
      .map((file) => {
        return {
          location: parseLocation(file, bucket),
          rawName: file.name,
          nameCell: (
            <NameCell
              file={file}
              bucket={bucket}
              workspaceNamespace={workspaceNamespace}
              accessReason={accessReason}
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
      showOnlyAppFiles
    );
    setFileDetail(files);
    setTable(initTable(files));
    setLoading(false);
  }

  useEffect(() => {
    setLoading(true);
    getWorkspaceFiles().finally(() => setLoading(false));
  }, [showOnlyAppFiles]);

  return (
    <div>
      <FlexRow>
        <CheckBox
          checked={!showOnlyAppFiles}
          onChange={() => {
            setShowOnlyAppFiles(!showOnlyAppFiles);
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
