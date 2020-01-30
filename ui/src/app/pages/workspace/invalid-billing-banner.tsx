import {Button} from 'app/components/buttons';
import {StatusAlertBanner} from 'app/components/status-alert-banner';
import {withCurrentWorkspace, withUserProfile} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {openZendeskWidget} from 'app/utils/zendesk';
import {Profile} from 'generated/fetch';
import * as fp from 'lodash';
import * as React from 'react';

interface Props {
  workspace: WorkspaceData;
  profileState: {
    profile: Profile
  };
  onClose: Function;
}

export const InvalidBillingBanner = fp.flow(
  withCurrentWorkspace(),
  withUserProfile(),
)((props: Props) => {
  return <StatusAlertBanner
    onClose={() => props.onClose()}
    title={'This workspace has run out of free credits'}
    message={'The free credits for the creator of this workspace have run out or expired. ' +
    'Please provide a valid billing account or contact support to extend free credits.'}
    footer={
      <div style={{display: 'flex', flexDirection: 'column'}}>
        <Button style={{height: '38px', width: '70%', fontWeight: 400}}
                onClick={() => {
                  openZendeskWidget(
                    props.profileState.profile.givenName,
                    props.profileState.profile.familyName,
                    props.profileState.profile.username,
                    props.profileState.profile.contactEmail,
                  );
                }}
        >
          Request Extension
        </Button>
        <a style={{marginTop: '.5rem', marginLeft: '.2rem'}}
           onClick={() => {
             navigate(['workspaces', props.workspace.namespace, props.workspace.id, 'edit']);
           }}
        >
          Provide billing account
        </a>
      </div>
    }
  />;
});
