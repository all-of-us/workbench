import {Button, Secondarybutton} from 'app/components/buttons';

import * as React from 'react';

export const styles = {
  sign: {
    backgroundSize: 'contain',
    backgroundRepeat: 'noRepeat',
    backgroundPosition: 'center',
    display: 'flex',
    justifyContent: 'spaceAround',
    alignItems: 'flexStart',
    width: 'auto'
  },

  icon: {
    height: '54px',
    width: '54px',
    /*The image file comes with about 5 pixels of padding around it, which we want to trim off*/
    margin: '-3px 19px -3px -3px'
  },

  logoImage: {
    width: '33rem',
    height: 'auto'
  },

  button: {
    marginTop: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    height: 'auto',
    paddingLeft: '0',
    fontSize: '18px',
    fontStyle: 'normal',
    textTransform: 'none',
    borderRadius: '2px',
    justifyContent: 'baseline',
    maxWidth: '11.45rem'
 },

  secondaryButton: {
    fontSize: '10px',
    margin: '.25rem .5rem .25rem 0'
  }
};

export const GoogleIcon = ({style = {}, ...props}) =>
    <img src={props.src} style={{ ...styles.icon , ...style}}/>;

export const LoginButton = ({style = {}, ...props}) =>
    <Button {...props} style={{...style, ...styles.button}}/>;

export const SecondaryLoginbutton = ({style = {}, ...props}) =>
    <Secondarybutton {...props} style={{...style, ...styles.secondaryButton}}/>;
