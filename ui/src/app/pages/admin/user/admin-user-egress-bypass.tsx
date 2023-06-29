import * as React from 'react';
import {useEffect, useState} from 'react';
import { Calendar } from 'primereact/calendar';
import { formatDate } from 'app/utils/dates';
import {
  AdminTableUser,
  CreateEgressBypassWindowRequest,
  EgressBypassWindow,
} from 'generated/fetch';
import { Button } from 'app/components/buttons';
import { SemiBoldHeader } from 'app/components/headers';
import {
  TextAreaWithLengthValidationMessage,
} from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import {
    userAdminApi
} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import { isDateValid } from 'app/utils/dates';
import {FlexColumn, FlexRow} from "app/components/flex";
import {reactStyles} from "app/utils";
import {commonStyles} from "./admin-user-common";

const styles = reactStyles({
    ...commonStyles,
    header: {
        color: colors.primary,
        fontSize: '18px',
        fontWeight: 600,
        padding: '1em',
    }
});

const MIN_BYPASS_DESCRIPTION = 10;
const MAX_BYPASS_DESCRIPTION = 4000;

interface Props {
    userId: number;
}
export const AdminUserEgressByPass = (props: Props) => {
  const [startTime, setStartTime] = useState(null);
  const [byPassDescription, setBypassDescription] = useState('');
  const [apiError, setApiError] = useState(false);
  const [currentEgressBypassWindow, setCurrentEgressBypassWindow] = useState(null);

  const invalidReason =
    !byPassDescription ||
    byPassDescription.length < MIN_BYPASS_DESCRIPTION ||
    byPassDescription.length > MAX_BYPASS_DESCRIPTION;

  const egressBypassButtonDisabled =
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

    useEffect(() => {
        userAdminApi()
            .getEgressBypassWindow(props.userId)
            .then((bypassWindow) =>
                setCurrentEgressBypassWindow(bypassWindow)
            );
    }, []);

  const onCreateBypassRequest = () => {
    const { userId } = props;
    const createEgressBypassWindowRequest: CreateEgressBypassWindowRequest = {
      startTime: startTime.valueOf(),
      byPassDescription,
    };

    userAdminApi()
      .createEgressBypassWindow(userId, createEgressBypassWindowRequest)
      .catch(() => {
        setApiError(true);
      });
  };

  return (
      <FlexRow>
    <FlexColumn style={{ width: '60%' }}>
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
        <FlexRow>
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
        </FlexRow>

        {/* Bypass request Date*/}
        <FlexRow>
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
            onChange={(e) => {
              setApiError(false);
              setStartTime(e.value);
            }}
          />
        </FlexRow>
        <FlexRow>
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
    </FlexColumn>
      <FlexColumn style={{ marginLeft: '8%', width: '50%' }}>
          <FlexRow><h3>Current Egress Large Download Window</h3></FlexRow>
        <FlexRow>
        {/* Current Bypass date*/}
        {currentEgressBypassWindow && (
          <div>
            <div
              style={{
                fontWeight: 'bold',
                color: colors.primary,
                paddingBottom: '0.45rem',
              }}
            >
              Start time
            </div>
            <div>{formatDate(currentEgressBypassWindow.startTime)}</div>
            <div
              style={{
                fontWeight: 'bold',
                color: colors.primary,
                paddingBottom: '0.45rem',
              }}
            >
              End time
            </div>
            <div>{formatDate(currentEgressBypassWindow.endTime)}</div>
            <div
              style={{
                fontWeight: 'bold',
                color: colors.primary,
                paddingBottom: '0.45rem',
              }}
            >
              Description time
            </div>
            <div>{currentEgressBypassWindow.description}</div>
          </div>
        )}
        </FlexRow>
          </FlexColumn>
      </FlexRow>
  );
};
