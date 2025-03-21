import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { Button, IconButton } from 'app/components/buttons';
import { SemiBoldHeader } from 'app/components/headers';
import { TrashCan } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import { reactStyles } from 'app/utils';

import { AdminBannerModal } from './admin-banner-modal';

const styles = reactStyles({
  page: {
    padding: '1rem',
    maxWidth: '100%',
  },
  header: {
    marginBottom: '1rem',
    fontSize: 20,
  },
  tableStyle: {
    fontSize: 12,
    width: '100%',
  },
  tableContainer: {
    overflowX: 'auto',
    width: '100%',
    minWidth: '100%',
  },
  colStyle: {
    fontSize: 12,
    height: 'auto',
    lineHeight: '1.2rem',
    padding: '.5em',
    overflow: 'visible',
    whiteSpace: 'normal',
    wordBreak: 'normal',
  },
  titleCol: {
    width: '20%',
  },
  messageCol: {
    width: '40%',
  },
  linkCol: {
    width: '17%',
  },
  locationCol: {
    width: '10%',
  },
  actionCol: {
    width: '5%',
    textAlign: 'center',
  },
  createButtonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '1rem',
  },
  messageText: {
    display: 'block',
    whiteSpace: 'normal',
    wordBreak: 'normal',
  },
});

const getDefaultStatusAlert = (): StatusAlert => {
  return {
    title: '',
    message: '',
    link: '',
    alertLocation: StatusAlertLocation.AFTER_LOGIN,
    startTimeEpochMillis: Date.now(),
  };
};

export const AdminBannerTable = (props: WithSpinnerOverlayProps) => {
  const [banners, setBanners] = useState<StatusAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newBanner, setNewBanner] = useState<StatusAlert>(
    getDefaultStatusAlert()
  );

  useEffect(() => {
    const loadBanners = async () => {
      props.hideSpinner();
      setLoading(true);
      try {
        const statusAlerts = await statusAlertApi().getStatusAlerts();
        setBanners(statusAlerts);
      } catch (e) {
        console.error('Error loading status alerts: ', e);
        setLoading(false);
      }
    };

    loadBanners();
  }, []);

  useEffect(() => {
    setLoading(false);
  }, [banners]);

  const deleteBanner = async (id: number) => {
    try {
      setLoading(true);
      await statusAlertApi().deleteStatusAlert(id);
      const statusAlerts = await statusAlertApi().getStatusAlerts();
      setBanners(statusAlerts);
    } catch (error) {
      console.error('Error deleting banner:', error);
    }
  };

  const actionBodyTemplate = (rowData: StatusAlert) => {
    return (
      <TooltipTrigger content='Delete Banner'>
        <IconButton
          label='Delete'
          icon={TrashCan}
          onClick={() => deleteBanner(rowData.statusAlertId)}
          style={{ height: '1.5rem', width: '1.5rem' }}
        />
      </TooltipTrigger>
    );
  };

  const handleCreateBanner = async () => {
    try {
      await statusAlertApi().postStatusAlert(newBanner);
      const statusAlerts = await statusAlertApi().getStatusAlerts();
      setBanners(statusAlerts);
      setShowCreateModal(false);
      setNewBanner(getDefaultStatusAlert());
    } catch (error) {
      console.error('Error creating banner:', error);
    }
  };

  const messageBodyTemplate = (rowData: StatusAlert) => {
    return (
      <span style={styles.messageText} title={rowData.message}>
        {rowData.message}
      </span>
    );
  };

  const linkBodyTemplate = (rowData: StatusAlert) => {
    return (
      <span style={styles.messageText} title={rowData.link || '-'}>
        {rowData.link || '-'}
      </span>
    );
  };

  const timestampTemplate = (value: number | null | undefined) => {
    if (!value) {
      return '-';
    }
    return new Date(value).toLocaleString();
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
      <div style={styles.tableContainer}>
        <DataTable
          value={banners}
          emptyMessage='No active banners found'
          style={styles.tableStyle}
          columnResizeMode='expand'
          scrollable={false}
          scrollHeight='flex'
        >
          <Column
            field='title'
            header='Title'
            style={styles.colStyle}
            sortable
          />
          <Column
            field='message'
            header='Message'
            style={styles.colStyle}
            body={messageBodyTemplate}
            sortable
          />
          <Column
            field='link'
            header='Link'
            style={styles.colStyle}
            body={linkBodyTemplate}
            sortable
          />
          <Column
            field='alertLocation'
            header='Location'
            style={styles.colStyle}
            body={(rowData: StatusAlert) =>
              rowData.alertLocation === StatusAlertLocation.BEFORE_LOGIN
                ? 'Before Login'
                : 'After Login'
            }
            sortable
          />
          <Column
            field='startTimeEpochMillis'
            header='Start Time'
            body={(rowData) => timestampTemplate(rowData.startTimeEpochMillis)}
            sortable
          />
          <Column
            field='endTimeEpochMillis'
            header='End Time'
            style={styles.colStyle}
            body={(rowData) => timestampTemplate(rowData.endTimeEpochMillis)}
            sortable
          />
          <Column
            body={actionBodyTemplate}
            header='Actions'
            style={styles.colStyle}
          />
        </DataTable>
      </div>

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
