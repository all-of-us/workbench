import * as React from 'react';

export interface LeftScrollLightState {
  fillColor: string;
}

export class LeftScrollLightReact extends React.Component<{}, LeftScrollLightState> {

  constructor(props: {}) {
    super(props);
    this.state = {
      fillColor: '#5FAEE0'
    };
  }

  mouseOver(): void {
    this.setState({fillColor: '#1892E0'});
  }

  mouseLeave(): void {
    this.setState({fillColor: '#5FAEE0'});
  }

  render() {
    return <svg width='40' height='41' viewBox='0 0 40 41' xmlns='http://www.w3.org/2000/svg'
                xmlnsXlink='http://www.w3.org/1999/xlink' onMouseOver={() => this.mouseOver()}
                onMouseLeave={() => this.mouseLeave()}>
      <defs>
        <circle id='path-1' cx='18' cy='18' r='18' />
        <filter x='-11.1%' y='-8.3%' width='122.2%' height='125%' filterUnits='objectBoundingBox'
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
      <g id='Workspaces' fill='none' fillRule='evenodd' opacity='0.54'>
        <g id='About-Workspace' transform='translate(-232 -326)'>
          <g id='scroll' transform='rotate(180 135 183)'>
            <g id='Oval-3'>
              <use fill='#000' filter='url(#filter-2)' xlinkHref='#path-1' />
              <use fill={this.state.fillColor} xlinkHref='#path-1' />
            </g>
            <g id='ic_chevron_right_24px-copy' transform='translate(14.233 11.72)'
               fill='#FFF' fillRule='nonzero'>
              <polygon id='Shape' points='1.59307033 0 0 1.57395349 5.17465399 6.69767442 0
              11.8213953 1.59307033 13.3953488 8.37209302 6.69767442'
              />
            </g>
          </g>
        </g>
      </g>
    </svg>;
  }
}

