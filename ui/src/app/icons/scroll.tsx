import {Clickable} from 'app/components/buttons';
import {ScrollIcon} from 'app/icons/scroll-icon';
import * as React from 'react';

export const Scroll = ({dir, shade, style = {}, ...props}) => {
  const transform = (dir === 'right') ? 'scaleX(-1)' : '';
  if (shade === 'light') {
    return <Clickable {...props}
      style={{color: '#5FAEE0', opacity: 0.54, transform, display: 'flex', ...style}}
      hover={{color: '#1892E0'}}
    ><ScrollIcon width={40} height={41} /></Clickable>;
  }
  return <Clickable {...props}
    style={{color: '#2691D0', opacity: 1, transform, display: 'flex', ...style}}
    hover={{color: '#72B9E2'}}
  ><ScrollIcon width={47} height={48} /></Clickable>;
};
