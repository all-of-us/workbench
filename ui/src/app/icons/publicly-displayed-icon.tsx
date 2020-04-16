import {InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';

const css = `
  .pointer {
     position: relative;
     height: 29px;
     width: 6.5rem;
     background-color: ` + colorWithWhiteness(colors.accent, 0.85) + `;
     line-height:1.2rem;
   }

  .pointer::after {
     position: absolute;
     left: 100%;
     top:0rem;
     width:0rem;
     height:0rem;
     border-top:0.6rem solid transparent;
     border-bottom:0.6rem solid transparent;
  }
  .pointer:before {
     content:"";
     position: absolute;
     right: 100%;
     top:0rem;
     width:0rem;
     height:0rem;
     border-top:0.6rem solid transparent;
     border-right:0.7rem solid ` + colorWithWhiteness(colors.accent, 0.85) + `;
     border-bottom:0.6rem solid transparent;
   }
 `;

export const PubliclyDisplayed = (props) => {
  return <div>
    <style>
      {css}
    </style>
    <div className='pointer' style={{...props.style}}>
      <label style={{marginLeft: '0.3rem', color: colors.accent}}>
        Public displayed
        <TooltipTrigger content='not sure about content'>
          <InfoIcon style={{marginLeft: '0.5rem', width: '0.7rem'}}/>
        </TooltipTrigger>
      </label>
    </div>
  </div>;
};
