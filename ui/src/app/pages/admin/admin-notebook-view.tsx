import * as React from 'react';
import {useParams} from 'react-router';

interface Props {
  workspaceNamespace: string;
  nbName: string;
}

const AdminNotebookViewComponent = (props: Props) => {
  const {workspaceNamespace, nbName} = props;
  return <div>HELLO WORLD: {workspaceNamespace}/{nbName}</div>;
};

// attempt 1

// export const AdminNotebookView = () => {
//     const {workspaceNamespace, nbName} = useParams();
//     return <AdminNotebookViewComponent workspaceNamespace={workspaceNamespace} nbName={nbName}/>;
// };

// attempt 2
export class AdminNotebookView extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props);
  }

  render() {
    const {workspaceNamespace, nbName} = useParams();
    return <AdminNotebookViewComponent workspaceNamespace={workspaceNamespace} nbName={nbName}/>;
  }
}
