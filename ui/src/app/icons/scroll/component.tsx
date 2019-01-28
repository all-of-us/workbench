import {Component, Input} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';

import {ScrollIcon} from 'app/icons/scroll-icon/component';
import * as React from 'react';
import * as Interactive from 'react-interactive';

const fillColorId = '#2691D0';
const hoverFillColorId = '#72B9E2';
const lightFillColorId = '#5FAEE0';
const lightHoverFillColorId = '#1892E0';

const wrapper = ScrollIcon;
export const Scroll: React.FunctionComponent<{icon: string, shade: string, style: object}> =
    ({icon, shade, style}) => {
      const transform = (icon === 'RightIcon') ? 'scaleX(-1)' : '';
      if (shade === 'Light') {
        return  <div>
          <Interactive as={wrapper}
            style={{color: lightFillColorId, opacity: '0.54', transform: transform, ...style}}
            hover={{color: lightHoverFillColorId}} width= '40' height='41'/>
        </div>;
      }

      return  <div>
        <Interactive as={wrapper}
          style={{color: fillColorId, opacity: '1', transform: transform, ...style}}
          hover={{color: hoverFillColorId}} width= '47' height='48'/>
      </div>;
    };

@Component({
  selector: 'app-scroll-icon',
  template: '<div #root></div>'
})
export class ScrollComponent extends ReactWrapperBase {
  @Input('icon') icon: string;
  @Input('shade') shade: string;
  @Input('style') style: object;

  constructor() {
    super(Scroll, ['icon', 'shade', 'style']);
  }
}
