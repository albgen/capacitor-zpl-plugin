// src/definitions.ts
export interface ZebraPrinterPlugin {
  /**
   * Discovers available Zebra printers.
   * @returns A list of printers with their IDs and names.
   */
  getPrinters(): Promise<{ printers: Printer[] }>;

  /**
   * Prints ZPL code to a specified printer.
   * @param options Contains the ZPL code and printer ID.
   */
  print(options: { type: string; id: string; zpl:string; address?: string; port?: string; action?: string; text: string, mmFeedPaper?:String, useEscPosAsterik?: boolean, initializeBeforeSend?: boolean, sendDelay?: string; chunkSize?: string}): Promise<void>;

  bluetoothHasPermissions(): Promise<{result: boolean;}>;
  bluetoothIsEnabled(): Promise<{result: boolean;}>;
  listPrinters(options: {type: string;}): Promise<Printers>;
  printFormattedText(options: { type: string; id: string; address?: string; port?: string; action?: string; text: string, mmFeedPaper?:String, useEscPosAsterik?: boolean, initializeBeforeSend?: boolean, sendDelay?: string; chunkSize?: string}): Promise<void>;
  logCat(options: { message: string; }): Promise<void>;
  rejectTest(): Promise<void>;
  throwException(): Promise<void>;
  echo(options: {value: string;}): Promise<{value: string;}>;
}

export interface Printer {
  id: string;    // Unique identifier (e.g., Bluetooth address)
  name: string;  // Friendly name of the printer
  zpl: string;
  address: string;
  bondState: string;
  type: string;
  //features: string;
  deviceClass: string;
  majorDeviceClass: string;
  //[uuid: string]: string; // For dynamic UUID keys
}

export interface Printers
{
  [key: string]: Printer;  // Dynamic key, where key is the printer name found also on name property of the PrinterInfo
}
