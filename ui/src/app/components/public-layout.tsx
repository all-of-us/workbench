import {Component} from '@angular/core';
import {StyledAnchorTag} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header, SmallHeader} from 'app/components/headers';
import {AouTitle} from 'app/components/text-wrappers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';

const styles = reactStyles({
  content: {
    color: colors.primary,
    margin: '1rem'
  }
});

export const PUBLIC_HEADER_IMAGE = '/assets/images/all-of-us-logo.svg';

export const PublicLayout = (props) => {
  return <React.Fragment>
    <div style={{width: '100%', height: '3.5rem',
                 borderBottom: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`}}>
      <a href="/">
        <img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
          src={PUBLIC_HEADER_IMAGE}/>
      </a>
    </div>
    <div style={styles.content}>
      {props.children}
    </div>
  </React.Fragment>;
};
