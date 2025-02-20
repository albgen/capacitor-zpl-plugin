import { registerPlugin } from '@capacitor/core';

import type { ZPLPluginPlugin } from './definitions';

const ZPLPlugin = registerPlugin<ZPLPluginPlugin>('ZPLPlugin', {
  web: () => import('./web').then(m => new m.ZPLPluginWeb()),
});

export * from './definitions';
export { ZPLPlugin };


