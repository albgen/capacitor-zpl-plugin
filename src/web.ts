import { WebPlugin } from '@capacitor/core';

import type { ZebraPrinterPlugin, Printer, Printers } from './definitions';

export class ZebraPrinterWeb extends WebPlugin implements ZebraPrinterPlugin {
  async getPrinters(): Promise<{ printers: Printer[] }> {
    throw new Error('Printer discovery not supported on the web');
  }

  async print(options: { zpl: string; printerId: string }): Promise<void> {
    console.log('ZPL Code (web mock):', options.zpl);
    console.log('Printer ID:', options.printerId);
  }

  async rejectTest(): Promise<void> {
    console.log('rejectTest not implemented on WEB');
    return Promise.resolve();
  }
  async throwException(): Promise<void> {
    console.log('throwException not implemented on WEB');
    return Promise.resolve();
  }
  async printFormattedText(options: { type: string; id: string; address?: string; port?: string; action?: string; text: string; mmFeedPaper?: String; useEscPosAsterik?: boolean, initializeBeforeSend?: boolean , sendDelay?: string; chunkSize?: string }): Promise<void> {
    console.log('printFormattedText not implemented on WEB' + options);
    return Promise.resolve();
  }

  async logCat(options: { message: string; }): Promise<void> {
    console.log('logCat not implemented on WEB', options);
    return Promise.resolve();
  }

  async listPrinters(options: { type: string; }): Promise<Printers> {
    console.log('ListPrinters not implemented on WEB', options);
    return Promise.resolve({});
  }

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async bluetoothHasPermissions(): Promise<{ result: boolean }> {
    console.log('BluetoothHasPermissions not implemented on WEB');
    return { result: true};
  }

  async bluetoothIsEnabled(): Promise<{ result: boolean }> {
    console.log('BluetoothIsEnabled not implemented on WEB');
    return { result: true};
  }
}