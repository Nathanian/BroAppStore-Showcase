package com.example.appstoredemo.activities;

import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import com.example.appstoredemo.R;
import com.example.appstoredemo.fragments.HomeFragment;
import com.example.appstoredemo.viewmodels.AppListViewModel;
import com.example.appstoredemo.viewmodels.DiscoverViewModel;
import com.example.appstoredemo.repositories.DownloadRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Hosts the main navigation flow of the BroStore application.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_CODE = 42;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Inject
    DownloadRepository downloadRepository;
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getData() == null) return;

            String pkg = intent.getData().getSchemeSpecificPart();
            if (pkg == null) return;

            String action = intent.getAction();
            DiscoverViewModel discoverVm = new ViewModelProvider(MainActivity.this).get(DiscoverViewModel.class);
            AppListViewModel appListVm = new ViewModelProvider(MainActivity.this).get(AppListViewModel.class);

            if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_REPLACED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                downloadRepository.markInstalled(pkg);
            }

            discoverVm.refreshInstalledState();
            appListVm.refreshInstalledState();

            discoverVm.load();
            appListVm.load();
        }
    };

    /**
     * Sets up dependency injection, permissions, and default navigation state.
     *
     * @param savedInstanceState previously saved state bundle or {@code null}.
     * @return void.
     * @throws IllegalStateException if required dependencies cannot be initialized.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadRepository.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
        requestAllPermissions();
        new ViewModelProvider(this).get(DiscoverViewModel.class);
        new ViewModelProvider(this).get(AppListViewModel.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
        if (savedInstanceState == null) {
            openFragment(new HomeFragment());
        }
    }

    /**
     * Cleans up registered receivers and repository resources.
     *
     * @return void.
     * @throws IllegalStateException if receiver unregistration fails.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to unregister package receiver", e);
        }
        downloadRepository.dispose();
    }

    /**
     * Replaces the fragment container with the provided fragment instance.
     *
     * @param fragment fragment to display.
     * @return void.
     * @throws IllegalArgumentException if {@code fragment} is {@code null}.
     */
    public void openFragment(Fragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment must not be null");
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
    /**
     * Navigates to the home screen, clearing the fragment back stack.
     *
     * @return void.
     * @throws IllegalStateException if the fragment transaction cannot be committed.
     */
    public void showHome() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        openFragment(new HomeFragment());
    }

    /**
     * Checks whether all mandatory runtime permissions are granted.
     *
     * @return {@code true} when every required permission is granted.
     * @throws IllegalStateException if permission checks fail unexpectedly.
     */
    public boolean hasAllPermissions() {
        try {
            for (String p : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check permissions", e);
        }
        return true;
    }

    /**
     * Requests any missing runtime permissions from the user.
     *
     * @return void.
     * @throws IllegalStateException if the permission dialog cannot be displayed.
     */
    public void requestAllPermissions() {
        List<String> missing = new ArrayList<>();
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            try {
                ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to request permissions", e);
            }
        }
    }

    /**
     * Handles the result of runtime permission requests.
     *
     * @param requestCode request identifier for the permission dialog.
     * @param permissions permission strings requested.
     * @param grantResults grant results for each permission.
     * @return void.
     * @throws IllegalStateException if the fragment cannot be updated.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof HomeFragment) {
                try {
                    ((HomeFragment) fragment).updateButtonState();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to update permission state", e);
                }
            }
        }
    }
    /**
     * Finishes the activity and relaunches the kiosk application if present.
     *
     * @return void.
     * @throws IllegalStateException if launching the kiosk intent fails.
     */
    public void exitToKiosk() {
        finish();

        String pkg = "YOUR_PACKAGE_NAME";
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(pkg);
        if (launch == null) {
            Toast.makeText(this, "Kiosk nicht installiert", Toast.LENGTH_SHORT).show();
            return;
        }

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean running = false;
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            if (procs != null) {
                for (ActivityManager.RunningAppProcessInfo p : procs) {
                    if (pkg.equals(p.processName)) {
                        running = true;
                        break;
                    }
                }
            }
        }

        if (running) {
            launch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            startActivity(launch);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to launch kiosk app", e);
        }
    }
}
