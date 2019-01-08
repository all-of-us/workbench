import * as React from 'react';

export const styles = {
  template: (props) => {
    return {
      backgroundImage:  calculateImage(),
      backgroundColor: '#dedfe1',
      backgroundRepeat: 'no-repeat',
      width: '100%',
      minHeight: '100vh',
      backgroundSize: props.windowsize.width <= 900 ? '0% 0%' : 'contain',
      backgroundPosition: calculateBackgroundPosition()
    };

    function calculateImage() {
      if (props.windowsize.width > 900 && props.windowsize.width <= 1300) {
        return 'url(\'' + props.images.smallerBackgroundImgSrc + '\')';
      }
      return 'url(\'' + props.images.backgroundImgSrc + '\')';
    }

    function calculateBackgroundPosition() {
      if (props.windowsize.width > 900 && props.windowsize.width <= 1300) {
         return 'bottom right' ;
      }
      return 'bottom right -1rem';
    }
  },
  headerImage: {
    height: '1.75rem',
    marginLeft: '1rem',
    marginTop: '1rem'
  },
  content: {
    flex: '0 0 41.66667%',
    maxWidth: '41.66667%',
    minWidth: '25rem'
  },
  signedInContainer: {
    backgroundSize: 'contain',
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'center',
    display: 'flex',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    width: 'auto'
  },

};


export const Template = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.template(props), ...style}}/>;

export const Header = ({style = {}, ...props}) =>
  <img {...props} style={{...styles.headerImage, ...style}}/>;

export const Content = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.content, ...style}}/>;

export const SignedIn = ({style = {}, ...props}) =>
    <div {...props} style={{...styles.signedInContainer, ...style}}/>;