import * as React from 'react';
import { useEffect, useState } from 'react';
import { Calendar } from 'primereact/calendar';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { CreateEgressBypassWindowRequest } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TextAreaWithLengthValidationMessage } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { formatDate, isDateValid } from 'app/utils/dates';
const MIN_BYPASS_DESCRIPTION = 10;
const MAX_BYPASS_DESCRIPTION = 4000;

interface Props {
  userId: number;
}

export const AdminUserEgressByPass = (props: Props) => {
  const [startTime, setStartTime] = useState(null);
  const [byPassDescription, setBypassDescription] = useState('');
  const [apiError, setApiError] = useState(false);
  const [bypassWindowsList, setBypasswindowsList] = useState([]);
  const [reload, setReload] = useState(false);

  useEffect(() => {
    const { userId } = props;
    userAdminApi()
      .listEgressBypassWindows(userId)
      .then((res) => setBypasswindowsList(res.bypassWindows));
  }, [reload]);

  const invalidReason =
    !byPassDescription ||
    byPassDescription.length < MIN_BYPASS_DESCRIPTION ||
    byPassDescription.length > MAX_BYPASS_DESCRIPTION;

  let egressBypassButtonDisabled =
    apiError || invalidReason || !isDateValid(startTime);

  const getToolTipContent = apiError ? (
    'Error occurred while Locking Workspace'
  ) : (
    <div>
      Required to lock workspace:
      <ul>
        {invalidReason && (
          <li>
            Request Reason (minimum length {MIN_BYPASS_DESCRIPTION}, maximum{' '}
            {MAX_BYPASS_DESCRIPTION})
          </li>
        )}
        {!isDateValid(startTime) && (
          <li>Valid Request Date (in YYYY-MM-DD Format)</li>
        )}
      </ul>
    </div>
  );

  const onCreateBypassRequest = () => {
    const { userId } = props;
    egressBypassButtonDisabled = true;
    const createEgressBypassWindowRequest: CreateEgressBypassWindowRequest = {
      startTime: startTime.valueOf(),
      byPassDescription,
    };

    userAdminApi()
      .createEgressBypassWindow(userId, createEgressBypassWindowRequest)
      .catch(() => {
        setApiError(true);
      })
      .finally(() => setReload(true));
  };

  const displayTime = (row, opt) => {
    return <div style={{ width: '7rem' }}>{formatDate(row[opt.field])}</div>;
  };

  return (
    <FlexRow>
      <FlexColumn style={{ width: '60%', justifyContent: 'space-between' }}>
        <FlexRow>
          <h3>Enable Large Download</h3>
        </FlexRow>
        <FlexRow>
          {apiError && (
            <label style={{ color: colors.danger }}>
              Something went wrong while enabling large download.
            </label>
          )}
        </FlexRow>
        {/* Text area to enter the reason for large file download */}
        <FlexRow>
          <label style={{ fontWeight: 'bold', color: colors.primary }}>
            Enter description for researchers on why request enabling large file
            download
          </label>
        </FlexRow>
        <FlexColumn>
          <TextAreaWithLengthValidationMessage
            id='BYPASS-DESCRIPTION'
            textBoxStyleOverrides={{ width: '60%' }}
            heightOverride={{ height: '5rem' }}
            initialText=''
            maxCharacters={MAX_BYPASS_DESCRIPTION}
            onChange={(s: string) => {
              setApiError(false);
              setBypassDescription(s);
            }}
            tooShortWarningCharacters={MIN_BYPASS_DESCRIPTION}
            tooShortWarning={`Bypass Egress Request Reason should be at least ${MIN_BYPASS_DESCRIPTION} characters long`}
          />
        </FlexColumn>

        {/* Bypass request Date*/}
        <FlexRow style={{ paddingTop: '1rem' }}>
          <div
            style={{
              fontWeight: 'bold',
              color: colors.primary,
              paddingBottom: '0.45rem',
            }}
          >
            By pass staring date. (end date is 48 hours after starting time)
          </div>
        </FlexRow>
        <FlexRow>
          <Calendar
            value={startTime}
            showTime
            hourFormat='12'
            minDate={new Date()}
            onChange={(e) => {
              setApiError(false);
              setStartTime(e.value);
            }}
          />
        </FlexRow>
        <FlexRow style={{ paddingTop: '1rem' }}>
          <TooltipTrigger
            content={getToolTipContent}
            disabled={!egressBypassButtonDisabled}
          >
            <Button
              type='primary'
              onClick={() => onCreateBypassRequest()}
              disabled={egressBypassButtonDisabled}
            >
              ENABLE LARGE DOWNLOAD
            </Button>
          </TooltipTrigger>
        </FlexRow>
        <FlexRow style={{ paddingTop: '1rem' }}>
          <DataTable value={bypassWindowsList}>
            <Column
              field={'startTime'}
              header={'Start Time'}
              body={(date, opt) => displayTime(date, opt)}
            />
            <Column
              field={'endTime'}
              header={'End Time'}
              body={(date, opt) => displayTime(date, opt)}
            />
            <Column field={'description'} header={'Description'} />
          </DataTable>
        </FlexRow>
      </FlexColumn>
    </FlexRow>
  );
};
