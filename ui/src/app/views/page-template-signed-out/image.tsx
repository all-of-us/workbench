import * as React from 'react';
import {withWindowSize} from '../../utils';

export const styles = {
  template: (props) => {
    return {
      backgroundImage: 'url(\'' + props.images.smallerBackgroundImgSrc + '\')',
      backgroundColor: '#dedfe1',
      backgroundRepeat: 'no-repeat',
      width: '100%',
      minHeight: '100vh',
      backgroundSize: props.windowsize.width <= 900 ? '0% 0%' : 'contain',
      backgroundPosition: (props.windowsize.width > 900 && props.windowsize.width <= 1300)
          ? 'bottom' : 'bottom right -1rem'
    };
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
  }
};

export const Template = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.template(props), ...style}}/>;

export const Header = ({style = {}, ...props}) =>
  <img {...props} style={{...styles.headerImage, ...style}}/>;

export const Signedin = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.content, ...style}}/>;