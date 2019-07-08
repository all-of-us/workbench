import * as React from "react";
import {ResourceCardTemplate} from "./resource-card-template";

interface Props {}
interface State {}

export class NotebookResourceCard extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
  }

  render() {
    return <ResourceCardTemplate
      actionsDisabled={true}
      disabled={true}
      resourceUrl={'test'}
      displayName={'a'}
      description={'b'}
      displayDate={'c'}
      footerText={'e'}
      footerColor={'#000000'}
    />
  }
}