import * as React from 'react';
import { useEffect, useState } from 'react';
import { Calendar, CalendarChangeEvent } from 'primereact/calendar';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  CreateEgressBypassWindowRequest,
  EgressBypassWindow,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TextAreaWithLengthValidationMessage } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { displayDate, isDateValid, maybeToSingleDate } from 'app/utils/dates';

export const MIN_BYPASS_DESCRIPTION = 10;
export const MAX_BYPASS_DESCRIPTION = 4000;

export interface AdminUserEgressBypassProps {
  targetUserId: number;
}
export const AdminUserEgressBypass = (props: AdminUserEgressBypassProps) => {
  const { targetUserId } = props;

  const [apiError, setApiError] = useState(false);
  const [bypassing, setBypassing] = useState(false);
  const [startTime, setStartTime] = useState(new Date());
  const [bypassDescription, setBypassDescription] = useState('');
  const [bypassWindowsList, setBypassWindowsList] = useState<
    EgressBypassWindow[]
  >([]);

  const loadEgressWindows = () => {
    userAdminApi()
      .listEgressBypassWindows(targetUserId)
      .then((res) => setBypassWindowsList(res.bypassWindows));
  };

  useEffect(loadEgressWindows, []);

  const invalidReason =
    !bypassDescription ||
    bypassDescription.length < MIN_BYPASS_DESCRIPTION ||
    bypassDescription.length > MAX_BYPASS_DESCRIPTION;

  const egressBypassButtonDisabled =
    bypassing || apiError || invalidReason || !isDateValid(startTime);

  const toolTipContent = apiError ? (
    'Error occurred while creating egress bypass request'
  ) : (
    <div>
      Required to enable large file downloads:
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
    setBypassing(true);
    const createEgressBypassWindowRequest: CreateEgressBypassWindowRequest = {
      startTime: startTime.valueOf(),
      byPassDescription: bypassDescription,
    };
    setBypassDescription('');

    userAdminApi()
      .createEgressBypassWindow(targetUserId, createEgressBypassWindowRequest)
      .catch(() => {
        setApiError(true);
      })
      .finally(() => {
        setBypassing(false);
        loadEgressWindows();
      });
  };

  return (
    <FlexRow style={{ minWidth: '100rem' }}>
      <FlexColumn style={{ width: '60%', justifyContent: 'space-between' }}>
        <FlexRow>
          <h3>Enable Large File Downloads</h3>
        </FlexRow>
        <FlexRow>
          {apiError && (
            <label style={{ color: colors.danger }}>
              Something went wrong while enabling large file downloads.
            </label>
          )}
        </FlexRow>
        {/* Text area to enter the reason for large file download */}
        <FlexRow>
          <label
            htmlFor='Egress Bypass Description'
            style={{ fontWeight: 'bold', color: colors.primary }}
          >
            Enter description for large file download request.
          </label>
        </FlexRow>
        <FlexColumn>
          <TextAreaWithLengthValidationMessage
            id='Egress Bypass Description'
            textBoxStyleOverrides={{ width: '60%' }}
            heightOverride={{ height: '5rem' }}
            initialText={bypassDescription}
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
            <div>Bypass starting date and time (in your local time zone)</div>
            <div>Note: the end time is 48 hours after the start time.</div>
          </div>
        </FlexRow>
        <FlexRow>
          <Calendar
            value={startTime}
            showTime
            hourFormat='12'
            minDate={new Date()}
            onChange={(e: CalendarChangeEvent) => {
              setApiError(false);
              setStartTime(maybeToSingleDate(e.value));
            }}
          />
        </FlexRow>
        <FlexRow style={{ paddingTop: '1rem' }}>
          <TooltipTrigger
            content={toolTipContent}
            disabled={!egressBypassButtonDisabled}
          >
            <Button
              type='primary'
              onClick={() => onCreateBypassRequest()}
              disabled={egressBypassButtonDisabled}
              style={{ maxWidth: 'none' }}
            >
              Temporarily Enable Large File Downloads
            </Button>
          </TooltipTrigger>
        </FlexRow>
        <FlexRow>
          <h3>Large File Download Requests (all times local)</h3>
        </FlexRow>
        <FlexRow style={{ paddingTop: '1rem', paddingBottom: '1rem' }}>
          <DataTable value={bypassWindowsList}>
            <Column
              field='startTime'
              header='Start Time'
              body={(row, opt) => <div>{displayDate(row[opt.field])}</div>}
            />
            <Column
              field='endTime'
              header='End Time'
              body={(row, opt) => <div>{displayDate(row[opt.field])}</div>}
            />
            <Column field={'description'} header={'Description'} />
          </DataTable>
        </FlexRow>
      </FlexColumn>
    </FlexRow>
  );
};
