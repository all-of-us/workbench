import {Component} from '@angular/core';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';

export const styles = {
  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingLeft: '13px',
    color: '#2F2E7E',
    borderBottom: '1px solid #E5E5E5'
  },

  addIcon: {
    marginLeft: 19,
    fill: '#2691D0',
    verticalAlign: '-6%'
  }
};

export const DataSet = withCurrentWorkspace()(class extends React.Component<{}, {}> {

  constructor(props) {
    super(props);
  }

  render() {
    return <React.Fragment>
      <FadeBox style={{marginTop: '1rem'}}>
        <h2 style={{marginTop: 0}}>Datasets</h2>
        <div style={{color: '#000000', fontSize: '14px'}}>Build a dataset by selecting the
          variables and values for one or more of your cohorts. Then export the completed dataset
          to Notebooks where you can perform your analysis</div>
      </FadeBox>
      <div style={{display: 'flex'}}>
        <div style={{marginLeft: '1.5rem', marginRight: '1.5rem', width: '33%'}}>
          <h2>Select Cohorts</h2>
          <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
            <div style={styles.selectBoxHeader}>
              Cohorts
              <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}/>
            </div>
            {/*TODO: load cohorts and display here*/}
            <div style={{height: '8rem'}}/>
          </div>
        </div>
        <div style={{width: '58%'}}>
          <h2>Select Concept Sets</h2>
          <div style={{display: 'flex', backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
            <div style={{flexGrow: 1}}>
              <div style={{...styles.selectBoxHeader, borderRight: '1px solid #E5E5E5'}}>
                Concept Sets
                <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}/>
              </div>
              {/*TODO: load concept steps and display here*/}
              <div style={{height: '8rem', borderRight: '1px solid #E5E5E5'}}/>
            </div>
            <div style={{flexGrow: 1}}>
              <div style={styles.selectBoxHeader}>
                Values
              </div>
              {/*TODO: load values and display here*/}
              <div style={{height: '8rem'}}/>
            </div>
          </div>
        </div>
      </div>
      <FadeBox style={{marginTop: '1rem'}}>
        <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
          <div style={{...styles.selectBoxHeader, display: 'flex', position: 'relative'}}>
            <div>Preview Dataset</div>
            <div style={{marginLeft: '1rem', color: '#000000', fontSize: '14px'}}>A visualization
              of your data table based on the variable and value you selected above</div>
            <Button style={{position: 'absolute', right: '1rem', top: '.25rem'}}>
              SAVE DATASET
            </Button>
          </div>
          {/*TODO: Display dataset preview*/}
          <div style={{height: '8rem'}}/>
        </div>
      </FadeBox>
    </React.Fragment>;
  }

});

@Component({
  template: '<div #root></div>'
})
export class DataSetComponent extends ReactWrapperBase {
  constructor() {
    super(DataSet, []);
  }
}
