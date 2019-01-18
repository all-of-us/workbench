import * as React from 'react';

export interface LeftScrollState {
  fillColor: string;
}

export class LeftScrollReact extends React.Component<{}, LeftScrollState> {

  constructor(props:{}) {
    super(props);
    this.state = {
      fillColor: '#2691D0'
    };
  }

  mouseOver(): void {
    this.setState({fillColor: '#72B9E2'});
  }

  mouseLeave(): void {
    this.setState({fillColor: '#2691D0'});
  }

  render() {
    return <svg width='47' height='48' viewBox='0 0 47 48' xmlns='http://www.w3.org/2000/svg'
                xmlnsXlink='http://www.w3.org/1999/xlink'
                onMouseOver={() => this.mouseOver()}
                onMouseLeave={() => this.mouseLeave()}>
      <defs>
        <circle id='path-1' cx='21.5' cy='21.5' r='21.5' />
        <filter x='-14%' y='-7%' width='127.9%' height='127.9%' filterUnits='objectBoundingBox'
                id='filter-2'>
          <feOffset dy='1' in='SourceAlpha' result='shadowOffsetOuter1' />
          <feGaussianBlur stdDeviation='1' in='shadowOffsetOuter1' result='shadowBlurOuter1'
          />
          <feColorMatrix values='0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.12 0' in='shadowBlurOuter1'
                         result='shadowMatrixOuter1' />
          <feOffset dy='2' in='SourceAlpha' result='shadowOffsetOuter2' />
          <feGaussianBlur stdDeviation='1' in='shadowOffsetOuter2' result='shadowBlurOuter2'
          />
          <feColorMatrix values='0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.12 0' in='shadowBlurOuter2'
                         result='shadowMatrixOuter2' />
          <feMerge>
            <feMergeNode in='shadowMatrixOuter1' />
            <feMergeNode in='shadowMatrixOuter2' />
          </feMerge>
        </filter>
      </defs>
      <g id='Homepage' fill='none' fillRule='evenodd'>
        <g transform='rotate(180 22.5 22)' id='scroll'>
          <g id='Oval-3' transform='rotate(180 21.5 21.5)'>
            <use fill='#000' filter='url(#filter-2)' xlinkHref='#path-1' />
            <use fill={this.state.fillColor} xlinkHref='#path-1' />
          </g>
          <g id='ic_chevron_right_24px-copy' transform='translate(17 14)' fill='#FFF'
             fillRule='nonzero'>
            <polygon id='Shape' points='1.90283401 0 0 1.88 6.18083671 8 0 14.12 1.90283401 16 10 8'
            />
          </g>
        </g>
      </g>
    </svg>;
  }
}
