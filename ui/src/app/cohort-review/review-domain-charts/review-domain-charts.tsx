import * as React from 'react';

export class ReviewDomainChartsComponent extends React.Component<{orgData: Array<any>}, {data: Array<any> }> {
  constructor(props) {
    super(props);
    this.state = {
      data: this.props.orgData,
    };
  }
  render() {
    // console.log(this.state.data);
    return (
      <div>
        hgjhghjghjghjjhgjjj
      </div>
    );
  }
}
export default ReviewDomainChartsComponent;
