import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { Button, IconButton } from 'app/components/buttons';
import { SemiBoldHeader } from 'app/components/headers';
import { PlaygroundIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import { reactStyles } from 'app/utils';
import { AdminBannerModal } from './admin-banner-modal';

const styles = reactStyles({
  page: {
    padding: '1rem',
  },
  header: {
    marginBottom: '1rem',
    fontSize: 20,
  },
  tableStyle: {
    fontSize: 12,
    minWidth: 800,
  },
  colStyle: {
    fontSize: 12,
    height: '60px',
    lineHeight: '0.75rem',
    overflow: 'hidden',
    padding: '.5em',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  createButtonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '1rem',
  },
});

export const AdminBannerTable = (props: WithSpinnerOverlayProps) => {
  const [banners, setBanners] = useState<StatusAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newBanner, setNewBanner] = useState<StatusAlert>({
    title: '',
    message: '',
    link: '',
    alertLocation: StatusAlertLocation.AFTER_LOGIN,
  });

  useEffect(() => {
    const loadBanners = async () => {
      setLoading(true);
      try {
        // Temporary solution until StatusAlertApi is regenerated
        const statusAlerts = await statusAlertApi().getStatusAlerts();
        setBanners(statusAlerts);
      } finally {
        setLoading(false);
        props.hideSpinner();
      }
    };

    loadBanners();
  }, []);

  const deleteProduct = async (id) => {
    console.log('Deleting banner:', id);
    try {
      await statusAlertApi().deleteStatusAlert(id);
      const statusAlerts = await statusAlertApi().getStatusAlerts();
      setBanners(statusAlerts);
    } catch (error) {
      console.error('Error deleting banner:', error);
    }
  };

  const actionBodyTemplate = (rowData) => {
    return (
      <IconButton
        label='Delete'
        icon={PlaygroundIcon}
        className='p-button-danger'
        onClick={() => {
          console.log('What is row data? ', rowData);
          deleteProduct(rowData.statusAlertId);
        }}
        style={{ height: '2rem', width: '2rem' }}
      />
    );
  };

  const handleCreateBanner = async () => {
    try {
      await statusAlertApi().postStatusAlert(newBanner);
      const statusAlerts = await statusAlertApi().getStatusAlerts();
      setBanners(statusAlerts);
      setShowCreateModal(false);
      setNewBanner({
        title: '',
        message: '',
        link: '',
        alertLocation: StatusAlertLocation.AFTER_LOGIN,
      });
    } catch (error) {
      console.error('Error creating banner:', error);
    }
  };

  if (loading) {
    return <SpinnerOverlay />;
  }

  return (
    <div style={styles.page}>
      <SemiBoldHeader style={styles.header}>Service Banners</SemiBoldHeader>
      <div style={styles.createButtonContainer}>
        <Button onClick={() => setShowCreateModal(true)}>
          Create New Banner
        </Button>
      </div>
      <DataTable
        value={banners}
        emptyMessage='No active banners found'
        style={styles.tableStyle}
      >
        <Column
          field='statusAlertId'
          header='ID'
          style={{ ...styles.colStyle, width: '50px' }}
        />
        <Column
          field='title'
          header='Title'
          style={{ ...styles.colStyle, width: '200px' }}
        />
        <Column
          field='message'
          header='Message'
          style={{ ...styles.colStyle, width: '400px' }}
        />
        <Column
          field='link'
          header='Link'
          style={{ ...styles.colStyle, width: '200px' }}
          body={(rowData: StatusAlert) => rowData.link || '-'}
        />
        <Column
          field='alertLocation'
          header='Location'
          style={{ ...styles.colStyle, width: '150px' }}
          body={(rowData: StatusAlert) =>
            rowData.alertLocation === StatusAlertLocation.BEFORE_LOGIN
              ? 'Before Login'
              : 'After Login'
          }
        />
        <Column body={actionBodyTemplate} header='Actions' />
      </DataTable>

      {showCreateModal && (
        <AdminBannerModal
          banner={newBanner}
          setBanner={setNewBanner}
          onClose={() => setShowCreateModal(false)}
          onCreate={handleCreateBanner}
        />
      )}
    </div>
  );
};
