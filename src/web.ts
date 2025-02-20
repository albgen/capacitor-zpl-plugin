import { WebPlugin } from '@capacitor/core';

import type { ZebraPrinterPlugin, Printer } from './definitions';

export class ZebraPrinterWeb extends WebPlugin implements ZebraPrinterPlugin {
  async getPrinters(): Promise<{ printers: Printer[] }> {
    throw new Error('Printer discovery not supported on the web');
  }

  async print(options: { zpl: string; printerId: string }): Promise<void> {
    console.log('ZPL Code (web mock):', options.zpl);
    console.log('Printer ID:', options.printerId);
  }
}