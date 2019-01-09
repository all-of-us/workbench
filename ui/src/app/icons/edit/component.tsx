import {Component, OnInit} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

interface EditComponentState { style: object; }

class EditComponentReact extends React.Component<{}, EditComponentState> {

  constructor(props) {
    super(props);
    this.state = {
      style: {}
    };
  }

  mouseOver(): void {
    this.setState({style: {fill: '#4D4CA5'}});
  }

  mouseLeave(): void {
    this.setState({style: {}});
  }

  render() {
    return (
      <svg className='icon'
           style={{width: '14px', height: '14px', ...this.state.style}}
           viewBox='0 0 14 14'
           version='1.1'
           xmlns='http://www.w3.org/2000/svg'
           xmlnsXlink='http://www.w3.org/1999/xlink'
           onMouseOver={() => this.mouseOver()}
           onMouseLeave={() => this.mouseLeave()}>
        <title>Edit</title>
        <desc>Created with Sketch.</desc>
        <defs></defs>
        <g id='Brand-styles' stroke='none' strokeWidth='1' fill='none' fillRule='evenodd'
           transform='translate(-764.000000, -3219.000000)'>
          <g id='Edit' transform='translate(764.000000, 3219.000000)' fill='#302C71'
             fillRule='nonzero'>
            <rect id='Rectangle-path' x='0.5' y='12.95' width='13' height='1'></rect>
            <path id='Shape'
                d='M12.52,3.47 C12.8955541,3.09486349 13.1065733,2.5858185 13.1065733,2.055
                   C13.1065733,1.5241815 12.8955541,1.01513651 12.52,0.64 C12.1448635,0.264445914
                   11.6358185,0.0534266872 11.105,0.0534266872 C10.5741815,0.0534266872
                   10.0651365,0.264445914 9.69,0.64 L2.62,7.71 L1.91,11.25 L5.45,10.54 L12.52,3.47
                   Z M3.19,10 L3.54,8.23 L10.4,1.35 C10.7900375,0.962276405 11.4199625,0.962276405
                   11.81,1.35 C12.1977236,1.74003745 12.1977236,2.36996255 11.81,2.76 L5,9.62
                   L3.19,10 Z'></path>
          </g>
        </g>
      </svg>
    );
  }
}

@Component({
  selector: 'app-edit-icon',
  templateUrl: './component.html',
})

export class EditComponent implements OnInit {

  constructor() {}

  ngOnInit(): void {
    this.renderComponent();
  }

  renderComponent() {
    ReactDOM.render(
      React.createElement(EditComponentReact, {}),
      document.getElementById('app-edit-icon'));
  }

}
