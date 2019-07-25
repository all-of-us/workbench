import * as fp from 'lodash/fp';
import * as React from 'react';
import {withContentRect} from 'react-measure';

import {SpinnerOverlay} from 'app/components/spinners';
import {Scroll} from 'app/icons/scroll';
import {userMetricsApi} from 'app/services/swagger-fetch-clients';

import {ResourceCard} from 'app/components/resource-card';
import {ReactWrapperBase} from 'app/utils';
import {RecentResource} from 'generated/fetch';

export const RecentWork = (fp.flow as any)(
  withContentRect('client'),
)(class extends React.Component<{
  measureRef: React.Ref<any>,
  contentRect: {client: {width: number}},
  dark: boolean,
  cardMarginTop: string
}, {
  loading: boolean,
  offset: number,
  resources: RecentResource[],
  existingCohortName: string[],
  existingConceptName: string[],
  existingNotebookName: string[]
}> {
  public static defaultProps = {
    cardMarginTop: '1rem'
  };

  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      resources: [],
      offset: 0,
      existingCohortName: [],
      existingConceptName: [],
      existingNotebookName: []};
  }

  componentDidMount() {
    this.loadResources();
  }

  async loadResources() {
    try {
      this.setState({loading: true});
      const resources = await userMetricsApi().getUserRecentResources();
      this.setState({resources});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({loading: false});
    }
  }

  getExistingNameList(resource) {
    if (resource.notebook) {
      return this.state.existingNotebookName;
    } else if (resource.conceptSet) {
      return this.state.existingConceptName;
    } else if (resource.cohort) {
      return this.state.existingCohortName;
    }
    return [];
  }

  render() {
    const {contentRect, measureRef, cardMarginTop} = this.props;
    const {offset, resources, loading} = this.state;
    const limit = (contentRect.client.width - 24) / 224;
    return <div ref={measureRef} style={{display: 'flex', position: 'relative', minHeight: 247}}>
      <div style={{display: 'flex', position: 'relative', alignItems: 'center',
        marginLeft: '-1rem', paddingLeft: '1rem', opacity: loading ? 0.5 : 1}}>
        {resources.slice(offset, offset + limit).map((resource, i) => {
          return <ResourceCard key={i} marginTop={cardMarginTop}
            onDuplicateResource={(duplicating) => this.setState({loading: duplicating})}
            resourceCard={resource} onUpdate={() => this.loadResources()}
            existingNameList={this.getExistingNameList(resource)}
          />;
        })}
        {offset > 0 && <Scroll
          dir='left'
          onClick={() => this.setState({offset: offset - 1})}
          style={{position: 'absolute', left: 0, paddingBottom: '0.5rem'}}
        />}
        {offset + limit < resources.length && <Scroll
          dir='right'
          onClick={() => this.setState({offset: offset + 1})}
          style={{position: 'absolute', right: 0, paddingBottom: '0.5rem'}}
        />}
      </div>
      {loading && <SpinnerOverlay dark={this.props.dark} />}
    </div>;
  }
});
