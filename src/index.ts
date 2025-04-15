// src/index.ts
import { registerPlugin } from '@capacitor/core';

import type { ZebraPrinterPlugin } from './definitions';

const ZebraPrinter = registerPlugin<ZebraPrinterPlugin>('ZebraPrinter', {
  web: () => import('./web.js').then(m => new m.ZebraPrinterWeb()),
});

export * from './definitions.js';
export { ZebraPrinter };