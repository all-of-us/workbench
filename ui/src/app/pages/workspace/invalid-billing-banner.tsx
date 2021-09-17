import { RouteLink } from 'app/components/app-router';
import {Button} from 'app/components/buttons';
import {StatusAlertBanner} from 'app/components/status-alert-banner';
import {withCurrentWorkspace, withUserProfile} from 'app/utils';
import {serverConfigStore} from 'app/utils/stores';
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
  const {profileState: {profile: {givenName, familyName, username, contactEmail}}, workspace: {namespace, id}} = props;
  const {enableBillingUpgrade} = serverConfigStore.get().config;
  const userAction = enableBillingUpgrade ?
    'Please provide a valid billing account or contact support to extend free credits.' :
    'Please contact support to extend free credits.';
  return <StatusAlertBanner
    onClose={() => props.onClose()}
    title={'This workspace has run out of free credits'}
    message={'The free credits for the creator of this workspace have run out. ' + userAction}
    footer={
      <div style={{display: 'flex', flexDirection: 'column'}}>
        <Button style={{height: '38px', width: '70%', fontWeight: 400}}
                onClick={() => {
                  openZendeskWidget(
                    givenName,
                    familyName,
                    username,
                    contactEmail,
                  );
                }}
        >
          Request Extension
        </Button>
        {enableBillingUpgrade && <RouteLink style={{marginTop: '.5rem', marginLeft: '.2rem'}}
                                            path={`/workspaces/${namespace}/${id}/edit`}>
            Provide billing account
        </RouteLink>}
      </div>
    }
  />;
});
