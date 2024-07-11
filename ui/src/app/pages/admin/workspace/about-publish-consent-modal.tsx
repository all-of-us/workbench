import * as React from 'react';
import { useState } from 'react';

import { Button } from 'app/components/buttons';
import { SemiBoldHeader } from 'app/components/headers';
import { CheckBox } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';

interface Props {
  onConfirm: () => void;
  onCancel: () => void;
}
export const AboutPublishConsentModal = (props: Props) => {
  const [checkboxStates, setCheckboxStates] = useState({
    readAndAgreeCheckbox: false,
    noHarmfulLanguageCheckbox: false,
    confirmContactCheckbox: false,
  });

  const handleCheckboxChange = (checkboxId: string, checked: boolean) => {
    // Step 2 & 3: Update the state immutably
    setCheckboxStates((prevState) => ({
      ...prevState,
      [checkboxId]: checked,
    }));
  };
  // State to control the visibility of the button
  const showConfirmButton = Object.values(checkboxStates).every(
    (state) => state === true
  );

  return (
    <Modal width={800}>
      <ModalTitle>
        <SemiBoldHeader>Publish As a Community Workspace</SemiBoldHeader>
      </ModalTitle>
      <ModalBody>
        {/* Text area to enter the reason for locking workspace */}
        <div>
          <label style={{ color: colors.primary }}>
            Please read and agree to the following statements to confirm that
            your workspace meets the{' '}
            <span style={{ fontStyle: 'italic' }}>All of US</span> Researcher
            Workbench community sharing requirements. Read the NAME article on
            the User Support Hub to learn more about community workspaces.
          </label>
          <br />
          <br />
          <CheckBox
            style={{ marginRight: '0.375rem' }}
            onChange={(checked) =>
              handleCheckboxChange('readAndAgreeCheckbox', checked)
            }
            checked={checkboxStates.readAndAgreeCheckbox}
          />
          <label style={{ color: colors.primary }}>
            I confirm that the workspace does not include any harmful or
            discriminatory language
          </label>
          <br />
          <br />
          <CheckBox
            style={{ marginRight: '0.375rem' }}
            onChange={(checked) => {
              handleCheckboxChange('noHarmfulLanguageCheckbox', checked);
            }}
            checked={checkboxStates.noHarmfulLanguageCheckbox}
          />
          <label style={{ color: colors.primary }}>
            I confirm that the workspace includes contact information in the
            'About' section of the workspace, such as the email address for the
            lead author or the URL to the lab's website
          </label>
          <br />
          <br />
          <CheckBox
            style={{ marginRight: '0.375rem' }}
            onChange={(checked) =>
              handleCheckboxChange('confirmContactCheckbox', checked)
            }
            checked={checkboxStates.confirmContactCheckbox}
          />
          <label style={{ color: colors.primary }}>
            I confirm that the workspace collaborators gave permission to share
            the workspace or that I do not have workspace collaborators.
          </label>
          <br />
          <br />
          <label style={{ color: colors.primary }}>
            By clicking the publish button, you agree for your workspace to be
            shared under the community workspace section of the Researcher
            Workbench.
          </label>
          <br />
          <br />
          <label style={{ color: colors.primary }}>
            If you want to remove your workspace as a community workspace,
            contact our support team by using the Help Desk widget in the
            Workbench or by emailing support@researchallofus.org.
          </label>
          <br />
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          type='secondary'
          style={{ marginRight: '0.75rem' }}
          onClick={props.onCancel}
        >
          CANCEL
        </Button>
        <Button
          type='primary'
          onClick={props.onConfirm}
          disabled={!showConfirmButton}
        >
          CONFIRM
        </Button>
      </ModalFooter>
    </Modal>
  );
};
