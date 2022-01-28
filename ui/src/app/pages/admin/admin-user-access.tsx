import * as React from 'react';
import * as fp from 'lodash/fp';

import { Button } from 'app/components/buttons';
import { SpinnerOverlay } from 'app/components/spinners';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import { TextArea } from 'app/components/inputs';
import { useEffect, useState } from 'react';
import { withErrorModal } from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { reactStyles } from 'app/utils';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
export const AdminUserAccess = (spinnerProps: WithSpinnerOverlayProps) => {
  const styles = reactStyles({
    accessContainer: {
      width: '25rem',
      height: '15rem',
      borderRadius: '0.31rem',
      backgroundColor: 'rgba(33,111,180,0.1)',
      marginLeft: '0.2rem',
      marginTop: '1rem',
      marginBottom: '1rem',
    },
    textArea: {
      width: '12rem',
      height: '8rem',
      borderRadius: '0.31rem',
    },
  });

  useEffect(() => spinnerProps.hideSpinner(), []);

  const [sendingRequest, setSendingRequest] = useState<boolean>(false);
  const [userEmails, setUserEmails] = useState<Array<string>>(String['']);
  const [cloudTaskNameList, setcloudTaskNameList] = useState<Array<string>>(
    String['']
  );

  const parseUserEmailInput = (input: string) => {
    setUserEmails(input?.split(/[,\n]+/).map((email) => email.trim()));
  };
  const sendBatchUpdateRequest = fp.flow(
    withErrorModal({
      title: 'Failed To send batch sync user access request',
      message: 'An error occurred. Please try again.',
    })
  )(async () => {
    setSendingRequest(true);
    const { cloudTaskNames } = await userAdminApi().batchSyncAccess({
      usernames: userEmails,
    });
    setcloudTaskNameList(cloudTaskNames);
    setSendingRequest(false);
  });

  return (
    <FlexColumn style={styles.accessContainer}>
      <h3>Sync User Access</h3>
      <FlexRow style={{ gap: '0.5rem' }}>
        <FlexColumn>
          <h3>User emails</h3>
          <TooltipTrigger
            content={`List of user emails, split by newline.`}
            side='left'
          >
            <TextArea
              style={styles.textArea}
              value={userEmails?.join(',\n')}
              data-test-id='user-access-email-list'
              onChange={(v) => parseUserEmailInput(v)}
            />
          </TooltipTrigger>
        </FlexColumn>
        <FlexColumn>
          <h3>Cloud task ids</h3>
          <TextArea
            style={styles.textArea}
            value={cloudTaskNameList?.join(',\n')}
            onChange={null}
            disabled={true}
          />
        </FlexColumn>
      </FlexRow>
      <FlexRow>
        <Button
          onClick={sendBatchUpdateRequest}
          type='primary'
          style={{
            marginTop: '10px',
            marginLeft: '20px',
            fontWeight: 400,
            height: '38px',
            width: '150px',
          }}
          disabled={sendingRequest}
        >
          Sync Access
        </Button>
      </FlexRow>
      {sendingRequest && <SpinnerOverlay />}
    </FlexColumn>
  );
};
