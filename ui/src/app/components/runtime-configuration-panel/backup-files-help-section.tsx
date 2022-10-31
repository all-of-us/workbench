import * as React from 'react';

import { supportUrls } from 'app/utils/zendesk';

export const BackupFilesHelpSection = () => (
  <div>
    <div>
      To backup and share files, such as input data, analysis outputs, or
      installed packages,{' '}
      <a href={supportUrls.workspaceBucket}>
        move them to the workspace bucket.
      </a>
    </div>
    <div>
      Note: Jupyter notebooks are autosaved to the workspace bucket, and
      deleting your disk will not delete your notebooks.
    </div>
  </div>
);
