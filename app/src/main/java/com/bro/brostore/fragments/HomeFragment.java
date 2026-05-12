package com.example.appstoredemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;

import com.example.appstoredemo.activities.MainActivity;
import com.example.appstoredemo.R;

/**
 * Landing fragment allowing users to jump to the discover or installed tabs.
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {
    private View myAppsBtn;
    private View discoverBtn;
    /**
     * Inflates the fragment layout.
     *
     * @param inflater layout inflater provided by the hosting activity.
     * @param container optional parent container.
     * @param savedInstanceState previously saved state or {@code null}.
     * @return inflated view hierarchy.
     * @throws IllegalStateException if inflation fails.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Binds UI listeners and updates the initial button state.
     *
     * @param view root view returned by {@link #onCreateView}.
     * @param savedInstanceState previously saved state or {@code null}.
     * @return void.
     * @throws IllegalStateException if required activity methods are unavailable.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        myAppsBtn = view.findViewById(R.id.buttonMyApps);
        discoverBtn = view.findViewById(R.id.buttonDiscover);
        myAppsBtn.setOnClickListener(v -> {
            MainActivity act = (MainActivity) requireActivity();
            if (act.hasAllPermissions()) {
                act.openFragment(new MyAppsFragment());
            } else {
                act.requestAllPermissions();
            }
        });
        discoverBtn.setOnClickListener(v -> {
            MainActivity act = (MainActivity) requireActivity();
            if (act.hasAllPermissions()) {
                act.openFragment(new DiscoverFragment());
            } else {
                act.requestAllPermissions();
            }
        });
        view.findViewById(R.id.buttonCloseApp).setOnClickListener(v ->
                ((MainActivity) requireActivity()).exitToKiosk());
        updateButtonState();
    }

    /**
     * Refreshes button state when the fragment resumes.
     *
     * @return void.
     * @throws IllegalStateException if activity interaction fails.
     */
    @Override
    public void onResume() {
        super.onResume();
        updateButtonState();
    }

    /**
     * Enables or disables navigation buttons based on permission state.
     *
     * @return void.
     * @throws IllegalStateException if the hosting activity cannot be cast to {@link MainActivity}.
     */
    public void updateButtonState() {
        MainActivity act = (MainActivity) requireActivity();
        boolean enabled = act.hasAllPermissions();
        if (myAppsBtn != null) myAppsBtn.setEnabled(enabled);
        if (discoverBtn != null) discoverBtn.setEnabled(enabled);
    }
}