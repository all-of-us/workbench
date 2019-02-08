import {reactStyles} from 'app/utils/index';
import * as React from 'react';

const styles = reactStyles({
  overlay: {
    backgroundColor: 'rgba(0, 0, 0, 0)',
    position: 'absolute', top: 0, left: 0, bottom: 0, right: 0,
    display: 'flex', justifyContent: 'center', alignItems: 'center'
  },
  square: {
    display: 'flex', borderRadius: 4, padding: '0.5rem'
  },
  darkBackground: {
    stroke: '#fff',
    strokeOpacity: 0.6
  }
});

export const Spinner = ({dark = false, style = {}, ...props}) => {
  return <svg
    xmlns='http://www.w3.org/2000/svg' viewBox='0 0 72 72' width={72} height={72}
    style={{animation: '1s linear infinite spin', ...style}} {...props}
  >
    <circle cx='36' cy='36' r='33' stroke={dark ? styles.darkBackground.stroke : '#000'}
            strokeOpacity={dark ? styles.darkBackground.strokeOpacity : '.1'}
            fill='none' strokeWidth='5' />
    <path d='M14.3 60.9A33 33 0 0 1 36 3' stroke='#0079b8' fill='none' strokeWidth='5' />
  </svg>;
};

export const SpinnerOverlay = ({dark = false}) => {
  return <div style={styles.overlay}>
    <div style={styles.square}><Spinner dark={dark} /></div>
  </div>;
};
