import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {useEffect, useState} from 'react';
import * as React from 'react';

import {ClrIcon} from './icons';
import {TextArea, TextInput} from './inputs';
import {Spinner} from './spinners';

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

interface Props {
  entityName: string;
  title?: string;
  getExistingNames: () => Promise<string[]>;
  save: Function;
  close: Function;

  alsoRequired: boolean;
}

export const CreateModal = ({entityName, title, getExistingNames, save, close}: Props) => {
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
    nameConflict ? nameConflictMsg : '';
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

