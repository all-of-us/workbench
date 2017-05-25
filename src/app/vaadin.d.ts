// Declare TypeScript types for the plain-javascript VAADIN third-party script.
// This gets included by the compiler; its symbols do not need to me imported by
// other source files but are globally available.

interface VaadinNs {
  initApplication: (a: string, b: any) => any;
}

declare var vaadin: VaadinNs;
