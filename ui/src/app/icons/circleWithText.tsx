import colors from 'app/styles/colors';
import * as React from 'react';

export const CircleWithText = (props) => {
  return <svg width='26' height='26' viewBox='0 0 36 36' xmlns='http://www.w3.org/2000/svg'
              {...props}>
    <path d='M18,4A14,14,0,1,0,32,18,14,14,0,0,0,18,4Z' className='clr-i-solid clr-i-solid-path-1'/>
      <rect width='36' height='36' fillOpacity='0' />
      <text x='14' y='24' style={{fill: colors.white}}>{props.text}</text>
   </svg>;
};
