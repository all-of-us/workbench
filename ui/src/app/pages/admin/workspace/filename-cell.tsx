import { FileDetail } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import {
  getAppInfoFromFileName,
  parseDirectory,
} from 'app/pages/analysis/util';
import { reactStyles } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';

import { formatMB } from './file-table';

const MAX_NOTEBOOK_READ_SIZE_BYTES = 5e6; // 5 MB, see NotebooksServiceImpl
const NOTEBOOKS_DIRECTORY = 'notebooks';

const styles = reactStyles({
  previewButton: {
    marginLeft: '20px',
    height: '1.5rem',
  },
});

interface Props {
  file: FileDetail;
  storageBucketPath: string;
  workspaceNamespace: string;
  accessReason: string;
}
export const FilenameCell = (props: Props) => {
  const [navigate] = useNavigation();
  const { file, storageBucketPath, workspaceNamespace, accessReason } = props;
  const filename = file.name.trim();

  const FileSpan = () => <span id={file.path}>{filename}</span>;

  const FileTooLarge = () => (
    <FlexRow>
      <FileSpan />
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
      { queryParams: { accessReason } }
    );

  const FileWithPreviewButton = () => (
    <FlexRow>
      <FileSpan />
      <TooltipTrigger
        content='Please enter an access reason below'
        disabled={!!accessReason?.trim()}
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
  const isAnalysisFile =
    NOTEBOOKS_DIRECTORY === parseDirectory(file, storageBucketPath) &&
    getAppInfoFromFileName(filename) !== undefined;

  const isTooLargeAnalysisFile =
    isAnalysisFile && file.sizeInBytes > MAX_NOTEBOOK_READ_SIZE_BYTES;

  return cond(
    [isTooLargeAnalysisFile, FileTooLarge],
    [isAnalysisFile, FileWithPreviewButton],
    FileSpan
  );
};
