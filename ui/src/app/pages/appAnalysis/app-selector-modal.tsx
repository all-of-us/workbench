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
import { serverConfigStore, useStore } from 'app/utils/stores';
import { Workspace } from 'generated/fetch/models/Workspace';

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
  workspace: Workspace;
}
export const AppSelectorModal = (props: AppSelectorModalProps) => {
  const { selectedApp, setSelectedApp, onNext, onClose, workspace } = props;
  const { config } = useStore(serverConfigStore);
  // in display order
  const appList = workspace.aws ? [UIAppType.SAGEMAKER] : [
    UIAppType.JUPYTER,
    ...(config.enableRStudioGKEApp ? [UIAppType.RSTUDIO] : []),
    ...(config.enableSasGKEApp ? [UIAppType.SAS] : []),
  ] ;

  return (
    <Modal
      data-test-id='select-application-modal'
      aria={{
        label: 'Select Applications Modal',
      }}
    >
      <ModalTitle>Analyze Data</ModalTitle>
      <ModalBody>
        <div style={styles.appsLabel} id='select-an-app'>
          Select an application
        </div>
        <Dropdown
          id='application-list-dropdown'
          data-test-id='application-list-dropdown'
          aria-labelledby='select-an-app'
          value={selectedApp}
          appendTo='self'
          options={appList}
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
