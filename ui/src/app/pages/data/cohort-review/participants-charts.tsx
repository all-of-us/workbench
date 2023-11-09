import * as React from 'react';

import { CohortDefinition } from 'generated/fetch';

import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import {
  cohortBuilderApi,
  cohortReviewApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { WorkspaceData } from 'app/utils/workspace-data';

const css = `
  .graph-border {
    padding: 0.45rem;
  }
  @media print{
    .graph-border {
      padding: 3rem;
      page-break-inside:avoid;
    }
    .page-break {
      page-break-inside: avoid;
    }
  }
`;

const styles = reactStyles({
  dataBlue: {
    backgroundColor: colors.secondary,
    color: colors.white,
    height: '24px',
    fontSize: '10px',
    textAlign: 'end',
    paddingRight: '0.3rem',
    fontWeight: 'bold',
  },
  lightGrey: {
    backgroundColor: colorWithWhiteness(colors.dark, 0.7),
    display: '-webkit-box',
  },
  dataBarContainer: {
    flex: '0 0 55%',
    position: 'relative',
    width: '100%',
    minHeight: '1px',
    maxWidth: '55%',
    padding: '0.75rem 0 0 1.5rem',
    borderLeft: '1px solid black',
  },
  dataHeading: {
    flex: '0 0 30%',
    position: 'relative',
    minHeight: '1px',
    maxWidth: '30%',
    padding: '0.75rem 0.75rem 0',
    width: '24rem',
    fontSize: '10px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    textAlign: 'end',
  },
  dataPercent: {
    height: '24px',
    marginLeft: '0.75rem',
    paddingTop: '0.75rem',
    whiteSpace: 'nowrap',
    fontSize: '10px',
    fontWeight: 'bold',
    color: colors.primary,
  },
  count: {
    paddingLeft: '0.3rem',
    fontSize: '10px',
    fontWeight: 'bold',
    color: colors.primary,
  },
  containerMargin: {
    margin: 0,
    minWidth: '100%',
  },
  chartWidth: {
    maxWidth: '100%',
    margin: 0,
    padding: '1.5rem 0.75rem',
  },
  chartHeading: {
    textAlign: 'center',
    color: colors.primary,
    fontSize: '12px',
    fontWeight: 'bold',
    whiteSpace: 'nowrap',
  },
  domainTitle: {
    paddingBottom: '0.75rem',
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    textTransform: 'capitalize',
    lineHeight: '22px',
  },
});

export interface ParticipantsChartsProps {
  cohortReviewId?: number;
  domain: string;
  searchRequest?: CohortDefinition;
  workspace: WorkspaceData;
}

export interface ParticipantsChartsState {
  data: any;
  loading: boolean;
  options: any;
}

export const ParticipantsCharts = withCurrentWorkspace()(
  class extends React.Component<
    ParticipantsChartsProps,
    ParticipantsChartsState
  > {
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
      this.getChartData();
    }

    componentDidUpdate(prevProps: Readonly<ParticipantsChartsProps>) {
      const { domain } = this.props;
      if (domain && domain !== prevProps.domain) {
        this.setState({ loading: true });
        this.getChartData();
      }
    }

    async getChartData() {
      const {
        cohortReviewId,
        domain,
        searchRequest,
        workspace: { namespace, terraName },
      } = this.props;
      const chartResponse = !!cohortReviewId
        ? await cohortReviewApi().getCohortReviewChartData(
            namespace,
            terraName,
            cohortReviewId,
            domain
          )
        : await cohortBuilderApi().getCohortChartData(
            namespace,
            terraName,
            domain,
            searchRequest
          );
      const data = chartResponse.items.map((item) => {
        this.nameRefs.push(React.createRef());
        const percentCount = Math.round(
          (item.count / chartResponse.count) * 100
        );
        return { ...item, percentCount };
      });
      this.setState({ data, loading: false });
    }

    checkWidth = (i: number) => {
      const el = this.nameRefs[i].current;
      return el ? el.offsetWidth >= el.scrollWidth : true;
    };

    render() {
      const { cohortReviewId, domain } = this.props;
      const { data, loading } = this.state;
      const heading = domain.toLowerCase();
      return (
        <React.Fragment>
          <style>{css}</style>
          {loading ? (
            <SpinnerOverlay />
          ) : (
            <div className='page-break' style={styles.chartWidth}>
              <div style={styles.domainTitle}>Top 10 {heading}s</div>
              <div className='graph-border'>
                {!!data &&
                  data.map((item, i) => (
                    <div
                      key={i}
                      className='row'
                      style={{ display: '-webkit-box' }}
                    >
                      <TooltipTrigger
                        content={<div>{item.name}</div>}
                        disabled={this.checkWidth(i)}
                      >
                        <div style={styles.dataHeading} ref={this.nameRefs[i]}>
                          {item.name}
                        </div>
                      </TooltipTrigger>
                      <div style={styles.dataBarContainer}>
                        <div style={styles.lightGrey}>
                          <div
                            style={{
                              ...styles.dataBlue,
                              width: `${item.percentCount}%`,
                            }}
                          >
                            {item.percentCount >= 90 && (
                              <span>{item.count}</span>
                            )}
                          </div>
                          <div
                            style={{
                              ...styles.count,
                              width: `${item.percentCount}%`,
                            }}
                          >
                            {item.percentCount < 90 && (
                              <span>{item.count}</span>
                            )}
                          </div>
                        </div>
                      </div>
                      <div style={styles.dataPercent}>
                        {item.percentCount}% of Cohort{' '}
                        {!!cohortReviewId ? 'Review' : ''}
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}
        </React.Fragment>
      );
    }
  }
);
