// For DT-32 demonstration plots
// original file from dolbeew/highcharts-demo -- src/app/components/highcharts-new-gallery.tsx
// 1. create new route - routing - workspace-app-routing.tsx
import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { ChartData } from 'generated/fetch';

import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

import { parseQueryParams } from './app-router';
import { Chart } from './highcharts-new-chart';
import { WithSpinnerOverlayProps } from './with-spinner-overlay';

const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer',
  },
  container: {
    width: '100%',
    marginLeft: 'auto',
    marginRight: 'auto',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem',
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  reportBackground: {
    backgroundColor: colors.white,
    paddingTop: '1rem',
    marginTop: '0.5rem',
  },
  chartTitle: {
    marginLeft: '0.4rem',
    paddingBottom: '0.5rem',
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '22px',
  },
});
export interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

interface State {
  newChartData: ChartData[];
  options: any;
  chartPopPyramid: any;
  chartsGenderRaceByAgeMap: {};
}

export const NewChart = fp.flow(withRouter)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        options: null,
        newChartData: null,
        chartPopPyramid: null,
        chartsGenderRaceByAgeMap: {},
      };
    }

    async componentDidMount(): Promise<void> {
      const { hideSpinner } = this.props;
      hideSpinner();
    }

    // data.sort(function (a, b) {
    //   return a.city.localeCompare(b.city) || b.price - a.price;
    // });

    render() {
      const cohortId = parseQueryParams(this.props.location.search).get(
        'cohortId'
      );
      const domain = parseQueryParams(this.props.location.search).get('domain');
      return (
        <React.Fragment>
          <div style={styles.row}>
            <div
              style={{
                ...styles.col,
                flex: '0 0 100%',
                maxWidth: '100%',
              }}
            >
              {
                <div>
                  <Chart domain={domain} cohortId={cohortId} />
                </div>
              }
            </div>
          </div>
        </React.Fragment>
      );
    }
  }
);
