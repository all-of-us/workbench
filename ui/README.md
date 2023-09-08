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

## Fontawesome

We use some [Fontawesome](https://fontawesome.com/) Pro icons. Fontawesome requires a license during a fresh `yarn install`. Delete `node_modules` and `yarn.lock` before running `yarn install` to test. If a license isn't found, `yarn install` will fail with the error message:
```
error An unexpected error occurred: "https://npm.fontawesome.com/@fortawesome%2ffontawesome-free: authentication required".
```
To obtain the license:
```
gsutil cat gs://all-of-us-workbench-test-credentials/dot-npmrc-fontawesome-creds-line.txt
  >> ~/.npmrc
```
