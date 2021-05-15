import {AlertDanger} from 'app/components/alert';
import {Button, Link} from 'app/components/buttons';
import {CheckBox, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {dataSetApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {summarizeErrors} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {
  DataSet,
  DataSetRequest,
  DomainValuePair,
  FileDetail,
  KernelTypeEnum,
  PrePackagedConceptSetEnum
} from 'generated/fetch';
import {useEffect, useState} from 'react';
import * as React from 'react';

import {validate} from 'validate.js';
import {FlexRow} from './flex';
import {ClrIcon} from './icons';
import {Spinner} from './spinners';
import {ExportDataSet} from '../pages/data/data-set/export-data-set';

interface Props {
  entityName: string;
  getExistingNames: () => string[];
  onSave: Function; // function that takes in name, description
  onCancel: Function;
}

// Name is required
// Cannot conflict with existing names
// handle save errors

const styles = {
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.25rem',
    padding: '3px 5px'
  },
  invalid: {
    background: colorWithWhiteness(colors.danger, .7),
    color: colorWithWhiteness(colors.dark, .1),
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.25rem',
    padding: '3px 5px'
  }
};

export const CreateModal = ({entityName, title, getExistingNames, save, close}) => {
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false); // saving refers to the loading request time
  const [name, setName] = useState('');
  const [nameTouched, setNameTouched] = useState(false);
  const [saveErrorMsg, setSaveErrorMsg] = useState('');
  const [existingNames, setExistingNames] = useState([]);

  useEffect(() => { getExistingNames().then(names => setExistingNames(names)); }, []);

  const nameConflictMsg = `A ${entityName.toLowerCase()} with this name already exists. Please choose a different name.`;
  const invalidNameInput = nameTouched && (!name || !name.trim());
  const nameConflict = !!name && existingNames.includes(name.trim());
  const inputErrorMsg = invalidNameInput ? `${entityName} name is required` :
    nameConflict ? nameConflictMsg :
    '';

  const disableSaveButton = nameConflict || invalidNameInput || !name || saving;

  const onSave = async() => {
    setSaving(true);
    setSaveErrorMsg('');
    save(name, description)
      .then(() => close())
      .catch(e => {
        setSaveErrorMsg(e.status === 409 ? nameConflictMsg : 'Data cannot be saved. Please try again.');
        setSaving(false);
      });
  };

  return <Modal>
    <ModalTitle style={inputErrorMsg ? {marginBottom: 0} : {}}>{title || `Create ${entityName}`}</ModalTitle>
    <ModalBody style={{marginTop: '0.2rem'}}>
      {saveErrorMsg && <div style={styles.error}>
          <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
          {saveErrorMsg}
      </div>}
      {inputErrorMsg && <div style={styles.invalid}>{inputErrorMsg}</div>}

      <TextInput style={{marginBottom: '0.5rem'}}
                 value={name}
                 placeholder={entityName.toUpperCase() + ' NAME'}
                 onChange={(v) => {
                   setName(v);
                   setNameTouched(true);
                 }}
                 disabled={saving}/>
      <TextArea value={description}
                placeholder='DESCRIPTION'
                disabled={saving}
                onChange={(v) => setDescription(v)}/>
    </ModalBody>
    <ModalFooter>
      <Button style={{color: colors.primary}}
              type='link'
              onClick={() => close()}
              disabled={saving}>
        Cancel
      </Button>
      <Button type='primary'
              disabled={disableSaveButton}
              onClick={() => onSave()}>
        {saving && <Spinner style={{marginRight: '0.25rem'}} size={18} />}
        Save
      </Button>
    </ModalFooter>
  </Modal>;
};
