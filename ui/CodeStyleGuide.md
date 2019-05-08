# Coding Style Guide

## React Component Structure

  <a name="react-component-structure--props-and-state"></a><a name="1.1"></a>
  - [1.1](#react-component-structure--props-and-state) Declare Props and State as interfaces

    ```typescript
    interface Props {
      name: string;
    }

    interface State {
      loading: boolean;
    }

    class MyReactComponent extends React.Component<Props, State> {
      constructor(props: Props) {
        super(props);
      }
    }
    ```
    - Props and State do not need to be prefixed with the class name as they are namespaced by the file.
    Ex. no need to declare the interfaces as MyReactComponentProps and MyReactComponentState
    - Props/State type can be assigned to function arguments to avoid `implicit any type` warning
    - Easier to read than inlining
    ```typescript
    class MyReactComponent extends React.Component<{
      name: string;
    }, {
      loading: boolean;
    }> {
       //
    }
    ```

  <a name="react-component-structure--export"></a><a name="1.2"></a>
  - [1.2](#react-component-structure--export) Exports

    ```javascript
    interface Props {
      name: string;
    }

    interface State {
      loading: boolean;
    }

    class MyReactComponent extends React.Component<Props, State> { }

    export {
      MyReactComponent,
      Props as MyReactComponentProps
    };
    ```
    - Declare all exports at the bottom of the file so there is one place to look at to determine what a file is exposing versus having to scan the file for export statements
    - Only export something when it must be referenced by something externally. Try to limit what gets exported.
    - Use aliases to keep variables namespaced appropriately to their scope. Ex. Props can just be Props in this file but can be prefixed with the class name when it is exposed publicly.
