/* SystemJS module definition */
declare let module: NodeModule;
interface NodeModule {
  id: string;
}

/*
Declare TypeScript types for the plain-javascript third-party scripts.
This gets included by the compiler; its symbols do not need to be imported by
other source files but are globally available.
*/

declare let ResizeObserver: any;

declare module 'outdated-browser-rework';
