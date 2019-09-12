import {Clickable} from 'app/components/buttons';
import {ScrollIcon} from 'app/icons/scroll-icon';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';

export const Scroll = ({dir, style = {}, ...props}) => {
  const transform = (dir === 'right') ? 'scaleX(-1)' : '';
  return <Clickable {...props}
    style={{color: colors.secondary, opacity: 1, transform, display: 'flex', ...style}}
    hover={{color: colorWithWhiteness(colors.secondary, 0.2)}}
  ><ScrollIcon width={47} height={48} /></Clickable>;
};
