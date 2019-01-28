import {Component, Input} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';

import * as React from 'react';
import * as Interactive from 'react-interactive';
import {ScrollIconReact} from '../scroll-icon/component';

const FillColorId = '#2691D0';
const HoverFillColorId = '#72B9E2';
const LightFillColorId = '#5FAEE0';
const LightHoverFillColorId = '#1892E0';

const wrapper = ScrollIconReact;
export const ScrollReact: React.FunctionComponent<{icon: string, shade: string, style: object}> =
    ({icon, shade, style}) => {
      const transform = (icon === 'RightIcon') ? 'scaleX(-1)' : '';
      let color = FillColorId;
      let hoverColor = HoverFillColorId;
      let opacity = '1';
      let width = '47';
      let height = '48';

      if (shade === 'Light') {
        color = LightFillColorId;
        hoverColor = LightHoverFillColorId;
        opacity = '0.54';
        width = '40';
        height = '41';
      }

    return  <div>
      <Interactive as={wrapper}
        style={{color: color, opacity: opacity, transform: transform, ...style}}
        hover={{color: hoverColor}} width= {width} height={height}/>
    </div>;
};

@Component({
  selector: 'app-scroll-icon',
  template: '<div #root></div>'
})
export class ScrollComponent extends ReactWrapperBase {
  @Input('icon') icon: string;
  @Input('shade') shade: string;
  @Input('style') style: string;

  constructor() {
    super(ScrollReact, ['icon', 'shade', 'style']);
   }
}
