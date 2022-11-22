import { useEffect, useState } from 'react';
import * as React from 'react';
import { validate } from 'validate.js';

import { ResourceType, Workspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { RadioButton, TextInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { nameValidationFormat } from 'app/components/rename-modal';
import { getExistingNotebookNames } from 'app/pages/analysis/util';
import { userMetricsApi } from 'app/services/swagger-fetch-clients';
import { summarizeErrors } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { useNavigation } from 'app/utils/navigation';
import { Kernels } from 'app/utils/notebook-kernels';

interface Props {
  onClose: Function;
  workspace: Workspace;
  existingNameList?: string[];
  onBack?: Function;
}

export const NewNotebookModal = (props: Props) => {
  const [name, setName] = useState('');
  const [kernel, setKernel] = useState(Kernels.Python3);
  const [nameTouched, setNameTouched] = useState(false);
  const [existingNotebookNameList, setExistingNotebookNameList] = useState([]);
  const [navigate] = useNavigation();

  const { onBack, onClose, workspace } = props;
  const errors = validate(
    { name, kernel },
    {
      kernel: { presence: { allowEmpty: false } },
      name: nameValidationFormat(
        existingNotebookNameList,
        ResourceType.NOTEBOOK
      ),
    }
  );

  useEffect(() => {
    const { existingNameList } = props;
    if (!!existingNameList) {
      setExistingNotebookNameList(existingNameList);
    } else {
      getExistingNotebookNames(workspace).then((notebookList) => {
        setExistingNotebookNameList(notebookList);
      });
    }
  }, [props.existingNameList]);

  const save = () => {
    userMetricsApi().updateRecentResource(workspace.namespace, workspace.id, {
      notebookName: `${name}.ipynb`,
    });
    navigate(
      [
        'workspaces',
        workspace.namespace,
        workspace.id,
        'notebooks',
        encodeURIComponent(name),
      ],
      { queryParams: { kernelType: kernel, creating: true } }
    );
  };

  return (
    <Modal onRequestClose={onClose}>
      <ModalTitle>New Notebook</ModalTitle>
      <ModalBody>
        <div style={headerStyles.formLabel}>Name:</div>
        <TextInput
          autoFocus
          value={name}
          onChange={(v) => {
            setName(v);
            setNameTouched(true);
          }}
        />
        <ValidationError>
          {summarizeErrors(nameTouched && errors && errors.name)}
        </ValidationError>
        <div style={headerStyles.formLabel}>Programming Language:</div>
        <label style={{ display: 'block' }}>
          <RadioButton
            checked={kernel === Kernels.Python3}
            onChange={() => setKernel(Kernels.Python3)}
          />
          &nbsp;Python 3
        </label>
        <label style={{ display: 'block' }}>
          <RadioButton
            checked={kernel === Kernels.R}
            onChange={() => setKernel(Kernels.R)}
          />
          &nbsp;R
        </label>
      </ModalBody>
      <ModalFooter>
        {onBack && (
          <Button type='secondary' onClick={onBack}>
            BACK
          </Button>
        )}
        <Button type='secondary' onClick={onClose}>
          Cancel
        </Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button
            style={{ marginLeft: '0.5rem' }}
            disabled={!!errors}
            onClick={() => {
              AnalyticsTracker.Notebooks.Create(Kernels[kernel]);
              save();
            }}
          >
            Create Notebook
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>
  );
};
