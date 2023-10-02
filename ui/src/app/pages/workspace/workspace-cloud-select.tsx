import { reactStyles } from 'app/utils';
import React, { Component } from 'react';

export const styles = reactStyles({
    radioCardContainer: {
        display: 'flex',
        flexDirection: 'row',
        padding: '0.9rem 0',
        width: '95%',
        justifyContent: 'flex-start',
      },
      
    radioCard: {
        border: '2px solid #ccc', /* Add a border to the radio cards */
        padding: '10px', /* Add padding to create space around the radio button and label */
        width: '50%', /* Set the width of each radio card */
        textAlign: 'center', /* Center-align text within the radio card */
        cursor: 'pointer' /* Change cursor to pointer on hover */
    },
    
    radioCardActive: {
        borderColor: '#007bff', 
        backgroundColor: '#f0f0f0', 
    },
});

interface RadioCardState {
  selectedOption: string;
}

class RadioCard extends Component<{}, RadioCardState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      selectedOption: 'GCP', // Default selected option
    };
  }

  handleOptionChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({
      selectedOption: e.target.value,
    });
  };

  render() {
    const { selectedOption } = this.state;

    return (
      <div style={styles.radioCardContainer}>
        <div style={styles.radioCard} className={`radio-card ${selectedOption === 'GCP' ? 'active' : ''}`}>
          <input
            type="radio"
            value="GCP"
            checked={selectedOption === 'GCP'}
            onChange={this.handleOptionChange}
          />          
          <label className="radio-label">GCP</label>

        </div>
        <div style={styles.radioCard}  className={`radio-card ${selectedOption === 'AWS' ? 'active' : ''}`}>
          <input
            type="radio"
            value="AWS"
            checked={selectedOption === 'AWS'}
            onChange={this.handleOptionChange}
          />
          <label className="radio-label">AWS</label>

        </div>
      </div>
    );
  }
}

export default RadioCard;