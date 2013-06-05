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

package edu.washington.cs.cellasecure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import edu.washington.cs.cellasecure.bluetooth.BluetoothUtility;
import edu.washington.cs.cellasecure.storage.DeviceUtils;
import edu.washington.cs.cellasecure.storage.DeviceUtils.OnPairedDrivesLoadListener;

public class DriveListActivity extends ListActivity implements OnItemClickListener {

    public static final String TAG = "DriveListActivity";

    private BluetoothUtility mBT;
    private LinearLayout mDriveListContainer;
    private MenuItem         mMenuRefresh;
    private ProgressBar mDriveScanIndicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_drive_list);
        ListView view = getListView();
        view.setOnItemClickListener(this);

        DriveListAdapter adapter = new DriveListAdapter();
        setListAdapter(adapter);
        mDriveScanIndicator = (ProgressBar) findViewById(android.R.id.progress);
        mDriveListContainer = (LinearLayout) findViewById(R.id.list_drive_container);
        
        mBT = new BluetoothUtility(this);
        mBT.setOnDiscoveryFinishedListener(adapter);
        mBT.setOnDiscoveryListener(adapter);
        DeviceUtils.loadDrives(this, adapter);
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.device_list, menu);

        mMenuRefresh = menu.findItem(R.id.menu_refresh_devices);
        assert mMenuRefresh != null;
        mMenuRefresh.setActionView(R.layout.actionbar_indeterminate_progress);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBT != null) {
            mBT.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBT != null) {
            mBT.onPause();
        }
        mDriveScanIndicator.setVisibility(View.GONE);
        if (mMenuRefresh != null) {
            mMenuRefresh.setActionView(null);
        }
        mDriveListContainer.setVisibility(View.VISIBLE);
    }

    /*
         * (non-Javadoc)
         *
         * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
         */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_devices:
                mMenuRefresh.setActionView(R.layout.actionbar_indeterminate_progress);
                mDriveListContainer.setVisibility(View.GONE);
                mDriveScanIndicator.setVisibility(View.VISIBLE);
                DriveListAdapter adapter = (DriveListAdapter) getListAdapter();
                adapter.clear();
                DeviceUtils.loadDrives(this, adapter);
                mMenuRefresh.setActionView(R.layout.actionbar_indeterminate_progress);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothUtility.BLUETOOTH_REQUEST_ID:
                if (mBT != null) {
                    mBT.scanForDevices();
                }
                break;
            default:
                // no-op
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (mBT.isScanning())
            mBT.cancelDiscovery();
        Drive d = (Drive) getListAdapter().getItem(position);
        Intent i = new Intent(this, DriveManageActivity.class);
        i.putExtra(Drive.KEY_BUNDLE_DRIVE, d);
        startActivity(i);
    }

    private class DriveListAdapter extends BaseAdapter implements ListAdapter, BluetoothUtility.OnDiscoveryListener,
            BluetoothUtility.OnDiscoveryFinishedListener, OnPairedDrivesLoadListener {
        private static final int VIEW_TYPE_PAIRED_INRANGE = 0;
        private static final int VIEW_TYPE_INRANGE        = 1;
        private static final int VIEW_TYPE_PAIRED_OORANGE = 2;
        private static final int VIEW_TYPE_COUNT          = 3;

        // load paired devices and add to list
        // then, scan over bluetooth and add pairable devices
        //
        // [In range + paired] > [Pairable] > [Out of range + paired]
        // In range + paired: text is black, in an "enabled" state
        // lock status is shown
        // Pairable: text is black, in an "enabled" state
        // "+" icon is shown instead of lock status
        // Out of range + paired: text is grayed out, in a "disabled" state

        private final Set<Drive> mInRangeDrives          = new HashSet<Drive>();
        private final Set<Drive> mPairedInRangeDrives    = new HashSet<Drive>();
        private final Set<Drive> mPairedOutOfRangeDrives = new HashSet<Drive>();

        @Override
        public int getCount() {
            return mPairedInRangeDrives.size() + mInRangeDrives.size() + mPairedOutOfRangeDrives.size();
        }

        @Override
        public Object getItem(int position) {
            int threshold1 = mPairedInRangeDrives.size();
            int threshold2 = threshold1 + mInRangeDrives.size();
            if (position >= threshold2) {
                return mPairedOutOfRangeDrives.toArray()[position - threshold2];
            } else if (position >= threshold1) {
                return mInRangeDrives.toArray()[position - threshold1];
            } else {
                return mPairedInRangeDrives.toArray()[position];
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public boolean isEnabled(int position) {
            return (position < mPairedInRangeDrives.size() + mInRangeDrives.size());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater li = getLayoutInflater();
            View v = (convertView != null) ? convertView : li.inflate(R.layout.list_drive, null);
            assert v != null;
            Drive d = (Drive) getItem(position);
            int viewType = getItemViewType(position);

            switch (viewType) {
                case VIEW_TYPE_PAIRED_INRANGE:
                    ((TextView) v.findViewById(R.id.list_drive_name)).setText(d.getName());
                    ((TextView) v.findViewById(R.id.list_drive_address)).setText(d.getAddress());
                    break;
                case VIEW_TYPE_INRANGE:
                    ((TextView) v.findViewById(R.id.list_drive_name)).setText(d.getName());
                    ((TextView) v.findViewById(R.id.list_drive_address)).setText(d.getAddress());
                    break;
                case VIEW_TYPE_PAIRED_OORANGE:
                    TextView tvDriveName = (TextView) v.findViewById(R.id.list_drive_name);
                    tvDriveName.setTextColor(Color.LTGRAY);
                    tvDriveName.setText(d.getName());
                    TextView tvDriveAddress = (TextView) v.findViewById(R.id.list_drive_address);
                    tvDriveAddress.setTextColor(Color.LTGRAY);
                    tvDriveAddress.setText(d.getAddress());
                    break;
            }
            return v;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return mPairedInRangeDrives.isEmpty() && mInRangeDrives.isEmpty() && mPairedOutOfRangeDrives.isEmpty();
        }

        @Override
        public int getItemViewType(int position) {
            int threshold1 = mPairedInRangeDrives.size();
            int threshold2 = threshold1 + mInRangeDrives.size();
            if (position >= threshold2) {
                return VIEW_TYPE_PAIRED_OORANGE;
            } else if (position >= threshold1) {
                return VIEW_TYPE_INRANGE;
            } else {
                return VIEW_TYPE_PAIRED_INRANGE;
            }
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public void onDiscovery(BluetoothDevice device) {
            Log.d(TAG, "onDiscovery called");
            Drive drive = new Drive(device);
            if (drive.getName().startsWith(DeviceUtils.DEVICE_NAME_PREFIX) 
                    && drive.getAddress().startsWith(DeviceUtils.RN_MAC_PREFIX)) {
                if (mPairedOutOfRangeDrives.remove(drive)) {
                    mPairedInRangeDrives.add(drive);
                } else {
                    mInRangeDrives.add(drive);
                }
    
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDriveScanIndicator.setVisibility(View.GONE);
                        mDriveListContainer.setVisibility(View.VISIBLE);
                    }
                });
                notifyDataSetChanged();
            }
        }

        @Override
        public void onDiscoveryFinished() {
            Log.d(TAG, "onDiscoveryFinished called");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDriveScanIndicator.setVisibility(View.GONE);
                    if (mMenuRefresh != null) {
                        mMenuRefresh.setActionView(null);
                    }
                    mDriveListContainer.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onPairedDrivesLoad(List<Drive> pairedDrives) {
            Log.d(TAG, "onPairedDrivesLoad called");
            mPairedOutOfRangeDrives.addAll(pairedDrives);

            if (!pairedDrives.isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDriveScanIndicator.setVisibility(View.GONE);
                        mDriveListContainer.setVisibility(View.VISIBLE);
                    }
                });
                notifyDataSetChanged();
            }
            if (!mBT.isEnabled()) {
                mBT.enableBluetooth();
            } else {
                mBT.scanForDevices();
            }
        }

        public void clear() {
            mInRangeDrives.clear();
            mPairedOutOfRangeDrives.clear();
            mPairedInRangeDrives.clear();
        }
    }
}
