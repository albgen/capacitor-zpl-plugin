package com.zpl.capacitor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
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
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zpl.helper.DeviceConnection;
import com.zpl.helper.bluetooth.BluetoothConnections;
import com.zpl.helper.bluetooth.BluetoothPrintersConnections;
import com.zpl.helper.usb.UsbConnections;

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
                Log.i("ESCPOSPlugin", str);
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
                Log.i("ESCPOSPlugin", "PermissionState.GRANTED already");
            } else {
                if (getPermissionState(BT_ALIAS) == PermissionState.DENIED) {
                    //Log.i("ESCPOSPlugin", "Permission is required for bluetooth");
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
                for (com.zpl.helper.usb.UsbConnection usbConnection : printerConnections.getList()) {
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
            for (com.zpl.helper.usb.UsbConnection usbConnection : printerConnections.getList()) {
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
        Log.i("ESCPOSPlugin", (mBluetoothAdapter == null) + " < - (bluetoothManager == null) ");
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