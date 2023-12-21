import * as React from 'react';
import { useEffect, useState } from 'react';
import { validate } from 'validate.js';

import { ResourceType } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { TextArea, TextInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { summarizeErrors } from 'app/utils';
import { nameValidationFormat, toDisplay } from 'app/utils/resources';

const styles = {
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.375rem',
    padding: '3px 5px',
  },
  invalid: {
    background: colorWithWhiteness(colors.danger, 0.7),
    color: colorWithWhiteness(colors.dark, 0.1),
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.375rem',
    padding: '3px 5px',
  },
};

interface Props {
  resourceType: ResourceType;
  title?: string;
  getExistingNames: () => Promise<string[]>;
  save: Function;
  close: Function;
}

export const CreateModal = ({
  resourceType,
  title,
  getExistingNames,
  save,
  close,
}: Props) => {
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false); // saving refers to the loading request time
  const [name, setName] = useState('');
  const [nameTouched, setNameTouched] = useState(false);
  const [saveErrorMsg, setSaveErrorMsg] = useState('');
  const [existingNames, setExistingNames] = useState([]);

  useEffect(() => {
    getExistingNames().then((names) => setExistingNames(names));
  }, []);

  const onSave = async () => {
    setSaving(true);
    setSaveErrorMsg('');
    save(name.trim(), description)
      .then(() => close())
      .catch(() => {
        setSaveErrorMsg('Data cannot be saved. Please try again.');
        setSaving(false);
      });
  };

  const errors = validate(
    {
      name: name?.trim(),
    },
    {
      name: nameValidationFormat(existingNames, resourceType),
    }
  );

  return (
    <Modal>
      <ModalTitle>{title || `Create ${toDisplay(resourceType)}`}</ModalTitle>
      <ModalBody style={{ marginTop: '0.3rem' }}>
        {saveErrorMsg && (
          <div style={styles.error}>
            <ClrIcon
              className='is-solid'
              shape='exclamation-triangle'
              size={22}
            />
            {saveErrorMsg}
          </div>
        )}
        <TextInput
          style={{
            marginBottom:
              nameTouched && errors?.name.length > 0 ? '0' : '0.75rem',
          }}
          value={name}
          placeholder={toDisplay(resourceType).toUpperCase() + ' NAME'}
          onChange={(v) => {
            setName(v);
            setNameTouched(true);
          }}
          disabled={saving}
        />
        <ValidationError>
          {summarizeErrors(nameTouched && errors?.name)}
        </ValidationError>
        <TextArea
          value={description}
          placeholder='DESCRIPTION'
          disabled={saving}
          onChange={(v) => setDescription(v)}
        />
      </ModalBody>
      <ModalFooter>
        <Button
          style={{ color: colors.primary }}
          type='link'
          onClick={() => close()}
          disabled={saving}
        >
          Cancel
        </Button>
        <Button
          type='primary'
          disabled={!!errors || saving}
          onClick={() => onSave()}
        >
          {saving && <Spinner style={{ marginRight: '0.375rem' }} size={18} />}
          Save
        </Button>
      </ModalFooter>
    </Modal>
  );
};
