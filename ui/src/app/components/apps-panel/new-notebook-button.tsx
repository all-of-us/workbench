import * as React from 'react';
import { useEffect, useState } from 'react';
import { faPlusCircle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import { NewNotebookModal } from 'app/pages/analysis/new-notebook-modal';
import { dropNotebookFileSuffix } from 'app/pages/analysis/util';
import { notebooksApi } from 'app/services/swagger-fetch-clients';

import { buttonStyles } from './utils';

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
      <Clickable
        style={{ padding: '0.5em' }}
        onClick={() => setShowModal(true)}
      >
        <FlexColumn style={buttonStyles.button}>
          <FontAwesomeIcon
            icon={faPlusCircle}
            style={buttonStyles.buttonIcon}
          />
          <div style={buttonStyles.buttonText}>Create New</div>
        </FlexColumn>
      </Clickable>
    </>
  );
};
