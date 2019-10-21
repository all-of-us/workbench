import * as fp from 'lodash/fp';
import * as React from 'react';
import {withContentRect} from 'react-measure';

import {SpinnerOverlay} from 'app/components/spinners';
import {Scroll} from 'app/icons/scroll';
import {userMetricsApi} from 'app/services/swagger-fetch-clients';

import {ResourceCard} from 'app/components/resource-card';
import {NotebookResourceCard} from 'app/pages/analysis/notebook-resource-card';
import {CohortResourceCard} from 'app/pages/data/cohort/cohort-resource-card';
import {RecentResource} from 'generated/fetch';

export const RecentResources = (fp.flow as any)(
  withContentRect('client'),
)(class extends React.Component<{
  measureRef: React.Ref<any>,
  contentRect: {client: {width: number}},
  dark: boolean
}, {
  loading: boolean,
  offset: number,
  resources: RecentResource[],
  existingCohortName: string[],
  existingConceptName: string[],
  existingNotebookName: string[]
}> {

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

  createResourceCard(resource: RecentResource) {
    if (resource.notebook) {
      return <NotebookResourceCard resource={resource}
                                   existingNameList={this.getExistingNameList(resource)}
                                   onUpdate={() => this.loadResources()}/>;
    } else if (resource.cohort) {
      return <CohortResourceCard resource={resource}
                                 existingNameList={this.getExistingNameList(resource)}
                                 onUpdate={() => this.loadResources()}/>;
    } else {
      return <ResourceCard resourceCard={resource}
                    onDuplicateResource={(duplicating) => this.setState({loading: duplicating})}
                    onUpdate={() => this.loadResources()}
                    existingNameList={this.getExistingNameList(resource)}
      />;
    }
  }

  render() {
    const {contentRect, measureRef} = this.props;
    const {offset, resources, loading} = this.state;
    const limit = (contentRect.client.width - 24) / 224;
    return <div ref={measureRef} style={{display: 'flex', position: 'relative', minHeight: 247}}>
      <div style={{display: 'flex', position: 'relative', alignItems: 'center',
        marginTop: '-1rem', marginLeft: '-1rem', paddingLeft: '1rem', opacity: loading ? 0.5 : 1}}>
        {resources.slice(offset, offset + limit).map((resource, i) => {
          return <div key={i}> {this.createResourceCard(resource)} </div>;
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
