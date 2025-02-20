import { WebPlugin } from '@capacitor/core';

import type { ZPLPluginPlugin } from './definitions';

export class ZPLPluginWeb extends WebPlugin implements ZPLPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
