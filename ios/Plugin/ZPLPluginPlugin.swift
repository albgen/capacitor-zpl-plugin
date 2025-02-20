import Capacitor
import Zebra_SDK // Replace with actual SDK import

@objc(ZebraPrinterPlugin)
public class ZebraPrinterPlugin: CAPPlugin {
    @objc func getPrinters(_ call: CAPPluginCall) {
        // Example discovery using Zebra SDK (pseudo-code)
        var printers: [[String: String]] = []
        
        // Replace with actual Zebra SDK discovery method
        let discovery = ZebraPrinterDiscovery()
        discovery.discoverPrinters { (printer) in
            let printerInfo: [String: String] = [
                "id": printer.address,
                "name": printer.name ?? "Zebra Printer"
            ]
            printers.append(printerInfo)
        } onCompletion: {
            let result = ["printers": printers]
            call.resolve(result)
        } onError: { error in
            call.reject("Discovery failed: \(error.localizedDescription)")
        }
    }

    @objc func print(_ call: CAPPluginCall) {
        guard let zpl = call.getString("zpl"),
              let printerId = call.getString("printerId") else {
            call.reject("Missing zpl or printerId")
            return
        }

        DispatchQueue.global().async {
            do {
                let connection = ZebraBluetoothConnection(address: printerId)
                try connection.open()
                try connection.write(zpl.data(using: .utf8))
                connection.close()
                call.resolve()
            } catch {
                call.reject("Print failed: \(error.localizedDescription)")
            }
        }
    }
}