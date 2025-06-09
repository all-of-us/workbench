import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { UserDisabledEvent } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import { displayDate } from 'app/utils/dates';

export interface AdminUserDisabledEventsProps {
  accountDisabledStatus: boolean;
  targetUserId: number;
}

export const AdminUserDisabledEvents = (
  props: AdminUserDisabledEventsProps
) => {
  const { accountDisabledStatus, targetUserId } = props;
  const [disabledEventsList, setDisabledEventsList] = useState<
    UserDisabledEvent[]
  >([]);

  const loadDisabledEvents = () => {
    userAdminApi()
      .listUserDisabledEvents(targetUserId)
      .then((res) => setDisabledEventsList(res.disabledEvents));
  };

  useEffect(loadDisabledEvents, [accountDisabledStatus]);

  return (
    <FlexRow style={{ minWidth: '100rem' }}>
      <FlexColumn style={{ width: '60%', justifyContent: 'space-between' }}>
        <FlexRow style={{ paddingTop: '1rem', paddingBottom: '1rem' }}>
          <DataTable value={disabledEventsList}>
            <Column
              field='updateTime'
              header='Updated'
              body={(row, opt) => <div>{displayDate(row[opt.field])}</div>}
            />
            <Column field={'updatedBy'} header={'Updated By'} />
            <Column
              field={'adminComment'}
              header={'Admin Comment'}
              body={(disabledEvent) => (
                <div
                  style={{
                    width: '300px',
                    overflowX: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {disabledEvent.adminComment}
                </div>
              )}
            />
            <Column field={'status'} header={'Status'} />
          </DataTable>
        </FlexRow>
      </FlexColumn>
    </FlexRow>
  );
};
