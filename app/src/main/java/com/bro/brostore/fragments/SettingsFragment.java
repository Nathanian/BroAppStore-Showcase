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
 * Displays settings shortcuts for the kiosk deployment.
 */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    /**
     * Inflates the settings layout.
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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    /**
     * Binds the navigation buttons after view creation.
     *
     * @param view root view returned by {@link #onCreateView}.
     * @param savedInstanceState previously saved state or {@code null}.
     * @return void.
     * @throws IllegalStateException if the hosting activity cannot be cast to {@link MainActivity}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> ((MainActivity) requireActivity()).showHome());
    }
}