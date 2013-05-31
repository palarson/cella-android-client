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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.washington.cs.cellasecure.fragments.DriveUnlockFragment;
import edu.washington.cs.cellasecure.storage.DeviceUtils;

import java.io.IOException;

public class DriveManageActivity extends Activity implements Drive.OnConnectListener,
        Drive.OnLockQueryResultListener {

    private static final String TAG = "DriveManageActivity";

    public static final String KEY_BUNDLE_LOCK_STATUS = "locked";

    private static Drive mDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_manage);
        Bundle args = getIntent().getExtras();
        if (args == null) {
            throw new IllegalStateException("DriveManageActivity expects a bundled Drive as " +
                    "input" + ".");
        }
        mDrive = (Drive) args.get(Drive.KEY_BUNDLE_DRIVE);

        mDrive.setOnConnectListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.device_manage, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // more cases here
            default:
                return false;
        }
    }

    @Override
    public void onConnect() {
        assert mDrive.isConnected();
        try {
            DeviceUtils.addToFile(getParent(), mDrive);
            mDrive.setOnLockQueryResultListener(this);
            mDrive.queryLockStatus();
        } catch (IOException e) { /* let pass */ }
    }

    @Override
    public void onConnectFailure(IOException connectException) {
        Log.e(TAG, "Failed to connect to drive", connectException);
        finish(); // DIE
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrive.isConnected()) {
            mDrive.disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mDrive.isConnected()) {
            mDrive.connect();
        }
    }

    @Override
    public void onLockQueryResult(final boolean status, IOException e) {
        if (e != null) {
            Log.e(TAG, "Lock query failure", e);
            finish();
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentTransaction trans = getFragmentManager().beginTransaction();
                Fragment f = new DriveUnlockFragment();
                Bundle args = new Bundle();
                args.putParcelable(Drive.KEY_BUNDLE_DRIVE, mDrive);
                args.putBoolean(KEY_BUNDLE_LOCK_STATUS, status);
                f.setArguments(args);
                trans.replace(R.id.drive_manage_fragment_container, f);
                findViewById(R.id.drive_loading_progress).setVisibility(View.GONE);
                trans.commit();
            }
        });
    }
}
