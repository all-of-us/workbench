import * as React from 'react';

import {StyledAnchorTag} from 'app/components/buttons';
import {BoldHeader} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import colors from 'app/styles/colors';
import {buildPageTitleForEnvironment} from 'app/utils/title';

const supportUrl = 'support@researchallofus.org';

export class UserDisabled extends React.Component {
  // TODO: esteemed reviewer, please yell at me if I don't write a paper about a global wrapper that
  // handles this sort of thing before turning this PR in for review
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
