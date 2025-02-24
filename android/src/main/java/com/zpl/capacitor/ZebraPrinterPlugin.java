package com.zpl.capacitor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.os.Looper;
import android.util.Log;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import com.getcapacitor.annotation.PermissionCallback;
import com.zebra.sdk.btleComm.BluetoothLeConnection;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;
import com.zpl.helper.DeviceConnection;
import com.zpl.helper.bluetooth.BluetoothConnections;
import com.zpl.helper.bluetooth.BluetoothPrintersConnections;
import com.zpl.helper.usb.UsbConnection;
import com.zpl.helper.usb.UsbConnections;
import com.zebra.sdk.printer.discovery.*;
import com.zebra.sdk.comm.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
    public static final String ZPL_PLUGIN = "ZPLPlugin";

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
    public void logCat(PluginCall call)  {
        try
        {
            String str = call.getString("message");
            if (str != null)
                Log.i(ZPL_PLUGIN, str);
        } catch (Exception e) {
            call.reject(e.getMessage(),"COD05");
        }
    }

    @PluginMethod
    public void bluetoothHasPermissions(PluginCall call) {
        try
        {
            JSObject ret = new JSObject();
            ret.put("result", bluetoothHasPermissions());
            call.resolve(ret);
        } catch (Exception ex) {
            call.reject(ex.getMessage(),"COD06");
        }
    }

    @PermissionCallback
    private void BTPermsCallback(PluginCall call) {
        try
        {
            if (getPermissionState(BT_ALIAS) == PermissionState.GRANTED) {
                Log.i(ZPL_PLUGIN, "PermissionState.GRANTED already");
            } else {
                if (getPermissionState(BT_ALIAS) == PermissionState.DENIED) {
                    //Log.i("ZPLPlugin", "Permission is required for bluetooth");
                    call.reject("You have denied the permission. Go to app settings and give the permission manually. In alternative you can clear the data and the system will ask you again.");
                }
            }
        }
        catch(Exception ex)
        {
            call.reject(ex.getMessage(),"COD04");
        }
    }

    @PluginMethod
    public void bluetoothIsEnabled(PluginCall call) throws Exception {
        try
        {
            JSObject ret = new JSObject();
            ret.put("result", bluetoothIsEnabled());
            call.resolve(ret);
        } catch(Exception ex)
        {
            call.reject(ex.getMessage(),"COD03");
        }
    }

    @PluginMethod
    public void listPrinters(PluginCall call) throws Exception{
        try {
            JSObject printers = new JSObject();
            String type = call.getString("type");
            if (type.equals("bluetooth")) {
                if (!bluetoothIsEnabled()) {
                    throw new JSONException("Bluetooth not enabled");
                }
                if (!bluetoothHasPermissions()) {
                    askForBTPermissionIfNotHaveAlready(call);
                    throw new JSONException("Missing permission for bluetooth");
                }
                try {
                    BluetoothConnections printerConnections = new BluetoothConnections();
                    for (com.zpl.helper.bluetooth.BluetoothConnection bluetoothConnection : printerConnections.getList()) {
                        BluetoothDevice bluetoothDevice = bluetoothConnection.getDevice();
                        JSONObject printerObj = new JSONObject();
                        try {
                            printerObj.put("address", bluetoothDevice.getAddress());
                        } catch (Exception ignored) {
                        }  // String
                        try {
                            printerObj.put("bondState", String.valueOf(bluetoothDevice.getBondState()));
                        } catch (SecurityException ignored) {
                        } // Ensure bondState is a string
                        try {
                            printerObj.put("name", bluetoothDevice.getName());
                        } catch (SecurityException ignored) {
                        }  // String
                        try {
                            printerObj.put("type", String.valueOf(bluetoothDevice.getType()));
                        } catch (SecurityException ignored) {
                        } // Convert type to string
                        //try { printerObj.put("features", String.valueOf(bluetoothDevice.getUuids())); } catch (SecurityException ignored) {}  // Convert type to string
                        try {
                            printerObj.put("deviceClass", String.valueOf(bluetoothDevice.getBluetoothClass().getDeviceClass()));
                        } catch (SecurityException ignored) {
                        }
                        try {
                            printerObj.put("majorDeviceClass", String.valueOf(bluetoothDevice.getBluetoothClass().getMajorDeviceClass()));
                        } catch (SecurityException ignored) {
                        }  // Convert type to string
                        try {
                            printers.put(bluetoothDevice.getName(), printerObj);
                        } catch (SecurityException ignored) {
                        }
                    }
                } catch (Exception e) {
                    printers.put("error", e.getMessage());
                    call.resolve(printers);
                    return;
                }
            } else {
                UsbConnections printerConnections = new UsbConnections(getContext());
                for (UsbConnection usbConnection : printerConnections.getList()) {
                    UsbDevice usbDevice = usbConnection.getDevice();
                    JSONObject printerObj = new JSONObject();
                    try {
                        printerObj.put("productName", Objects.requireNonNull(usbDevice.getProductName()).trim());
                    } catch (Exception ignored) {
                    }
                    try {
                        printerObj.put("manufacturerName", usbDevice.getManufacturerName());
                    } catch (Exception ignored) {
                    }
                    try {
                        printerObj.put("deviceId", usbDevice.getDeviceId());
                    } catch (Exception ignored) {
                    }
                    try {
                        printerObj.put("serialNumber", usbDevice.getSerialNumber());
                    } catch (Exception ignored) {
                    }
                    try {
                        printerObj.put("vendorId", usbDevice.getVendorId());
                    } catch (Exception ignored) {
                    }
                    printers.put(usbDevice.getDeviceName(), printerObj);
                }
            }
            Log.i("Printer Object", printers.toString());
            call.resolve(printers);
        }
        catch(Exception ex)
        {
            call.reject(ex.getMessage(),"COD02");
        }
    }

    @PluginMethod
    public void getPrinters(PluginCall call) throws Exception {

        if (!bluetoothIsEnabled()) {
            throw new JSONException("Bluetooth not enabled");
        }
        if (!bluetoothHasPermissions()) {
            askForBTPermissionIfNotHaveAlready(call);
            throw new JSONException("Missing permission for bluetooth");
        }

        final PluginCall savedCall = call;
        final List<JSObject> printerList = new ArrayList<>();

        DiscoveryHandler discoveryHandlerBluetooth = new DiscoveryHandler() {
            @Override
            public void foundPrinter(DiscoveredPrinter printer) {
                JSObject printerObj = new JSObject();
                printerObj.put("id", printer.getDiscoveryDataMap().get("ADDRESS"));
                printerObj.put("name", printer.getDiscoveryDataMap().get("FRIENDLY_NAME"));
                printerObj.put("type", "bluetooth");
                printerList.add(printerObj);
            }

            @Override
            public void discoveryFinished() {
                JSObject result = new JSObject();
                result.put("printers", new JSArray(printerList));
                Log.i(ZPL_PLUGIN,"printerListBT:" + printerList.toString());
                savedCall.resolve(result);
            }

            @Override
            public void discoveryError(String message) {
                savedCall.reject("Discovery bluetooth failed: " + message);
            }
        };

        DiscoveryHandler discoveryHandlerNetwork = new DiscoveryHandler() {
            @Override
            public void foundPrinter(DiscoveredPrinter printer) {
                JSObject printerObj = new JSObject();
                printerObj.put("id", printer.getDiscoveryDataMap().get("ADDRESS"));
                printerObj.put("name", printer.getDiscoveryDataMap().get("FRIENDLY_NAME"));
                printerObj.put("type", "tcp");
                printerList.add(printerObj);
            }

            @Override
            public void discoveryFinished() {
                JSObject result = new JSObject();
                result.put("printers", new JSArray(printerList));
                Log.i(ZPL_PLUGIN,"printerListTCP:" + printerList.toString());
                savedCall.resolve(result);
            }

            @Override
            public void discoveryError(String message) {
                savedCall.reject("Discovery network failed: " + message);
            }
        };

        DiscoveryHandler discoveryHandlerUSB = new DiscoveryHandler() {

            @Override
            public void foundPrinter(DiscoveredPrinter printer) {
                JSObject printerObj = new JSObject();
                printerObj.put("id", printer.getDiscoveryDataMap().get("ADDRESS"));
                printerObj.put("name", printer.getDiscoveryDataMap().get("FRIENDLY_NAME"));
                printerObj.put("type", "usb");
                printerList.add(printerObj);
            }

            @Override
            public void discoveryFinished() {
                JSObject result = new JSObject();
                result.put("printers", new JSArray(printerList));
                Log.i(ZPL_PLUGIN,"printerListUSB:" + printerList.toString());
                savedCall.resolve(result);
            }

            @Override
            public void discoveryError(String message) {
                savedCall.reject("Discovery usb failed: " + message);
            }
        };

        try {
            BluetoothDiscoverer.findPrinters(getContext(), discoveryHandlerBluetooth);
            NetworkDiscoverer.findPrinters(discoveryHandlerNetwork);
            UsbDiscoverer.findPrinters(this.getContext().getApplicationContext(), discoveryHandlerUSB);

        } catch (Exception e) {
            call.reject("Discoveries failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void print(PluginCall call) throws Exception {

        JSONObject data = new JSObject();
        data.put("action", call.getString("action"));                   //i.e: cut
        data.put("mmFeedPaper", call.getString("mmFeedPaper"));
        data.put("id", call.getString("id"));
        data.put("address", call.getString("address"));
        data.put("zpl", call.getString("zpl"));
        data.put("type", call.getString("type"));
        data.put("port", call.getString("port",  String.valueOf(TcpConnection.DEFAULT_ZPL_TCP_PORT)));
        data.put("useEscPosAsterik", call.getBoolean("useEscPosAsterik", false));
        data.put("initializeBeforeSend", call.getBoolean("initializeBeforeSend", false));
        data.put("charsetEncoding", call.getObject("charsetEncoding"));
        data.put("sendDelay", call.getString("sendDelay","0"));
        data.put("chunkSize", call.getString("chunkSize","0"));

        switch (data.getString("type")){
            case "bluetooth":
                // Code to handle bluetooth case goes here
                printBT(data,call);
                break;
            case "bluetoothLe":
                // Code to handle bluetooth case goes here
                printBTLe(data,call);
                break;
            case "bluetoothInsecure":
                // Code to handle bluetooth case goes here
                printBTInsecure(data,call);
                break;
            case "tcp":
                // Code to handle tcp case goes here
                printTCP(data,call);
                break;
            case "usb":
                // Code to handle usb case goes here

                break;
            default:
                // Code to handle unknown types goes here
                call.reject(new Exception("Connection type for the printer is not specified!").getMessage(),"COD01");
                break;
        }
    }

    public void printBT(JSONObject data, PluginCall call) {
        String zpl = data.optString("zpl","^XA^FO20,20^A0N,25,25^FDMissing ZPL Commands!^FS^XZ");
        String printerId = data.optString("id","");

        if (printerId.isEmpty()) {
            call.reject("Missing printerId");
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
                call.reject("Print failed on BT: " + e.getMessage(), "COD003");
            }
        }).start();
    }

    public void printBTLe(JSONObject data, PluginCall call) {
        String zpl = data.optString("zpl","^XA^FO20,20^A0N,25,25^FDMissing ZPL Commands!^FS^XZ");
        String printerId = data.optString("id","");

        if (printerId.isEmpty()) {
            call.reject("Missing printerId");
            return;
        }
        Context context = this.getActivity().getApplicationContext();

        new Thread(() -> {
            Connection connection = null;
            try {
                // Instantiate connection for given Bluetooth&reg; MAC Address.
                connection = new BluetoothLeConnection(printerId, context);

                // Open the connection - physical connection is established here.
                connection.open();

                // Send the data to printer as a byte array.
                connection.write(zpl.getBytes());

                // Make sure the data got to the printer before closing the connection
                Thread.sleep(500);
                call.resolve();
            } catch (Exception e) {
                call.reject("Print failed on BTLe: " + e.getMessage(), "COD004");

            }
            finally {
                // Close the connection to release resources.
                if (null != connection) {
                    try {
                        connection.close();
                    } catch (ConnectionException e) {
                        call.reject("Print failed on BTLe: " + e.getMessage(), "COD006");
                    }
                }
            }
        }).start();
    }

    public void printBTInsecure(JSONObject data, PluginCall call) {
        String zpl = data.optString("zpl","^XA^FO20,20^A0N,25,25^FDMissing ZPL Commands!^FS^XZ");
        String printerId = data.optString("id","");

        if (printerId.isEmpty()) {
            call.reject("Missing printerId");
            return;
        }

        new Thread(() -> {
            try {
                // Establishes an insecure Bluetooth® connection to a printer. Insecure Bluetooth® connections do not require the device and the printer to be paired.
                Connection thePrinterConn = new BluetoothConnectionInsecure(printerId);

                // Initialize
                Looper.prepare();

                // Open the connection - physical connection is established here.
                thePrinterConn.open();

                // Send the data to printer as a byte array.
                thePrinterConn.write(zpl.getBytes());

                // Make sure the data got to the printer before closing the connection
                Thread.sleep(500);

                // Close the insecure connection to release resources.
                thePrinterConn.close();

                Looper.myLooper().quit();
                call.resolve();
            } catch (Exception e) {
                call.reject("Print failed on BT: " + e.getMessage(), "COD005");

            }
        }).start();
    }

    public void printTCP(JSONObject data, PluginCall call) throws Exception {
        String zpl = data.optString("zpl","^XA^FO20,20^A0N,25,25^FDMissing ZPL Commands!^FS^XZ");
        String printerId = data.optString("id",""); // IP Address
        String port = data.optString("port");

        if (printerId.isEmpty()) {
            call.reject("Missing printerId");
            return;
        }
        // Instantiate connection for ZPL TCP port at given address
        Connection thePrinterConn = new TcpConnection(printerId, TcpConnection.DEFAULT_ZPL_TCP_PORT);

        try {
            // Open the connection - physical connection is established here.
            thePrinterConn.open();

            // Send the data to printer as a byte array.
            thePrinterConn.write(zpl.getBytes());

        } catch (ConnectionException e) {
            // Handle communications error here.
            e.printStackTrace();
            call.reject("Print failed on TCP: " + e.getMessage(), "COD002");
        } finally {
            // Close the connection to release resources.
            thePrinterConn.close();
        }
    }

    private DeviceConnection getPrinterConnection(JSONObject data) throws Exception {
        String type = data.getString("type");
        String id = data.getString("id");
        String sendDelay =  data.getString("sendDelay");
        String hashKey = type + "-" + id;

        DeviceConnection deviceConnection = this.getDevice(
                data.getString("type"),
                data.optString("id"),
                data.optString("address"),
                data.optInt("port", 9100),
                data.optInt("sendDelay", 0)
        );
        if (deviceConnection == null) {
            throw new JSONException(String.valueOf(new HashMap<String, Object>() {{
                put("error", "Device not found or not connected!");
                put("type", type);
                put("id", id);
            }}));
        }
        if (!this.connections.containsKey(hashKey)) {
            this.connections.put(hashKey, deviceConnection);
        }
        return deviceConnection;
    }

    private DeviceConnection getDevice(String type, String id, String address, int port, int sendDelay) throws Exception {
        String hashKey = type + "-" + id;
        if (!(type.equals("tcp") && this.connections.containsKey(hashKey))) {
            DeviceConnection connection = this.connections.get(hashKey);
            if (connection != null) {
                if (connection.isConnected()) {
                    return connection;
                } else {
                    this.connections.remove(hashKey);
                }
            }
        }

        if (type.equals("bluetooth")) {
            if (!bluetoothHasPermissions()) {
                throw new JSONException("Missing permission for bluetooth");
            }
            if (id.equals("first")) {
                return BluetoothPrintersConnections.selectFirstPaired();
            }
            BluetoothConnections printerConnections = new BluetoothConnections();
            for (com.zpl.helper.bluetooth.BluetoothConnection bluetoothConnection : printerConnections.getList()) {
                BluetoothDevice bluetoothDevice = bluetoothConnection.getDevice();
                bluetoothConnection.setSendDelay(sendDelay);
                try { if (bluetoothDevice.getAddress().equals(id)) { return bluetoothConnection; } } catch (Exception ignored) {}
                try { if (bluetoothDevice.getName().equals(id)) { return bluetoothConnection; } } catch (SecurityException ignored) {}
            }
        } else if (type.equals("tcp")) {
            com.zpl.helper.tcp.TcpConnection tcpConnection =  new  com.zpl.helper.tcp.TcpConnection(address, port);
            tcpConnection.setSendDelay(sendDelay);
            return tcpConnection;
        } else {
            UsbConnections printerConnections = new UsbConnections(this.getActivity());
            for (UsbConnection usbConnection : printerConnections.getList()) {
                UsbDevice usbDevice = usbConnection.getDevice();
                usbConnection.setSendDelay(sendDelay);
                try { if (usbDevice.getDeviceId() == Integer.parseInt(id)) { return usbConnection; } } catch (Exception ignored) {}
                try { if (Objects.requireNonNull(usbDevice.getProductName()).trim().equals(id)) { return usbConnection; } } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private boolean bluetoothHasPermissions() {
        return getPermissionState(BT_ALIAS) == PermissionState.GRANTED;
    }

    private boolean bluetoothIsEnabled() throws Exception {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i(ZPL_PLUGIN, (mBluetoothAdapter == null) + " < - (bluetoothManager == null) ");
        if (mBluetoothAdapter == null)
            throw new Exception("Device doesn't support Bluetooth!");

        // Here check only whether the Bluetooth hardware is off
        if(mBluetoothAdapter != null) {
            return mBluetoothAdapter.isEnabled();
        }
        // Bluetooth is off by convention is bluetoothManager is not defined
        return false;
    }

    private void checkBluetooth() throws Exception {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            throw new Exception("Device doesn't support Bluetooth!");
        } else if (!mBluetoothAdapter.isEnabled()) {
            throw new Exception("Device not enabled Bluetooth!");
        }
    }

    private void askForBTPermissionIfNotHaveAlready(PluginCall call) throws Exception
    {
        checkBluetooth();
        if (!this.bluetoothHasPermissions()) {
            requestPermissionForAliases(aliasesPermissions, call, "BTPermsCallback");
            return;
        }
    }


}