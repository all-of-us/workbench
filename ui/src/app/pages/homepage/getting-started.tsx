import * as React from 'react';

import colors, {addOpacity} from 'app/styles/colors';
import {CustomBulletList, CustomBulletListItem} from 'app/components/lists';
import {StyledAnchorTag} from 'app/components/buttons';
import {AoU} from 'app/components/text-wrappers';
import {supportUrls} from 'app/utils/zendesk';

export const GettingStarted = () => {
    return <div data-test-id='getting-started'
                style={{
                    backgroundColor: addOpacity(colors.primary, .1).toString(),
                    color: colors.primary,
                    borderRadius: 10,
                    margin: '2em 0em'}}>
        <div style={{margin: '1em 2em'}}>
            <h2 style={{fontWeight: 600, marginTop: 0}}>Here are some tips to get you started:</h2>
            <CustomBulletList>
                <CustomBulletListItem bullet='→'>
                    Create a <StyledAnchorTag href='https://support.google.com/chrome/answer/2364824'
                                              target='_blank'>Chrome Profile</StyledAnchorTag> with your <AoU/> Researcher
                    Workbench Google account. This will keep your workbench browser sessions isolated from
                    your other Google accounts.
                </CustomBulletListItem>
                <CustomBulletListItem bullet='→'>
                    Check out <StyledAnchorTag href='library'>Featured Workspaces</StyledAnchorTag> from
                    the left hand panel to browse through example workspaces.
                </CustomBulletListItem>
                <CustomBulletListItem bullet='→'>
                    Browse through our <StyledAnchorTag href={supportUrls.helpCenter}
                                                        target='_blank'>support materials</StyledAnchorTag> and forum topics.
                </CustomBulletListItem>
            </CustomBulletList>
        </div>
    </div>;
}