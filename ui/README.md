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
