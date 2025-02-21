package com.zpl.capacitor;

import android.Manifest;
import android.util.Log;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zpl.helper.DeviceConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CapacitorPlugin(
        name = "ZebraPrinter",
        permissions = {
                @Permission(
                        strings = {
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_ADVERTISE
                        }, alias = "BT"
                )
        }
)
public class ZebraPrinterPlugin extends Plugin {

    private static final Integer REQUEST_ENABLE_BT = 1;

    // Reference to the Bridge
    protected Bridge bridge;

    // The same as defined in alias at @Permission
    private static final String BT_ALIAS = "BT";
    private static final String[] aliasesPermissions = new String[]{BT_ALIAS};
    private final HashMap<String, DeviceConnection> connections = new HashMap<>();

    @Override
    public void load() {
        Log.i("ZebraPrinterPlugin", "loading the plugin...");
    }

    @PluginMethod
    public void getPrinters(PluginCall call) {
        final PluginCall savedCall = call;
        final List<JSObject> printerList = new ArrayList<>();

        DiscoveryHandler discoveryHandler = new DiscoveryHandler() {
            @Override
            public void foundPrinter(DiscoveredPrinter printer) {
                JSObject printerObj = new JSObject();
                printerObj.put("id", printer.getDiscoveryDataMap().get("ADDRESS"));
                printerObj.put("name", printer.getDiscoveryDataMap().get("FRIENDLY_NAME"));
                printerList.add(printerObj);
            }

            @Override
            public void discoveryFinished() {
                JSObject result = new JSObject();
                result.put("printers", new JSArray(printerList));
                savedCall.resolve(result);
            }

            @Override
            public void discoveryError(String message) {
                savedCall.reject("Discovery failed: " + message);
            }
        };

        try {
            BluetoothDiscoverer.findPrinters(getContext(), discoveryHandler);
        } catch (Exception e) {
            call.reject("Discovery failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void print(PluginCall call) {
        String zpl = call.getString("zpl");
        String printerId = call.getString("printerId");

        if (zpl == null || printerId == null) {
            call.reject("Missing zpl or printerId");
            return;
        }

        new Thread(() -> {
            try {
                Connection connection = new BluetoothConnection(printerId);
                connection.open();
                connection.write(zpl.getBytes());
                connection.close();
                call.resolve();
            } catch (Exception e) {
                call.reject("Print failed: " + e.getMessage());
            }
        }).start();
    }
}