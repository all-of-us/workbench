import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  StatusAlert,
  StatusAlertLocation,
  VwbSystemNotification,
  VwbSystemNotificationPriority,
  VwbSystemNotificationType,
} from 'generated/fetch';

import { Button, IconButton } from 'app/components/buttons';
import { SemiBoldHeader } from 'app/components/headers';
import { TrashCan } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  statusAlertApi,
  vwbSystemNotificationAdminApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

import { AdminBannerModal } from './admin-banner-modal';
import { AdminVwbSystemNotificationModal } from './admin-vwb-system-notification-modal';

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
    gap: '1rem',
    marginBottom: '1rem',
  },
  sourceCol: {
    width: '8%',
  },
  messageText: {
    display: 'block',
    whiteSpace: 'normal',
    wordBreak: 'normal',
  },
});

/**
 * One row of the banner table. All of Us service banners and Verily Workbench system
 * notifications are stored separately and have different fields, so they are normalized here to
 * render in a single table.
 */
interface BannerRow {
  key: string;
  isVwb: boolean;
  title: string;
  message: string;
  link?: string;
  alertLocation?: StatusAlertLocation;
  startTimeEpochMillis?: number;
  endTimeEpochMillis?: number;
  statusAlertId?: number;
  vwbNotificationId?: string;
}

const toBannerRows = (
  statusAlerts: StatusAlert[],
  vwbNotifications: VwbSystemNotification[]
): BannerRow[] => [
  ...statusAlerts.map((alert) => ({
    key: `aou-${alert.statusAlertId}`,
    isVwb: false,
    title: alert.title,
    message: alert.message,
    link: alert.link,
    alertLocation: alert.alertLocation,
    startTimeEpochMillis: alert.startTimeEpochMillis,
    endTimeEpochMillis: alert.endTimeEpochMillis,
    statusAlertId: alert.statusAlertId,
  })),
  ...vwbNotifications.map((notification) => ({
    key: `vwb-${notification.id}`,
    isVwb: true,
    title: notification.title,
    message: notification.message,
    startTimeEpochMillis: notification.startTimeEpochMillis,
    endTimeEpochMillis: notification.endTimeEpochMillis,
    vwbNotificationId: notification.id,
  })),
];

const getDefaultVwbSystemNotification = (): VwbSystemNotification => {
  return {
    title: '',
    message: '',
    notificationType: VwbSystemNotificationType.PASSIVE,
    notificationPriority: VwbSystemNotificationPriority.INFO,
    startTimeEpochMillis: Date.now(),
  };
};

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
  const [banners, setBanners] = useState<BannerRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newBanner, setNewBanner] = useState<StatusAlert>(
    getDefaultStatusAlert()
  );
  const [showVwbCreateModal, setShowVwbCreateModal] = useState(false);
  const [newVwbNotification, setNewVwbNotification] =
    useState<VwbSystemNotification>(getDefaultVwbSystemNotification());
  const [error, setError] = useState<string>(null);

  const loadBanners = async () => {
    // Verily Workbench is the system of record for its own notifications, so listing them depends
    // on user-manager being reachable. Keep All of Us banners usable if it is not.
    const [statusAlerts, vwbNotifications] = await Promise.all([
      statusAlertApi().getStatusAlerts(),
      vwbSystemNotificationAdminApi()
        .listVwbSystemNotifications()
        .catch((e) => {
          console.error('Error loading Verily Workbench notifications: ', e);
          setError(
            'Could not load Verily Workbench notifications. All of Us banners are shown below.'
          );
          return [];
        }),
    ]);
    setBanners(toBannerRows(statusAlerts, vwbNotifications));
  };

  useEffect(() => {
    const initialLoad = async () => {
      props.hideSpinner();
      setLoading(true);
      try {
        await loadBanners();
      } catch (e) {
        console.error('Error loading banners: ', e);
        setError('Failed to load banners.');
        setLoading(false);
      }
    };

    initialLoad();
  }, []);

  useEffect(() => {
    setLoading(false);
  }, [banners]);

  const deleteBanner = async (row: BannerRow) => {
    try {
      setLoading(true);
      setError(null);
      if (row.isVwb) {
        // Removes the notification in Verily Workbench as well as our record of it.
        await vwbSystemNotificationAdminApi().deleteVwbSystemNotification(
          row.vwbNotificationId
        );
      } else {
        await statusAlertApi().deleteStatusAlert(row.statusAlertId);
      }
      await loadBanners();
    } catch (e) {
      console.error('Error deleting banner:', e);
      setError('Failed to delete the banner. Please try again.');
      setLoading(false);
    }
  };

  const actionBodyTemplate = (rowData: BannerRow) => {
    return (
      <TooltipTrigger
        content={
          rowData.isVwb
            ? 'Delete Verily Workbench Notification'
            : 'Delete Banner'
        }
      >
        <IconButton
          label='Delete'
          icon={TrashCan}
          onClick={() => deleteBanner(rowData)}
          style={{ height: '1.5rem', width: '1.5rem' }}
        />
      </TooltipTrigger>
    );
  };

  const handleCreateBanner = async () => {
    try {
      setError(null);
      await statusAlertApi().postStatusAlert(newBanner);
      await loadBanners();
      setShowCreateModal(false);
      setNewBanner(getDefaultStatusAlert());
    } catch (e) {
      console.error('Error creating banner:', e);
      setError('Failed to create the banner. Please try again.');
    }
  };

  const handleCreateVwbNotification = async () => {
    try {
      setError(null);
      await vwbSystemNotificationAdminApi().createVwbSystemNotification(
        newVwbNotification
      );
      await loadBanners();
      setShowVwbCreateModal(false);
      setNewVwbNotification(getDefaultVwbSystemNotification());
    } catch (e) {
      console.error('Error creating Verily Workbench notification:', e);
      setError(
        'Failed to create the Verily Workbench notification. Please try again.'
      );
    }
  };

  const handleCloseVwbModal = () => {
    setShowVwbCreateModal(false);
    setNewVwbNotification(getDefaultVwbSystemNotification());
  };

  const handleCloseModal = () => {
    setShowCreateModal(false);
    setNewBanner(getDefaultStatusAlert());
  };

  const messageBodyTemplate = (rowData: BannerRow) => {
    return (
      <span style={styles.messageText} title={rowData.message}>
        {rowData.message}
      </span>
    );
  };

  const linkBodyTemplate = (rowData: BannerRow) => {
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
        <Button
          type='secondary'
          onClick={() => setShowVwbCreateModal(true)}
          style={{ maxWidth: 'none' }}
        >
          Create Verily Workbench Banner
        </Button>
        <Button onClick={() => setShowCreateModal(true)}>
          Create New Banner
        </Button>
      </div>
      {error && (
        <div style={{ color: colors.danger, marginBottom: '1rem' }}>
          {error}
        </div>
      )}
      <div style={styles.tableContainer}>
        <DataTable
          value={banners}
          dataKey='key'
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
            field='isVwb'
            header='Verily Workbench'
            style={styles.sourceCol}
            body={(rowData: BannerRow) => (rowData.isVwb ? 'Yes' : 'No')}
            sortable
          />
          <Column
            field='alertLocation'
            header='Location'
            style={styles.colStyle}
            body={(rowData: BannerRow) =>
              // Location is an All of Us banner concept; Verily Workbench notifications are
              // always shown inside the app.
              rowData.isVwb
                ? '-'
                : rowData.alertLocation === StatusAlertLocation.BEFORE_LOGIN
                ? 'Before Login'
                : 'After Login'
            }
            sortable
          />
          <Column
            field='startTimeEpochMillis'
            header='Start Time (Local)'
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
          onClose={handleCloseModal}
          onCreate={handleCreateBanner}
        />
      )}

      {showVwbCreateModal && (
        <AdminVwbSystemNotificationModal
          notification={newVwbNotification}
          setNotification={setNewVwbNotification}
          onClose={handleCloseVwbModal}
          onCreate={handleCreateVwbNotification}
        />
      )}
    </div>
  );
};
