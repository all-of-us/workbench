import {Component, Input} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';

import * as React from 'react';
import {LeftScrollLightReact} from '../left-scroll-light/component';
import {LeftScrollReact} from '../left-scroll/component';

interface ScrollProps {
  direction: string;
}

export const ScrollReact: React.FunctionComponent<{direction: string}> = ({direction}) => {
     return  <div>
       {
         (() => {
           switch (direction) {
             case 'left':
               return <LeftScrollReact/>;
             case 'leftLight':
               return <LeftScrollLightReact/>;
           }
         })()
       }
       </div>;

};

@Component({
  selector: 'app-scroll-icon',
  template: '<div #root></div>'
})
export class ScrollComponent extends ReactWrapperBase {
  @Input('direction') direction: ScrollProps['direction'];

  constructor() {
    super(ScrollReact, ['direction']);
  }
}
