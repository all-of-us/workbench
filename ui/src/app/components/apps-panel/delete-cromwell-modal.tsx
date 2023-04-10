import * as React from 'react';

import { Button } from 'app/components/buttons';
import { WarningMessage } from 'app/components/messages';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  toast: {
    position: 'absolute',
    top: 0,
    right: 0,
    width: '13rem',
  },
  commandBox: {
    fontSize: 'medium',
    borderStyle: 'solid',
    padding: '0.5rem 1rem 0.5rem 1rem ',
  },
  commandRow: {
    paddingTop: '1rem',
    paddingBottom: '1rem',
    justifyContent: 'center',
    fontWeight: 'bold',
  },
  copyIcon: {
    marginLeft: 20,
    marginRight: 10,
    marginTop: 8,
  },
});

export const DeleteCromwellConfirmationModal = (props: {
  clickYes;
  clickNo;
}) => {
  let toast: any;
  let toastTimer: NodeJS.Timer;

  const cromshell_job_check_command = 'cromshell-beta list -u';

  const onCopyTextClick = () => {
    navigator.clipboard.writeText(cromshell_job_check_command);
    toast.show({
      severity: 'success',
      detail: 'Text Copied',
      closable: false,
      life: 1000,
    });
    if (!!toastTimer) {
      clearTimeout(toastTimer);
    }
  };
  return (
    <Modal data-test-id='delete-cromwell-modal' width={500}>
      <ModalTitle> Delete Cromwell: check for running jobs </ModalTitle>
      <ModalBody style={{ color: colors.primary }}>
        <WarningMessage>
          <div style={{ color: colors.primary }}>
            <b>Warning: </b> If you delete your Cromwell environment while any
            of your jobs are still running, you will lose the results of your
            jobs.
          </div>
        </WarningMessage>
        <div style={{ paddingTop: '1rem' }}>
          {/* the command to list Jobs is not available for cromshell-alpha*/}
          {/* Ticket https://precisionmedicineinitiative.atlassian.net/browse/RW-9847?search_id=32e4287f-9e8d-4327-9017-d5120d8fabe8
          will install cromshell-beta*/}
          {/*  Use the following command in Jupyter/Terminal to check if you have any*/}
          {/*  jobs running{' '}*/}
          {/*  <FlexRow style={styles.commandRow}>*/}
          {/*    <div style={styles.commandBox}>{cromshell_job_check_command}</div>*/}
          {/*    <Clickable*/}
          {/*      data-test-id={'copy-to-clipboard'}*/}
          {/*      onClick={() => onCopyTextClick()}*/}
          {/*    >*/}
          {/*      <TooltipTrigger content={'Click to Copy Command'}>*/}
          {/*        <FontAwesomeIcon*/}
          {/*          size={'xl'}*/}
          {/*          icon={faClipboard}*/}
          {/*          style={styles.copyIcon}*/}
          {/*        />*/}
          {/*      </TooltipTrigger>*/}
          {/*    </Clickable>*/}
          {/*    <Toast ref={(el) => (toast = el)} style={styles.toast} />*/}
          {/*  </FlexRow>*/}
          Do you still want to delete your Cromwell environment?
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          data-test-id={'delete-cromwell-btn'}
          type='primary'
          onClick={props.clickYes}
        >
          YES, DELETE
        </Button>
        <Button type='secondary' onClick={props.clickNo}>
          No
        </Button>
      </ModalFooter>
    </Modal>
  );
};
