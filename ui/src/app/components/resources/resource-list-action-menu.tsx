import * as React from 'react';

import { WorkspaceResource } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';
import { isNotebook } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';
import { isValidBilling } from 'app/utils/workspace-utils';

export interface CommonActionMenuProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
}
interface Props extends CommonActionMenuProps {
  workspace: WorkspaceData;
}
export const ResourceListActionMenu = (props: Props) => {
  const { resource, workspace } = props;

  return cond(
    [
      isNotebook(resource),
      () => (
        <NotebookActionMenu
          {...props}
          disableDuplicate={!isValidBilling(workspace)}
          useAppFilesListIcon={false}
        />
      ),
    ],
    null
  );
};
