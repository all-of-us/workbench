import * as React from 'react';
import { useEffect, useState } from 'react';

import { NewNotebookModal } from 'app/pages/analysis/new-notebook-modal';
import { notebooksApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace } from 'app/utils';

export const JupyterModal = withCurrentWorkspace()((props) => {
  const [notebookNameList, setNotebookNameList] = useState([]);
  const { workspace, onClose, onBack } = props;

  useEffect(() => {
    const { namespace, id } = workspace;

    notebooksApi()
      .getNoteBookList(namespace, id)
      .then((notebookList) => {
        setNotebookNameList(
          notebookList.map((fd) => fd.name.slice(0, -'.ipynb'.length))
        );
      });
  }, []);

  return (
    <NewNotebookModal
      onClose={() => onClose()}
      workspace={workspace}
      existingNameList={notebookNameList}
      onBack={() => onBack()}
      showBackButton
    />
  );
});
