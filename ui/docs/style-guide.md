# UI Style Guide

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