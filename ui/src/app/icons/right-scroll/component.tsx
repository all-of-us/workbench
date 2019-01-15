import {Component, Input} from '@angular/core';

import * as React from 'react';

import {ReactWrapperBase} from 'app/utils';

interface RightScrollState {
  style: object;
}

const defaultStyle = {
  width: 47,
  height: 48
};

const hoverStyle = {fill: '#72B9E2'};

export class RightScrollReact extends React.Component<{}, RightScrollState> {

  constructor(props) {
    super(props);
    this.state = {
        style: defaultStyle
    };
  }

  mouseOver(): void {
    this.setState({style: {...defaultStyle, ...hoverStyle}});
  }

  mouseLeave(): void {
    this.setState({style: defaultStyle});
  }

  render() {
    return (
      <svg
        style={this.state.style}
        viewBox='0 0 47 48'
        version='1.1'
        xmlns='http://www.w3.org/2000/svg'
        xmlnsXlink='http://www.w3.org/1999/xlink'
        onMouseOver={() => this.mouseOver()}
        onMouseLeave={() => this.mouseLeave()}>
        <defs>
          <circle id='path-1' cx='21.5' cy='21.5' r='21.5'>
          </circle>
          <filter x='-14.0%' y='-7.0%' width='127.9%' height='127.9%'
                  filterUnits='objectBoundingBox' id='filter-2'>
            <feOffset dx='0' dy='1' in='SourceAlpha' result='shadowOffsetOuter1'>
            </feOffset>
            <feGaussianBlur stdDeviation='1'
                            in='shadowOffsetOuter1' result='shadowBlurOuter1'>
            </feGaussianBlur>
            <feColorMatrix values='0 0 0 0 0   0 0 0 0 0   0 0 0 0 0  0 0 0 0.12 0'
                           type='matrix' in='shadowBlurOuter1'
                           result='shadowMatrixOuter1'>
            </feColorMatrix>
            <feOffset dx='0' dy='2' in='SourceAlpha' result='shadowOffsetOuter2'>
            </feOffset>
            <feGaussianBlur stdDeviation='1' in='shadowOffsetOuter2'
                            result='shadowBlurOuter2'>
            </feGaussianBlur>
            <feColorMatrix values='0 0 0 0 0   0 0 0 0 0   0 0 0 0 0  0 0 0 0.12 0'
                           type='matrix' in='shadowBlurOuter2'
                           result='shadowMatrixOuter2'>
            </feColorMatrix>
            <feMerge>
              <feMergeNode in='shadowMatrixOuter1'>
              </feMergeNode>
              <feMergeNode in='shadowMatrixOuter2'>
              </feMergeNode>
            </feMerge>
          </filter>
        </defs>
        <g id='Homepage' stroke='none' strokeWidth='1' fill='none' fillRule='evenodd'>
          <g transform='translate(-1131.000000, -667.000000)' id='scroll'>
            <g transform='translate(1133.000000, 668.000000)'>
              <g id='Oval-3'>
                <use fill='black' fillOpacity='1' filter='url(#filter-2)' xlinkHref='#path-1'>
                </use>
                <use fill='#2691D0' fillRule='evenodd' xlinkHref='#path-1'>
                </use>
              </g>
              <g id='ic_chevron_right_24px-copy'
                 transform='translate(17.000000, 14.000000)'
                 fill='#FFFFFF' fillRule='nonzero'>
                <polygon id='Shape'
                         points='1.90283401 0 0 1.88 6.18083671 8 0 14.12 1.90283401 16 10 8'>
                </polygon>
              </g>
            </g>
          </g>
        </g>
      </svg>
    );
  }
}

@Component({
  selector: 'app-right-scroll-icon',
  template: '<div #root></div>'
})
export class RightScrollComponent extends ReactWrapperBase {
  constructor() {
    super(RightScrollReact, []);
  }
}
