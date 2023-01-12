import * as React from 'react';
import { useEffect, useState } from 'react';
import { faPlusCircle } from '@fortawesome/free-solid-svg-icons';

import { Workspace } from 'generated/fetch';

import { NewNotebookModal } from 'app/pages/analysis/new-notebook-modal';
import { dropNotebookFileSuffix } from 'app/pages/analysis/util';
import { notebooksApi } from 'app/services/swagger-fetch-clients';

import { AppsPanelButton } from './apps-panel-button';

export const NewNotebookButton = (props: { workspace: Workspace }) => {
  const { workspace } = props;

  const [showModal, setShowModal] = useState(false);
  const [notebookNameList, setNotebookNameList] = useState<string[]>([]);

  useEffect(() => {
    notebooksApi()
      .getNoteBookList(workspace.namespace, workspace.id)
      .then((nbl) =>
        setNotebookNameList(nbl.map((fd) => dropNotebookFileSuffix(fd.name)))
      );
  }, []);

  return (
    <>
      {showModal && (
        <NewNotebookModal
          {...{ workspace }}
          existingNameList={notebookNameList}
          onClose={() => {
            setShowModal(false);
          }}
        />
      )}
      <AppsPanelButton
        onClick={() => setShowModal(true)}
        icon={faPlusCircle}
        buttonText='Create New'
      />
    </>
  );
};
