# All of Us Workbench UI

The UI is a React app in TypeScript. 
A tutorial in React can be found [here](https://reactjs.org/tutorial/tutorial.html).

This app uses [CRA](https://create-react-app.dev/docs/getting-started/) / [react-app-rewired](https://www.npmjs.com/package/react-app-rewired) as a server and [react-router-dom](https://reactrouter.com/web/guides/quick-start) for routing.

All the interesting code is under `src/app/`.

A UI style guide can be found [here](https://github.com/all-of-us/workbench/wiki/UI-Style-Guide).

To debug Project Clarity, the widget framework, go here for examples:
https://github.com/vmware/clarity/tree/master/src/app

To run UI unit tests:

`yarn install && yarn test`

or to run tests headless:

`yarn install && yarn test --browsers=ChromeHeadless`

To regenerate TypeScript APIs after you've changed them in Swagger:

`yarn codegen`

To run the UI locally against the test API:

`REACT_APP_ENVIRONMENT=test yarn dev-up`

By default, this runs the server at: http://localhost:4200/.

To automatically fix lint errors:

`yarn lint --fix`

Other useful helper scripts are available. To get a list of all available scripts:

```
$ yarn run
...
   - deps
      ./src/deps/deps.sh
   - dev-up
       yarn && yarn run deps test && yarn start
   - dev-up-local
      yarn && yarn run deps local && REACT_APP_ENVIRONMENT=local yarn start
   - dev-up-tanagra-local
      ./src/tanagra-ui/dev-up-tanagra-local.sh
   - dev-up-test
      yarn && yarn run deps test && REACT_APP_ENVIRONMENT=localtest yarn start
...
```
