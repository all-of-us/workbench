import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore} from 'app/utils/navigation';
import * as React from 'react';

const css = `
  .graph-border {
    padding: 0.3rem;
  }
  @media print{
    .graph-border {
      padding: 2rem;
      page-break-inside:avoid;
    }
    .page-break {
      page-break-inside: avoid;
    }
  }
`;

const styles = reactStyles({
  dataBlue: {
    backgroundColor: '#216FB4',
    color: 'white',
    height: '24px',
    fontSize: '10px',
    textAlign: 'end',
    paddingRight: '0.2rem',
    fontWeight: 'bold'
  },
  lightGrey: {
    backgroundColor: '#CCCCCC',
    display: '-webkit-box',
  },
  dataBarContainer: {
    flex: '0 0 58.33333%',
    position: 'relative',
    width: '100%',
    minHeight: '1px',
    maxWidth: '58.33333%',
    padding: '0.5rem 0 0 1rem',
    borderLeft: '1px solid black'
  },
  dataHeading: {
    flex: '0 0 33.33333%',
    position: 'relative',
    minHeight: '1px',
    maxWidth: '33.33333%',
    padding: '0.5rem 0.5rem 0',
    width: '16rem',
    fontSize: '10px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    textAlign: 'end',
  },
  dataPercent: {
    height: '24px',
    marginLeft: '0.5rem',
    paddingTop: '0.5rem',
    whiteSpace: 'nowrap',
    fontSize: '10px',
    fontWeight: 'bold',
    color: '#4a4a4a',
  },
  count: {
    paddingLeft: '0.2rem',
    fontSize: '10px',
    fontWeight: 'bold',
    color: '#4a4a4a',
  },
  containerMargin: {
    margin: 0,
    minWidth: '100%',
  },
  chartWidth: {
    maxWidth: '100%',
    margin: 0,
    padding: '1rem 0.5rem',
  },
  chartHeading: {
    textAlign: 'center',
    color: '#4A4A4A',
    fontSize: '12px',
    fontWeight: 'bold',
    whiteSpace: 'nowrap',
  },
  domainTitle: {
    paddingBottom: '0.5rem',
    fontSize: '16px',
    fontWeight: 600,
    color: '#262262',
    textTransform: 'capitalize',
    lineHeight: '22px',
  }
});

export interface ParticipantsChartsProps {
  domain: string;
  workspace: WorkspaceData;
}

export interface ParticipantsChartsState {
  data: any;
  loading: boolean;
  options: any;
}

export const ParticipantsCharts = withCurrentWorkspace()(
  class extends React.Component<ParticipantsChartsProps, ParticipantsChartsState>  {
    nameRefs: Array<any> = [];
    constructor(props: ParticipantsChartsProps) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        options: null,
      };
    }

    componentDidMount() {
      const {domain, workspace: {cdrVersionId, id, namespace}} = this.props;
      const cohort = currentCohortStore.getValue();
      cohortReviewApi().getCohortChartData(namespace, id, cohort.id, +cdrVersionId, domain, 10)
        .then(resp => {
          const data = resp.items.map(item => {
            this.nameRefs.push(React.createRef());
            const percentCount = Math.round((item.count / resp.count) * 100);
            return {...item, percentCount};
          });
          this.setState({data, loading: false});
        });
    }

    checkWidth = (i: number) => {
      const el = this.nameRefs[i].current;
      return el ? el.offsetWidth >= el.scrollWidth : true;
    }

    render() {
      const {domain} = this.props;
      const {data, loading} = this.state;
      const heading = domain.toLowerCase();
      return <React.Fragment>
        <style>{css}</style>
        {data && <div className='page-break' style={styles.chartWidth}>
          <div style={styles.domainTitle}>Top 10 {heading}s</div>
          <div className='graph-border'>
            {data.map((item, i) => (
              <div key={i} className='row' style={{display: '-webkit-box'}}>
                <TooltipTrigger content={<div>{item.name}</div>} disabled={this.checkWidth(i)}>
                  <div style={styles.dataHeading} ref={this.nameRefs[i]}>
                    {item.name}
                  </div>
                </TooltipTrigger>
                <div style={styles.dataBarContainer}>
                  <div style={styles.lightGrey}>
                    <div style={{...styles.dataBlue, width: `${item.percentCount}%`}}>
                      {item.percentCount >= 90 && <span>{item.count}</span>}
                    </div>
                    <div style={{...styles.count, width: `${item.percentCount}%`}}>
                      {item.percentCount < 90 && <span>{item.count}</span>}
                    </div>
                  </div>
                </div>
                <div style={styles.dataPercent}>
                  {item.percentCount}% of Cohort
                </div>
              </div>
            ))}
          </div>
        </div>}
        {loading && <SpinnerOverlay />}
      </React.Fragment>;
    }
  }
);
