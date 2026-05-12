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

import com.example.appstoredemo.adapters.DiscoverAdapter;
import com.example.appstoredemo.R;
import com.example.appstoredemo.activities.MainActivity;
import com.example.appstoredemo.repositories.DownloadRepository;
import com.example.appstoredemo.viewmodels.DiscoverViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Displays remote applications users can discover and download.
 */
@AndroidEntryPoint
public class DiscoverFragment extends Fragment {

    private DiscoverAdapter adapter;
    private DiscoverViewModel viewModel;
    @Inject
    DownloadRepository downloadRepository;

    /**
     * Inflates the discovery layout.
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
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    /**
     * Configures the recycler view and subscribes to repository data streams.
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
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewOptional);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DiscoverAdapter(downloadRepository);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.buttonCloseApp).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showHome());

        viewModel = new ViewModelProvider(requireActivity()).get(DiscoverViewModel.class);
        if (viewModel == null) {
            throw new IllegalStateException("DiscoverViewModel not available");
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
            throw new IllegalStateException("DiscoverViewModel not initialized");
        }
        viewModel.refreshInstalledState();
    }
}