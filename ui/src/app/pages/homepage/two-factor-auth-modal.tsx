import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {redirectToTwoFactorSetup} from 'app/utils/access-utils';
import twoFactorAuthModalImage from 'assets/images/2sv-image.png';
import * as React from 'react';

const styles = reactStyles({
  twoFactorAuthModalCancelButton: {
    marginRight: '1rem',
  },
  twoFactorAuthModalHeader: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    lineHeight: '24px',
    marginBottom: 0
  },
  twoFactorAuthModalImage: {
    border: `1px solid ${colors.light}`,
    height: '6rem',
    width: '100%',
    marginTop: '1rem'
  },
  twoFactorAuthModalText: {
    color: colors.primary,
    lineHeight: '22px'
  },
});

interface Props {
  onClick: Function;
  onCancel: Function;
}
export const TwoFactorAuthModal = (props: Props) => {
  const {onClick, onCancel} = props;
  return <Modal width={500}>
    <ModalTitle style={styles.twoFactorAuthModalHeader}>Redirecting to turn on Google 2-step Verification</ModalTitle>
    <ModalBody>
      <div style={styles.twoFactorAuthModalText}>Clicking ‘Proceed’ will direct you to a Google page where you
                need to login with your <span style={{fontWeight: 600}}>researchallofus.org</span> account and turn
                on 2-Step Verification. Once you complete this step, you will see the screen shown below. At that
                point, you can return to this page and click 'Refresh’.</div>
      <img style={styles.twoFactorAuthModalImage} src={twoFactorAuthModalImage} />
    </ModalBody>
    <ModalFooter>
      <Button
        type='secondary'
        style={styles.twoFactorAuthModalCancelButton}
        onClick = {onCancel}>Cancel</Button>
      <Button
        type='primary'
        onClick = {() => {
          onClick();
          redirectToTwoFactorSetup();
        }}>Proceed</Button>
    </ModalFooter>
  </Modal>;
};
