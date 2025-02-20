package com.zpl.helper;
import java.io.IOException;
import java.io.OutputStream;

public abstract class DeviceConnection {
    protected OutputStream outputStream;
    protected byte[] data;
    protected int sendDelay; // Added sendDelay parameter

    public DeviceConnection() {
        this.outputStream = null;
        this.data = new byte[0];
        this.sendDelay = 0; // Default send delay set to 0
    }

    public abstract DeviceConnection connect() throws ZPLConnectionException;

    public abstract DeviceConnection disconnect();

    /**
     * Check if OutputStream is open.
     *
     * @return true if is connected
     */
    public boolean isConnected() {
        return this.outputStream != null;
    }

    /**
     * Set the send delay in milliseconds.
     *
     * @param delay Delay time in milliseconds
     */
    public void setSendDelay(int delay) {
        this.sendDelay = delay;
    }

    /**
     * Get the current send delay in milliseconds.
     *
     * @return Current send delay
     */
    public int getSendDelay() {
        return this.sendDelay;
    }

    /**
     * Add data to send.
     */
    public void write(byte[] bytes) {
        byte[] data = new byte[bytes.length + this.data.length];
        System.arraycopy(this.data, 0, data, 0, this.data.length);
        System.arraycopy(bytes, 0, data, this.data.length, bytes.length);
        this.data = data;
    }

    /**
     * Send data to the device.
     */
    public void send() throws ZPLConnectionException {
        this.send(0);
    }

    /**
     * Send data to the device with additional waiting time.
     */
    public void send(int addWaitingTime) throws ZPLConnectionException {
        if (!this.isConnected()) {
            throw new ZPLConnectionException("Unable to send data to device.");
        }
        try {
            this.outputStream.write(this.data);
            this.outputStream.flush();
            int waitingTime = addWaitingTime + this.data.length / 16 + this.sendDelay; // Include sendDelay
            this.data = new byte[0];
            if (waitingTime > 0) {
                Thread.sleep(waitingTime);
            }
        } catch (IOException | InterruptedException e) {
            this.disconnect();
            this.connect();
            e.printStackTrace();
            throw new ZPLConnectionException(e.getMessage());
        }
    }
}
