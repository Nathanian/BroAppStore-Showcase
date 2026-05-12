package com.example.appstoredemo.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.appstoredemo.R;
import com.example.appstoredemo.models.AppModel;
import com.example.appstoredemo.models.DownloadState;
import com.example.appstoredemo.models.UpdateInfo;
import com.example.appstoredemo.repositories.DownloadRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for the "My Apps" list showing installed applications.
 */
public class AppAdapter extends ListAdapter<AppModel, AppAdapter.AppViewHolder> {
    private final DownloadRepository downloadRepository;
    private Map<String, DownloadState> states = new HashMap<>();

    /**
     * Creates the adapter with a backing download repository.
     *
     * @param repo repository used to initiate download actions.
     * @throws IllegalArgumentException if {@code repo} is {@code null}.
     */
    public AppAdapter(DownloadRepository repo) {
        super(DIFF_CALLBACK);
        if (repo == null) {
            throw new IllegalArgumentException("DownloadRepository must not be null");
        }
        this.downloadRepository = repo;
        setHasStableIds(true);
    }

    /**
     * Updates the adapter with the latest download state mapping.
     *
     * @param map state map keyed by package name.
     * @return void.
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     */
    public void updateStates(Map<String, DownloadState> map) {
        if (map == null) {
            throw new IllegalArgumentException("State map must not be null");
        }
        this.states = map;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<AppModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppModel oldItem, @NonNull AppModel newItem) {
                    return oldItem.packageName.equals(newItem.packageName);
                }

                @Override
                public boolean areContentsTheSame(@NonNull AppModel oldItem, @NonNull AppModel newItem) {
                    return oldItem.equals(newItem);
                }
            };

    /**
     * View holder storing references to UI components for a single app item.
     */
    public static class AppViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, version;
        Button openBtn, actionBtn;
        ProgressBar progressBar;

        /**
         * Creates a view holder for app list items.
         *
         * @param itemView inflated item view.
         * @throws IllegalArgumentException if {@code itemView} is {@code null}.
         */
        public AppViewHolder(View itemView) {
            super(itemView);
            if (itemView == null) {
                throw new IllegalArgumentException("Item view must not be null");
            }
            icon = itemView.findViewById(R.id.imageViewIcon);
            name = itemView.findViewById(R.id.textViewName);
            version = itemView.findViewById(R.id.textViewVersion);
            openBtn = itemView.findViewById(R.id.buttonOpen);
            actionBtn = itemView.findViewById(R.id.buttonUpdate);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    /**
     * Inflates new view holders.
     *
     * @param parent recycler view parent.
     * @param viewType unused view type.
     * @return newly created view holder.
     * @throws IllegalArgumentException if {@code parent} is {@code null}.
     */
    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent must not be null");
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    /**
     * Binds app data and actions to the provided holder.
     *
     * @param holder target view holder.
     * @param position item position.
     * @return void.
     * @throws IllegalArgumentException if {@code holder} is {@code null}.
     */
    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {
        if (holder == null) {
            throw new IllegalArgumentException("Holder must not be null");
        }
        AppModel app = getItem(position);
        Context context = holder.itemView.getContext();
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.name);
        holder.version.setText("Version: " + app.version);

        holder.openBtn.setOnClickListener(v -> {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (launch != null) context.startActivity(launch);
        });

        DownloadState state = states.get(app.packageName);
        if (state == null) state = DownloadState.IDLE;

        holder.progressBar.setVisibility(View.GONE);
        holder.actionBtn.setVisibility(View.VISIBLE);
        holder.actionBtn.setEnabled(true);

        UpdateInfo update = app.updateInfo;
        if (update == null && state == DownloadState.IDLE) {
            holder.actionBtn.setVisibility(View.GONE);
            return;
        }

        switch (state) {
            case IDLE:
                holder.actionBtn.setText(update != null ? R.string.update : R.string.download);
                holder.actionBtn.setOnClickListener(v -> {
                    if (update == null || update.apk_url == null) return;
                    DownloadRepository.DownloadEntry entry = new DownloadRepository.DownloadEntry();
                    entry.packageName = app.packageName;
                    entry.versionCode = update.versionCode;
                    entry.url = update.apk_url;
                    entry.sha256 = update.sha256;
                    entry.sizeBytes = update.sizeBytes;
                    entry.signatureDigest = update.signatureDigest;
                    downloadRepository.startDownload(entry);
                });
                break;
            case DOWNLOADING:
                holder.actionBtn.setText(R.string.cancel);
                holder.actionBtn.setOnClickListener(v -> downloadRepository.cancel(app.packageName));
                holder.progressBar.setVisibility(View.VISIBLE);
                break;
            case VERIFYING:
                holder.actionBtn.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.VISIBLE);
                break;
            case INSTALL_PROMPT:
            case INSTALLED:
                holder.actionBtn.setVisibility(View.GONE);
                break;
            case FAILED:
                holder.actionBtn.setText(R.string.retry);
                holder.actionBtn.setOnClickListener(v -> {
                    if (update == null || update.apk_url == null) return;
                    DownloadRepository.DownloadEntry entry = new DownloadRepository.DownloadEntry();
                    entry.packageName = app.packageName;
                    entry.versionCode = update.versionCode;
                    entry.url = update.apk_url;
                    entry.sha256 = update.sha256;
                    entry.sizeBytes = update.sizeBytes;
                    entry.signatureDigest = update.signatureDigest;
                    downloadRepository.startDownload(entry);
                });
                break;
        }
    }

    /**
     * Provides stable IDs derived from the package name hash.
     *
     * @param position adapter position.
     * @return stable ID for the item.
     * @throws IndexOutOfBoundsException if the position is invalid.
     */
    @Override
    public long getItemId(int position) {
        AppModel item = getItem(position);
        return item.packageName.hashCode();
    }
}
