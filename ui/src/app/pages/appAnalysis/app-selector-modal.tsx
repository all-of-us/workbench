import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const APP_LIST = [UIAppType.JUPYTER, UIAppType.RSTUDIO];

const styles = reactStyles({
  appsLabel: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '14px',
    lineHeight: '24px',
    paddingBottom: '0.75rem',
  },
});

interface AppSelectorModalProps {
  selectedApp: UIAppType;
  setSelectedApp: (UIAppType) => void;
  onNext: () => void;
  onClose: () => void;
}
export const AppSelectorModal = (props: AppSelectorModalProps) => {
  const { selectedApp, setSelectedApp, onNext, onClose } = props;
  return (
    <Modal
      data-test-id='select-application-modal'
      aria={{
        label: 'Select Applications Modal',
      }}
    >
      <ModalTitle>Analyze Data</ModalTitle>
      <ModalBody>
        <div style={styles.appsLabel}>Select an application</div>
        <Dropdown
          id='application-list-dropdown'
          data-test-id='application-list-dropdown'
          ariaLabel='Application List Dropdown'
          value={selectedApp}
          appendTo='self'
          options={APP_LIST}
          placeholder='Choose One'
          onChange={(e) => setSelectedApp(e.value)}
          style={{ width: '13.5rem' }}
        />
      </ModalBody>
      <ModalFooter style={{ paddingTop: '3rem' }}>
        <Button
          style={{ marginRight: '3rem' }}
          type='secondary'
          aria-label='close'
          onClick={onClose}
        >
          Close
        </Button>
        <Button
          data-test-id='next-btn'
          type='primary'
          aria-label='next'
          onClick={onNext}
          disabled={!selectedApp}
        >
          Next
        </Button>
      </ModalFooter>
    </Modal>
  );
};
