package com.example.appstoredemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appstoredemo.adapters.AppAdapter;
import com.example.appstoredemo.R;
import com.example.appstoredemo.activities.MainActivity;
import com.example.appstoredemo.repositories.DownloadRepository;
import com.example.appstoredemo.viewmodels.AppListViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Shows locally installed applications and their update states.
 */
@AndroidEntryPoint
public class MyAppsFragment extends Fragment {

    private AppAdapter adapter;
    private AppListViewModel viewModel;
    @Inject
    DownloadRepository downloadRepository;

    /**
     * Inflates the "My Apps" layout.
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
        return inflater.inflate(R.layout.fragment_my_apps, container, false);
    }

    /**
     * Configures the recycler view and subscribes to installed app updates.
     *
     * @param view root view returned by {@link #onCreateView}.
     * @param savedInstanceState previously saved state or {@code null}.
     * @return void.
     * @throws IllegalStateException if required dependencies are unavailable.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (downloadRepository == null) {
            throw new IllegalStateException("DownloadRepository injection failed");
        }
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppAdapter(downloadRepository);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.buttonCloseApp).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showHome());

        viewModel = new ViewModelProvider(requireActivity()).get(AppListViewModel.class);
        if (viewModel == null) {
            throw new IllegalStateException("AppListViewModel not available");
        }
        viewModel.getApps().observe(getViewLifecycleOwner(), list -> adapter.submitList(new ArrayList<>(list)));
        downloadRepository.getStates()
                .observe(getViewLifecycleOwner(), map -> adapter.updateStates(new HashMap<>(map)));
    }

    /**
     * Refreshes installed state when the fragment resumes.
     *
     * @return void.
     * @throws IllegalStateException if the view model is unavailable.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel == null) {
            throw new IllegalStateException("AppListViewModel not initialized");
        }
        viewModel.refreshInstalledState();
    }
}