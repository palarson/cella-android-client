package edu.washington.cs.cellasecure.fragments;

import java.io.IOException;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import edu.washington.cs.cellasecure.Drive;
import edu.washington.cs.cellasecure.Drive.OnPasswordSetListener;
import edu.washington.cs.cellasecure.DriveManageActivity;
import edu.washington.cs.cellasecure.Drive.OnLockStateChangeListener;
import edu.washington.cs.cellasecure.R;
import edu.washington.cs.cellasecure.bluetooth.DeviceConfiguration;

public class DriveConfigureFragment extends Fragment implements View.OnClickListener, Drive.OnConfigurationListener, 
    OnLockStateChangeListener, OnPasswordSetListener {
    private final static String TAG = "DriveConfigureFragment";
    private static String mUUID;
    
    private Drive mDrive;
    private Button mLockDriveButton;
    private Button mSetConfigButton;
    private Button mChangePasswordButton;
    
    private RadioGroup mRadioGroup;
    private EditText mPasswordBox;
    
    private int mEncryptionLevel;
    private int mPendingEncryptionLevel;
    
    
    /*
     * (non-Javadoc)
     *
     * @see android.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drive_configure, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        activity.findViewById(R.id.drive_loading_progress).setVisibility(View.GONE);

        Bundle args = getArguments();
        mDrive = (Drive) args.getParcelable(Drive.KEY_BUNDLE_DRIVE);
        mEncryptionLevel = args.getInt(DriveManageActivity.KEY_BUNDLE_ENCRYPTION_LEVEL);
        mPendingEncryptionLevel = mEncryptionLevel;
        
        mRadioGroup = (RadioGroup) activity.findViewById(R.id.encryption_settings_group);
        mPasswordBox = (EditText) activity.findViewById(R.id.password_box);
        mLockDriveButton = (Button) activity.findViewById(R.id.configure_lock_drive);
        mSetConfigButton = (Button) activity.findViewById(R.id.configure_set_configuration);
        mChangePasswordButton = (Button) activity.findViewById(R.id.configure_change_password);
        mLockDriveButton.setOnClickListener(this);
        mSetConfigButton.setOnClickListener(this);
        mChangePasswordButton.setOnClickListener(this);
        
        TelephonyManager tManager = 
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        mUUID = tManager.getDeviceId();

        mDrive.setOnLockStateChangeListener(this);
        mDrive.setOnConfigurationListener(this);
        mDrive.setOnPasswordSetListener(this);
    }
    
    @Override
    public void onClick(View v) {
        Log.d(TAG, "Button clicked");
        if (v.equals(mLockDriveButton)) {
            Log.d(TAG, "Lock drive button pressed");
            if (mEncryptionLevel != 0) 
                mDrive.lock();
            else
                getActivity().finish();
        } else if (v.equals(mSetConfigButton)) {
            Log.d(TAG, "Set config button pressed");
            int encryptionId = mRadioGroup.getCheckedRadioButtonId();
            if (encryptionId == R.id.encryption_level_0)
                mPendingEncryptionLevel = 0;
            else if (encryptionId == R.id.encryption_level_1)
                mPendingEncryptionLevel = 1;
            else
                mPendingEncryptionLevel = 2;
            Log.e(TAG, "encryptionLevel: " + mPendingEncryptionLevel);
            DeviceConfiguration config = new DeviceConfiguration();
            config.setOption("encryption_level", "" + mPendingEncryptionLevel);
            mDrive.sendConfiguration(config, mUUID);
        } else if (v.equals(mChangePasswordButton)) {
            Log.d(TAG, "Change password button pressed");
            String password = mPasswordBox.getText().toString();
            Log.d(TAG, "Password: " + password);

            mDrive.setPassword(password, mUUID);
        }
    }

    @Override
    public void onConfigurationRead(DeviceConfiguration config, IOException e) {
        return;
    }

    @Override
    public void onConfigurationWritten(IOException e) {
        mEncryptionLevel = mPendingEncryptionLevel;
        Log.d(TAG, "Beginning Toast");
        Activity activity = getActivity();
        if (activity != null) {
            final String message = (e == null) ? "Configuration set successfully" : "Configuration failed";
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onLockStateChanged(boolean status, IOException lockStateException) {
        if (!status) {
            Log.e(TAG, "Locking failed");
        } else {
            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Drive locked", Toast.LENGTH_LONG).show();
                }
            });
            getActivity().finish();
        }
    }

    @Override
    public void onPasswordSet(IOException e) {
            Activity activity = getActivity();
            final String message = (e == null) ? "Password updated" : "Password failed to be updated";
            if (activity != null) activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            });
    }
}
