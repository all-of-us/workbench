import {Component, DoCheck, Injector, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {createCustomElement} from '@angular/elements';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {SignInService} from '../../services/sign-in.service';
import {InvitationKeyComponent} from '../invitation-key/component';


class LoginReact extends React.Component<any, any> {
  backgroundImgSrc = '/assets/images/login-group.png';
  smallerBackgroundImgSrc = '/assets/images/login-standing.png';
  googleIcon = '/assets/icons/google-icon.png';
  showCreateAccount = true;
  container = {};
  props = {injector: Injector};
  constructor(props: object) {
    super(props);
    this.container = React.createRef();
  }

  componentDidMount() {
  //  angular.bootstrap(this.container, ['app-invitation-key']);
  }
  render() {
    return  <>
      <div className='sign-in-container'>
          {!this.showCreateAccount &&
          <div>
          <div>
                <h3>Already have an account?</h3>
                <div style={{display: 'flex'}}>
                  <button className='btn btn-lg btn-primary'>
                  <img className='google-icon' src={this.googleIcon}/>
                  <div>Sign In with Google</div>
                  </button>
                </div>
              </div>
              <div>
              <h4>Don't have an account?</h4>
            <button className='btn btn-secondary'>Create Account</button>
                  x</div>
          </div>
          }
        {this.showCreateAccount &&
        <div>jhgg
          <InvitationReact></InvitationReact>
        </div>}

    </div>
    </>;
  }
}

@Component({
  selector: 'app-signed-out',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})

export class LoginComponent implements DoCheck, OnInit {
  showCreateAccount = false;
  backgroundImgSrc = '/assets/images/login-group.png';
  smallerBackgroundImgSrc = '/assets/images/login-standing.png';
  googleIcon = '/assets/icons/google-icon.png';
  constructor(
      /* Ours */
      private signInService: SignInService,
      /* Angular's */
      private router: Router,
  ) {}

  ngOnInit(): void {
    document.body.style.backgroundColor = '#e2e3e5';

    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        this.router.navigateByUrl('/');
      }
    });
    ReactDOM.render(React.createElement(LoginReact),
        document.getElementById('quick-tour'));

  }

  ngDoCheck(): void {
    ReactDOM.render(React.createElement(LoginReact),
        document.getElementById('quick-tour'));
  }
}
