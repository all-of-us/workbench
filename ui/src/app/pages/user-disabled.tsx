import * as React from 'react';

import {StyledAnchorTag} from 'app/components/buttons';
import {BoldHeader} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import colors from 'app/styles/colors';
import {buildPageTitleForEnvironment} from 'app/utils/title';

const supportUrl = 'support@researchallofus.org';

export class UserDisabled extends React.Component {
  componentDidMount() {
    document.title = buildPageTitleForEnvironment('Disabled');
  }

  render() {
    return <PublicLayout>
      <BoldHeader>Your account has been disabled</BoldHeader>
      <section style={{color: colors.primary, fontSize: '18px', marginTop: '.5rem'}}>
        Contact <StyledAnchorTag href={'mailto:' + supportUrl}>{supportUrl}</StyledAnchorTag> for
        more information.
      </section>
    </PublicLayout>;
  }
}
