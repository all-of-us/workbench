import * as React from 'react';
import { useState } from 'react';
import { faCheck, faClipboard } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';

interface Props {
  title: string;
  copyText: string;
  closeFunction: Function;
}

export const CopySnippetModal = ({ title, copyText, closeFunction }: Props) => {
  const [isCopied, setIsCopied] = useState(false);
  const [timer, setTimer] = useState(null);

  return (
    <Modal>
      <ModalTitle>{title}</ModalTitle>
      <ModalBody style={{ color: colors.primary }}>{copyText}</ModalBody>
      <ModalFooter style={{ justifyContent: 'space-between' }}>
        <Button
          type='secondaryLight'
          style={{ paddingLeft: '0' }}
          onClick={() => {
            navigator.clipboard.writeText(copyText);
            setIsCopied(true);

            // The desired behavior is that clicking the button will always copy the text onto the clipboard
            // but the "Copied" state is only shown for two seconds after the latest click
            clearTimeout(timer);
            setTimer(setTimeout(() => setIsCopied(false), 2000));
          }}
        >
          {isCopied ? (
            <React.Fragment>
              <FontAwesomeIcon
                icon={faCheck}
                style={{ marginRight: '0.6rem' }}
              />
              Copied to Clipboard
            </React.Fragment>
          ) : (
            <React.Fragment>
              <FontAwesomeIcon
                icon={faClipboard}
                style={{ marginRight: '0.6rem' }}
              />
              Copy Snippet
            </React.Fragment>
          )}
        </Button>
        <Button onClick={() => closeFunction()}>Close</Button>
      </ModalFooter>
    </Modal>
  );
};
