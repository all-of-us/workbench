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
      let imageUrl = 'url(\'' + images.backgroundImgSrc + '\')';
      if (windowsize.width > 900 && windowsize.width <= 1300) {
        imageUrl = 'url(\'' + images.smallerBackgroundImgSrc + '\')';
      }
      return imageUrl;
    }

    function calculateBackgroundPosition() {
      let position = 'bottom right -1rem';
      if (windowsize.width > 900 && windowsize.width <= 1300) {
        position = 'bottom right' ;
      }
      return position;
    }
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
