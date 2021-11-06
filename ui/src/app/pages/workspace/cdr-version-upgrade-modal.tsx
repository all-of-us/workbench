import {Button, Clickable} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import colors from 'app/styles/colors';
import {AnalyticsTracker} from 'app/utils/analytics';
import * as React from 'react';

interface Props {
  defaultCdrVersionName: string;
  onClose: () => void;
  upgrade: () => void;
}

const CdrVersionUpgradeModal = (props: Props) => {
  const {defaultCdrVersionName, onClose, upgrade} = props;
  return <Modal onRequestClose={() => onClose()}>
        <ModalTitle data-test-id='cdr-version-upgrade-modal'><FlexRow>
            <span>{defaultCdrVersionName} is now available</span>
            <span><Clickable onClick={() => onClose()}><ClrIcon
                shape='times' size='48' style={{color: colors.accent}}/></Clickable></span>
        </FlexRow></ModalTitle>
        <ModalBody>New data releases add participants and data points. You can upgrade by making a duplicate of your
            workspace, attached to the new version. You'll still have this original workspace in case you need it.</ModalBody>
        <ModalFooter>
            <Button type='primary' onClick={() => {
              onClose();
              AnalyticsTracker.Workspaces.OpenDuplicatePage('Upgrade Modal');
              upgrade();
            }}>Try {defaultCdrVersionName}</Button>
        </ModalFooter>
    </Modal>;
};

export {
    CdrVersionUpgradeModal
};
