// import * as domtoimage from 'dom-to-image';


// declare const domtoimage: any;

/* SystemJS module definition */
declare var module: NodeModule;
interface NodeModule {
  id: string;
}

/*
Declare TypeScript types for the plain-javascript third-party scripts.
This gets included by the compiler; its symbols do not need to be imported by
other source files but are globally available.
*/

// interface DomToImageNs {
//   toPng: (element: any) => any;
//   toBlob: (image: any) => any;
// }
//
// declare var domtoimage: DomToImageNs;

declare module "dom-to-image";
