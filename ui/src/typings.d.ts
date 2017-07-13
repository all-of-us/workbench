/* SystemJS module definition */
declare var module: NodeModule;
interface NodeModule {
  id: string;
}

/*
Declare TypeScript types for the plain-javascript VAADIN third-party script.
This gets included by the compiler; its symbols do not need to be imported by
other source files but are globally available.
*/

interface VaadinNs {
  initApplication: (a: string, b: any) => any;
}

// This var name matches the third-party symbol name.
declare var vaadin: VaadinNs;
