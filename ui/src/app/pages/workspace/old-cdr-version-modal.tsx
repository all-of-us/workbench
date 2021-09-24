
import * as React from 'react';
import {useState} from 'react';

import {Button, StyledExternalLink} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {CheckBox} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';

export const styles = reactStyles({
  checkbox: {
    marginRight: '.31667rem',
    zoom: '1.5'
  },
  text: {
    fontSize: '14px',
    color: colors.primary,
    fontWeight: 400,
    lineHeight: '24px'
  },
});

interface Props {
  onCancel: () => void;
  onContinue: () => void;
}

const OldCdrVersionModal = (props: Props) => {
  const {onCancel, onContinue} = props;

  const [willUse, setWillUse] = useState(false);
  const [willIdentify, setWillIdentify] = useState(false);

  // Enable "Continue" only if the user consents to these two conditions
  const canContinue = willUse && willIdentify;

  return <Modal width={550}>
        <ModalTitle data-test-id='old-cdr-version-modal' style={{fontSize: '16px'}}>
            You have selected an older version of the dataset.
        </ModalTitle>
        <ModalBody>
            <FlexColumn>
                <div style={styles.text}>
                    Use of older dataset versions is <StyledExternalLink
                    href='https://www.researchallofus.org/data-tools/data-access/'>
                    permitted</StyledExternalLink> only to complete a study that was started using that older
                    version, or to replicate a previous study. You must confirm the following to continue.
                </div>
                <div style={{margin: '24px 0'}} data-test-id='consent-will-use'><CheckBox
                    style={styles.checkbox}
                    labelStyle={styles.text}
                    label='I will use this workspace to complete an existing study or replicate a previous study.'
                    onChange={setWillUse}/>
                </div>
                <div data-test-id='consent-will-identify'><CheckBox
                    style={styles.checkbox}
                    labelStyle={styles.text}
                    label='In the workspace description below, I will identify which study I am continuing or replicating.'
                    onChange={setWillIdentify}/>
                </div>
            </FlexColumn>
        </ModalBody>
        <ModalFooter>
            <Button type='secondary' onClick={onCancel}>Cancel</Button>
            <Button type='primary' onClick={onContinue} disabled={!canContinue}>Continue</Button>
        </ModalFooter>
    </Modal>;
};

export {
    OldCdrVersionModal,
};
