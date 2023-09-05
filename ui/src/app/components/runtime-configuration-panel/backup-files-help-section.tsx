import * as React from 'react';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import { UIAppType } from 'app/components/apps-panel/utils';
import { supportUrls } from 'app/utils/zendesk';

interface BackupFilesHelpSectionProps {
  appType: UIAppType;
}

export const BackupFilesHelpSection = ({
  appType,
}: BackupFilesHelpSectionProps) => {
  const savedFiles = switchCase<UIAppType, string>(
    appType,
    [UIAppType.JUPYTER, () => 'Jupyter notebooks'],
    [UIAppType.RSTUDIO, () => '.Rmd and .R files'],
    [DEFAULT, () => null]
  );
  const savedFilesReferenceText = switchCase<UIAppType, string>(
    appType,
    [UIAppType.JUPYTER, () => 'your notebooks'],
    [UIAppType.RSTUDIO, () => 'those files'],
    [DEFAULT, () => null]
  );

  return (
    <div>
      <div>
        To backup and share files, such as input data, analysis outputs, or
        installed packages,{' '}
        <a href={supportUrls.workspaceBucket}>
          move them to the workspace bucket.
        </a>
      </div>
      {appType !== UIAppType.CROMWELL && (
        <div>
          Note: {savedFiles} are autosaved to the workspace bucket, and deleting
          your disk will not delete {savedFilesReferenceText}.
        </div>
      )}
    </div>
  );
};
