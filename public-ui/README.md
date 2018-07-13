# All of Us Workbench Public UI -- aka Databrowser

The Public UI is an Angular app in TypeScript. For an intro to Angular, see
[the tutorial](https://angular.io/docs/ts/latest/tutorial/).

All the interesting code is under `src/app/`.

`*.component.ts` are UI components, controlling various widgets; they have
associated `*.component.html` and/or `*.component.css` for their templated DOM
and style. Angular scopes CSS to its associated component.

`*.service.ts` provide data. Typically, these wrap REST APIs.

`*.module.ts` set up glue to stick the app together: routing of URLs to
different views, installation of the various components/services for use in
other places.

`src/generated/*` and `src/publicGenerated/*` code are generated services by SWAGGER codegen for using the api and 
public api. These get rebuilt everytime you run dev-up. If you change an api, you need to run dev-up in the ui again to regenerate.

To debug Project Clarity, the widget framework, go here for examples:
https://github.com/vmware/clarity/tree/master/src/app

To run UI unit tests:

`yarn install && yarn test`

or to run tests headless:

`yarn install && yarn test --browsers=ChromeHeadless`
