import * as React from 'react';
import * as fp from 'lodash/fp';

import { Button, IconButton } from 'app/components/buttons';
import { Check, ClrIcon, Times } from 'app/components/icons';
import { Toggle } from 'app/components/inputs';
import { PopupTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { serverConfigStore } from 'app/utils/stores';
import { AccessModule, AdminTableUser } from 'generated/fetch';
import { TextArea, TextInputWithLabel } from 'app/components/inputs';
import {ReactFragment, useState} from 'react';
import { withErrorModal } from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { reactStyles } from 'app/utils';

export const AdminUserAccess = () => {
  const styles = reactStyles({
    accessContainer: {
      width: '31rem',
      height: '20rem',
      borderRadius: '0.31rem',
      backgroundColor: 'rgba(33,111,180,0.1)',
      marginBottom: '1rem',
    },
  });

  const [userEmails, setUserEmails] = useState<Array<string>>(String['']);
  const [cloudTaskNames, setcloudTaskNames] = useState<Array<string>>(String['']);

  const parseUserEmailInput = (input : string) => {

  }
  const sendBatchUpdateRequest = fp.flow(
    withErrorModal({
      title: 'Failed To send batch sync user access request',
      message: 'An error occurred. Please try again.',
    })
  )(async () => {
    await userAdminApi().batchSyncAccess({
      usernames: userEmails,
    });
  });

  return (
    <FlexColumn style={styles.accessContainer}>
      <h3>Sync User Access</h3>
    <FlexRow style={{ gap: '0.5rem' }}>
      <FlexColumn>
    <h3>User emails</h3>
    <TooltipTrigger
      content={`List of user emails, split by comma or newline.`}>
      <TextArea
        value={userEmails?.join(',\n')}
        data-test-id='user-access-email-list'
        onChange={v => parseUserEmailInput(v)}
      />
    </TooltipTrigger>
        </FlexColumn>
      <FlexColumn>
          <h3>Cloud task ids</h3>
          <TooltipTrigger
            content={`List of user emails, split by comma or newline.`}>
            <TextArea
              value={cloudTaskNames?.join(',\n')}
              data-test-id='user-access-cloud-task'
              onChange={v => parseUserEmailInput(v)}
            />
          </TooltipTrigger>
      </FlexColumn>
    </FlexRow>
      <FlexRow>
        <Button
          onClick={sendBatchUpdateRequest}
          type='primary'
        >
          Sync Access
        </Button>
      </FlexRow>
    </FlexColumn>
  );
}
