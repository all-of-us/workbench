import * as React from 'react';

export const styles = {
  template: (windowsize, images) => {
    return {
      backgroundImage:  calculateImage(),
      backgroundColor: '#dedfe1',
      backgroundRepeat: 'no-repeat',
      width: '100%',
      minHeight: '100vh',
      backgroundSize: windowsize.width <= 900 ? '0% 0%' : 'contain',
      backgroundPosition: calculateBackgroundPosition()
    };

    function calculateImage() {
      if (windowsize.width > 900 && windowsize.width <= 1300) {
        return 'url(\'' + images.smallerBackgroundImgSrc + '\')';
      }
      return 'url(\'' + images.backgroundImgSrc + '\')';
    }

    function calculateBackgroundPosition() {
      if (windowsize.width > 900 && windowsize.width <= 1300) {
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
