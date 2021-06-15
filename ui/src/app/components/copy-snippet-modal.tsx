import {faCheck, faClipboard} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Button} from 'app/components/buttons';
import colors from 'app/styles/colors';
import {useState} from 'react';
import * as React from 'react';
import {Modal, ModalBody, ModalFooter, ModalTitle} from './modals';

interface Props {
  title: string;
  copyText: string;
  closeFunction: Function;
}

export const CopySnippetModal = ({title, copyText, closeFunction}: Props) => {
  const [isCopied, setIsCopied] = useState(false);
  const [timer, setTimer] = useState(null);

  return (
    <Modal>
      <ModalTitle>{title}</ModalTitle>
      <ModalBody style={{color: colors.primary}}>{copyText}</ModalBody>
      <ModalFooter style={{justifyContent: 'space-between'}}>
        <Button type='secondaryLight'
                style={{paddingLeft: '0'}}
                onClick={() => {
                  // @ts-ignore: Unreachable code error. TODO: We can remove this once TS is upgraded to >= 3.4
                  navigator.clipboard.writeText(copyText);
                  setIsCopied(true);

                  clearTimeout(timer);
                  setTimer(setTimeout(() => setIsCopied(false), 2000));
                }}>
          {
            isCopied
              ? <React.Fragment><FontAwesomeIcon icon={faCheck} style={{marginRight: '0.4rem'}}/>Copied to Clipboard</React.Fragment>
              : <React.Fragment><FontAwesomeIcon icon={faClipboard} style={{marginRight: '0.4rem'}}/>Copy Snippet</React.Fragment>
          }
        </Button>
        <Button onClick={() => closeFunction()}>Close</Button>
      </ModalFooter>
    </Modal>
  );
};
