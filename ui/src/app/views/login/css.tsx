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
   fontWeight: 500,
   fontStyle: 'normal',
   borderRadius: '2px',
   backgroundColor: '#4356A7',
   color: '#fff'
 }
};

export const Google = ({style = {}, ...props}) =>
    <img src={props.src} style={{ ...styles.icon , ...style}}/>;

export const Button = ({style = {}, ...props}) =>
    <button {...props} style={{ ...styles.button, ...style}}/>;