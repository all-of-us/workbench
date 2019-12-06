import * as React from "react";
import colors from "app/styles/colors";
import {signInStore} from "../utils/navigation";
import {getConfiguration} from "../app.module";

declare const gapi: any;

const styles = {
  alertBanner: {
    backgroundColor: colors.warning,
    color: colors.white,
    marginLeft: '1rem',
    marginRight: '1rem',
    marginBottom: '0.5rem',
    flex: '1 0 20%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  }
};

export interface State {
  message: string;
}

export class StatusAlert extends React.Component<{}, State> {
  constructor(props) {
    super(props);
    this.state = {
      message: ''
    }
  }

  componentDidMount() {
    gapi.load("client", () => {
      gapi.client.load('storage', 'v1', () => {
        gapi.client.storage.objects.get({
          bucket: "all-of-us-workbench-test-alerts",
          object: "alert.json",
          alt: "media"
        })
        .then((response) => {
          console.log(response.body);
        })
      });
    });
  }

  getStyles(message) {
    if (!!message) {
      return {...styles.alertBanner, height: '80%'}
    }
    else {
      return {...styles.alertBanner}
    }
  }

  render() {
    const {message} = this.state;
    return <div style={this.getStyles(message)}>
        <div>{message}</div>
    </div>
  }
}