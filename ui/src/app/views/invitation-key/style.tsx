import * as React from 'react';

import {Button} from '../../components/buttons';

export const styles = {
  button: {
    width: '10rem',
    height: '2rem',
    margin: '.25rem .5rem .25rem 0'
  }
};

export const NextButton = ({style = {}, ...props}) =>
    <Button {...props} style={{...style, ...styles.button}}/>;
