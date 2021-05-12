import * as Color from 'color';
import {useEffect} from 'react';
import * as React from 'react';
import * as ReactModal from 'react-modal';

import colors from 'app/styles/colors';
import {reactStyles, withStyle} from 'app/utils/index';
import {useSpring} from 'react-spring';
import {Modal} from './modals';
import {SpinnerOverlay} from './spinners';


export const ExpandingModal = ({width, ...props}) => {

  // useEffect(() => {
  //
  // }, [width]);

  const expand = useSpring({
    config: { friction: 10 },
    width: width
  });

  console.log(expand.width);
  return <Modal
    {...props}
    width={expand.width}
  >
    {props.children}
    </Modal>;
};
