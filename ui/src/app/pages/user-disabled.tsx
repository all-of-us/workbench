import {Component} from '@angular/core';
import * as React from 'react';

import {StyledAnchorTag} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {backgroundStyleTemplate, SIGNED_OUT_HEADER_IMAGE, SignInStep, StepToImageConfig} from 'app/pages/login/sign-in';
import colors from 'app/styles/colors';
import {
  ReactWrapperBase, WindowSizeProps,
  withWindowSize
} from 'app/utils';

import * as fp from 'lodash/fp';

const styles = {
  disabledContainer: {
    backgroundSize: 'contain',
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'center',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    width: 'auto',
    minHeight: '100vh'
  }
};

const supportUrl = 'support@researchallofus.org';

export const UserDisabled = fp.flow(withWindowSize())
(class extends React.Component<WindowSizeProps, {}> {
  render() {
    return <div style={styles.disabledContainer}>
      <FlexColumn style={{...backgroundStyleTemplate(this.props.windowSize, StepToImageConfig.get(SignInStep.LANDING)),
        minHeight: '100vh'}}>
        <div style={{marginLeft: '1rem'}}>
          <div><img style={{height: '1.75rem', marginTop: '1rem'}}
                    src={SIGNED_OUT_HEADER_IMAGE}/></div>
          <div style={{marginTop: '1rem'}}>
            <Header style={{fontSize: 28, fontWeight: 400}}>Your account has been disabled</Header>
            <div style={{fontSize: 18, color: colors.primary, marginTop: '1rem'}}>Contact <StyledAnchorTag
              href={'mailto:' + supportUrl}>{supportUrl}</StyledAnchorTag><br/> for more information.</div>
          </div>
        </div>
      </FlexColumn>
    </div>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class UserDisabledComponent extends ReactWrapperBase {
  constructor() {
    super(UserDisabled, []);
  }
}
