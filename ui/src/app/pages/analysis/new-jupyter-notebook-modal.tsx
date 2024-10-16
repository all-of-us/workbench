import * as React from 'react';
import { useEffect, useState } from 'react';
import { validate } from 'validate.js';

import { Workspace } from 'generated/fetch';

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
import { getExistingJupyterNotebookNames } from 'app/pages/analysis/util';
import { analysisTabName } from 'app/routing/utils';
import { userMetricsApi } from 'app/services/swagger-fetch-clients';
import { isEmpty, summarizeErrors } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { useNavigation } from 'app/utils/navigation';
import { Kernels } from 'app/utils/notebook-kernels';
import { validateNewNotebookName } from 'app/utils/resources';

import { appendJupyterNotebookFileSuffix } from './util';

interface Props {
  onClose: Function;
  workspace: Workspace;
  existingNameList?: string[];
  onBack?: Function;
}

export const NewJupyterNotebookModal = (props: Props) => {
  const { onBack, onClose, workspace } = props;

  const [name, setName] = useState('');
  const [kernel, setKernel] = useState(Kernels.Python3);
  const [nameTouched, setNameTouched] = useState(false);
  const [existingNotebookNameList, setExistingNotebookNameList] = useState([]);
  const [navigate] = useNavigation();

  useEffect(() => {
    const { existingNameList } = props;
    if (!!existingNameList) {
      setExistingNotebookNameList(existingNameList);
    } else {
      getExistingJupyterNotebookNames(workspace).then(
        setExistingNotebookNameList
      );
    }
  }, [props.existingNameList]);

  const errors = {
    ...validateNewNotebookName(name, existingNotebookNameList),
    ...validate(
      { kernel },
      {
        kernel: { presence: { allowEmpty: false } },
      }
    ),
  };

  const create = () => {
    userMetricsApi().updateRecentResource(
      workspace.namespace,
      workspace.terraName,
      {
        notebookName: appendJupyterNotebookFileSuffix(name),
      }
    );
    navigate(
      [
        'workspaces',
        workspace.namespace,
        workspace.terraName,
        analysisTabName,
        encodeURIComponent(name),
      ],
      { queryParams: { kernelType: kernel, creating: true } }
    );
  };

  return (
    <Modal
      onRequestClose={onClose}
      aria={{
        label: 'New Notebook Modal',
      }}
    >
      <ModalTitle>New Jupyter Notebook</ModalTitle>
      <ModalBody>
        <div style={headerStyles.formLabel}>Name:</div>
        <TextInput
          data-test-id='create-jupyter-new-name-input'
          autoFocus
          value={name}
          onChange={(v) => {
            setName(v);
            setNameTouched(true);
          }}
        />
        <ValidationError data-test-id='create-jupyter-new-name-invalid'>
          {summarizeErrors(nameTouched && errors?.name)}
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
            style={{ marginLeft: '0.75rem' }}
            disabled={!isEmpty(errors)}
            onClick={() => {
              AnalyticsTracker.Notebooks.Create(Kernels[kernel]);
              create();
            }}
          >
            Create Notebook
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>
  );
};
