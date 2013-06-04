/*
 * Copyright 2013 CellaSecure
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.washington.cs.cellasecure.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth Utility library for connecting Android mobile applications with
 * Bluetooth devices.
 * <p/>
 * requires BLUETOOTH and BLUETOOTH_ADMIN
 *
 * @author CellaSecure
 */
public class BluetoothUtility {

    public static final String TAG = "BluetoothUtility";
    public static final int BLUETOOTH_REQUEST_ID = 1337;

    private BluetoothAdapter mBluetoothAdapter;    // Connection point for Bluetooth devices
    private BroadcastReceiver mBroadcastReceiver;   // Broadcast receiver to listen for various callbacks
    private Activity mActivity;            // Parent activity of this instance
    private List<BluetoothDevice> mDiscoveredDevices;   // List of found devices that have not been paired

    private OnDiscoveryListener mDiscoveryListener;   // Listener to handle device discovery
    private OnDiscoveryFinishedListener mDiscoveryFinishedListener; // Listener to handle discovery finished


    /**
     * Constructs a new Bluetooth utility to manage devices
     *
     * @param activity Context for mobile application
     */
    public BluetoothUtility(Activity activity) {
        Log.d(TAG, "Bluetooth Utility constructor");
        mActivity = activity;
        if (mActivity == null)
            throw new IllegalArgumentException("An activity is required to register BT receivers!");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            throw new IllegalStateException("Bluetooth not supported");
        mDiscoveredDevices = new ArrayList<BluetoothDevice>();

        // register receiver to hear when a device is found
        mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDiscoveredDevices.add(device);
                    if (mDiscoveryListener != null)
                        mDiscoveryListener.onDiscovery(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    mActivity.unregisterReceiver(mBroadcastReceiver);
                    if (mDiscoveryFinishedListener != null)
                        mDiscoveryFinishedListener.onDiscoveryFinished();
                }// else if (BluetoothAdapter.STATE_DISCONNECTED )
            }
        };
        Log.d(TAG, "BluetoothUtility created");
    }

    /**
     * Start a discovery for in-range Bluetooth devices, scanning
     * for at most 12 seconds.  When a device is found will call
     * onDiscovery with an updated list of found devices.
     *
     * @see OnDiscoveryListener
     */
    public void scanForDevices() {
        if (!mBluetoothAdapter.isEnabled())
            throw new IllegalStateException("Bluetooth must be enabled");

        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        IntentFilter action_found_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mActivity.registerReceiver(mBroadcastReceiver, action_found_filter);
        IntentFilter discovery_finished_filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mActivity.registerReceiver(mBroadcastReceiver, discovery_finished_filter);

        mBluetoothAdapter.startDiscovery();
    }

    /**
     * @return true if Bluetooth is enabled on the mobile device, else false
     */
    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Enables bluetooth via ACTION_REQUEST_ENABLE intent
     */
    public void enableBluetooth() {
        if (mActivity != null) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(i, BLUETOOTH_REQUEST_ID);
        } else {
            throw new IllegalStateException("Activity must be non-null");
        }
    }

    /**
     * @return true if Bluetooth adapter is discovering, else false
     */
    public boolean isScanning() {
        return mBluetoothAdapter.isDiscovering();
    }

    /**
     * Cancel device discovery
     */
    public void cancelDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    /**
     * Accessor for both bonded and discovered devices
     *
     * @return a list of all discovered and bonded devices
     * @throws IllegalStateException if Bluetooth is not enabled
     */
    public List<BluetoothDevice> getAllDevices() {
        if (!mBluetoothAdapter.isEnabled())
            throw new IllegalStateException("Bluetooth must be enabled");
        List<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>(mDiscoveredDevices);
        allDevices.addAll(mBluetoothAdapter.getBondedDevices());
        return allDevices;
    }

    /**
     * Accessor for bonded devices
     *
     * @return a list of all bonded devices
     * @throws IllegalStateException if Bluetooth is not enabled
     */
    public List<BluetoothDevice> getBondedDevices() {
        if (!mBluetoothAdapter.isEnabled())
            throw new IllegalStateException("Bluetooth must be enabled");
        return new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices());
    }

    /**
     * Accessor for discovered devices
     *
     * @return a list of all discovered devices
     * @throws IllegalStateException if Bluetooth is not enabled
     */
    public List<BluetoothDevice> getDiscoveredDevices() {
        return mDiscoveredDevices;
    }

    /**
     * Create a bond with the given device
     *
     * @param device the Bluetooth device to bond with
     * @return true if already paired or on successful pairing, false otherwise
     */
    public boolean pairDevice(BluetoothDevice device) {
        switch (device.getBondState()) {
            case (BluetoothDevice.BOND_BONDED):
                return true;
            case (BluetoothDevice.BOND_NONE):
                try {
                    return (Boolean) (device.getClass()).getMethod("createBond").invoke(device);
                } catch (Exception e) {
                    return false;
                }
                // case (BluetoothDevice.BOND_BONDING): // taken care of by default
            default:
                return false;
        }
    }

    /**
     * Erase the bond with the given device
     *
     * @param device the Bluetooth device to unpair from
     * @return true if already unpaired or on successful unpairing, else false
     */
    public boolean unpairDevice(BluetoothDevice device) {
        switch (device.getBondState()) {
            case (BluetoothDevice.BOND_BONDED):
                try {
                    return (Boolean) (device.getClass()).getMethod("removeBond").invoke(device);
                } catch (Exception e) {
                    return false;
                }
            case (BluetoothDevice.BOND_NONE):
                return true;
            // case (BluetoothDevice.BOND_BONDING): // taken care of by default
            default:
                return false;
        }
    }
    
    /* For Android */

    public void onPause() {
        try {
            if (mActivity != null)
                mActivity.unregisterReceiver(mBroadcastReceiver);
            mBluetoothAdapter.cancelDiscovery();
        } catch (IllegalArgumentException e) { /* Do nothing */ }
    }

    public void onResume() {
        if (mActivity != null) {
            IntentFilter action_found_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mActivity.registerReceiver(mBroadcastReceiver, action_found_filter);
            IntentFilter discovery_finished_filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mActivity.registerReceiver(mBroadcastReceiver, discovery_finished_filter);
        }
    }
    
    /* Callback setters */

    public void setOnDiscoveryListener(OnDiscoveryListener dl) {
        mDiscoveryListener = dl;
    }

    //    public void setOnConnectListener (OnConnectListener cl) {
    //        mConnectListener = cl;
    //    }

    public void setOnDiscoveryFinishedListener(OnDiscoveryFinishedListener dfl) {
        this.mDiscoveryFinishedListener = dfl;
    }

    /* Listener interfaces */

    public interface OnDiscoveryListener {
        /**
         * Callback to notify a client when a device is found
         *
         * @param device the discovered bluetooth device
         */
        public void onDiscovery(BluetoothDevice device);
    }

    //    public interface OnConnectListener {
    //        /**
    //         * Callback to return an established connection
    //         *
    //         * @param connection
    //         *            the connection to the Bluetooth device
    //         */
    //        public void onConnect(Connection connection);
    //    }

    public interface OnDiscoveryFinishedListener {
        /**
         * Callback to notify a client when the discovery is finished
         */
        public void onDiscoveryFinished();
    }
}