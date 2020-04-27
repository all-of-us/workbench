# UI Coding Style Guide

## Starting new work

When starting a UI card, always check Invision for latest designs.
	https://projects.invisionapp.com/d/main#/projects/prototypes/12820961
	
We are working towards abstracting styles (and possibly components) where it makes sense. When starting a new UI ticket, look around. 
* Are there other places where elements similar to ones you are using are used? 
* If so, have we already abstracted out the style and/or component?
* If not, should you abstract out the style/component as part of your card?


## React Component Structure

  - Declare Props and State as interfaces

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

  - Exports

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


## Links:
When in doubt, prefer the use of `<StyledAnchorTag>`, a custom component with styling attached,
as well as a custom handler for hrefs that handles same-page navigation within the site. This should
*not* be used for click handling that does not change the URL of the page, or open a new URL. For
generic on-click handling, we should use one of the button classes, to avoid semantic confusion
between the usages.

To use disabled links, or links with logic, we should still try to make use of the `<StyledAnchorTag>`.
Although disabling buttons is easier, it is valuable to maintain the accessibility benefits and keyboard
shortcut benefits of anchor tags for page navigation.

TODO: We need to build disabled functionality into that wrapper.

## Async Code:
Where possible, we prefer the use of async functions with the keyword `await` within them. If a function
wants to make promises in parallel, prefer abstracting to other functions. If there is logic
that needs to happen after all promises have resolved, use the `await all` function.
