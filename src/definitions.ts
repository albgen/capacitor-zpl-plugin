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
  print(options: { zpl: string; printerId: string }): Promise<void>;
}

export interface Printer {
  id: string;    // Unique identifier (e.g., Bluetooth address)
  name: string;  // Friendly name of the printer
}