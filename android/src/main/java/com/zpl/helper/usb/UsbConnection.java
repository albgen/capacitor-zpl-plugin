package com.zpl.helper.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.zpl.helper.DeviceConnection;
import com.zpl.helper.ZPLConnectionException;

import java.io.IOException;

public class UsbConnection extends DeviceConnection {

    private UsbManager usbManager;
    private UsbDevice usbDevice;

    /**
     * Create un instance of UsbConnection.
     *
     * @param usbManager an instance of UsbManager
     * @param usbDevice  an instance of UsbDevice
     */
    public UsbConnection(UsbManager usbManager, UsbDevice usbDevice) {
        super();
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
    }

    /**
     * Get the instance UsbDevice connected.
     *
     * @return an instance of UsbDevice
     */
    public UsbDevice getDevice() {
        return this.usbDevice;
    }

    /**
     * Start socket connection with the usbDevice.
     */
    public UsbConnection connect() throws ZPLConnectionException {
        if (this.isConnected()) {
            return this;
        }

        try {
            this.outputStream = new com.zpl.helper.usb.UsbOutputStream(this.usbManager, this.usbDevice);
            this.data = new byte[0];
        } catch (IOException e) {
            e.printStackTrace();
            this.outputStream = null;
            throw new ZPLConnectionException("Unable to connect to USB device.");
        }
        return this;
    }

    /**
     * Close the socket connection with the usbDevice.
     */
    public UsbConnection disconnect() {
        this.data = new byte[0];
        if (this.isConnected()) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.outputStream = null;
        }
        return this;
    }

    /**
     * Send data to the device.
     */
    public void send() throws ZPLConnectionException {
        this.send(0);
    }
    /**
     * Send data to the device.
     */
    public void send(int addWaitingTime) throws ZPLConnectionException {
        try {
            this.outputStream.write(this.data);
            this.data = new byte[0];
        } catch (IOException e) {
            e.printStackTrace();
            throw new ZPLConnectionException(e.getMessage());
        }
    }
}
