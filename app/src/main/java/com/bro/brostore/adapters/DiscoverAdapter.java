package com.example.appstoredemo.adapters;

import android.content.Context;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.appstoredemo.R;
import com.example.appstoredemo.models.DownloadState;
import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.models.UpdateInfo;
import com.example.appstoredemo.repositories.DownloadRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Displays apps for discovery. Uses DiffUtil for smooth updates and reacts to
 * per item download states exposed by {@link DownloadRepository}.
 */
public class DiscoverAdapter extends ListAdapter<RemoteAppInfo, DiscoverAdapter.ViewHolder> {
    private final DownloadRepository downloadRepository;
    private Map<String, DownloadState> states = new HashMap<>();

    /**
     * Creates the adapter with a download repository for actions.
     *
     * @param repo repository used to initiate download operations.
     * @throws IllegalArgumentException if {@code repo} is {@code null}.
     */
    public DiscoverAdapter(DownloadRepository repo) {
        super(DIFF_CALLBACK);
        if (repo == null) {
            throw new IllegalArgumentException("DownloadRepository must not be null");
        }
        this.downloadRepository = repo;
        setHasStableIds(true);
    }

    /**
     * Updates the adapter with the latest download state map.
     *
     * @param stateMap state map keyed by package name.
     * @return void.
     * @throws IllegalArgumentException if {@code stateMap} is {@code null}.
     */
    public void updateStates(Map<String, DownloadState> stateMap) {
        if (stateMap == null) {
            throw new IllegalArgumentException("State map must not be null");
        }
        this.states = stateMap;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<RemoteAppInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RemoteAppInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull RemoteAppInfo oldItem, @NonNull RemoteAppInfo newItem) {
                    // Guard against null package names to avoid NPE when comparing items
                    String oldPkg = oldItem.packageName;
                    String newPkg = newItem.packageName;
                    if (oldPkg == null || newPkg == null) {
                        return false;
                    }
                    return oldPkg.equals(newPkg);
                }

                @Override
                public boolean areContentsTheSame(@NonNull RemoteAppInfo oldItem, @NonNull RemoteAppInfo newItem) {
                    return oldItem.equals(newItem);
                }
            };

    /**
     * View holder storing references for the discover list item views.
     */
    public static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, version;
        Button actionBtn;
        ProgressBar progressBar;

        /**
         * Creates a view holder for discover list items.
         *
         * @param itemView inflated item view.
         * @throws IllegalArgumentException if {@code itemView} is {@code null}.
         */
        public ViewHolder(View itemView) {
            super(itemView);
            if (itemView == null) {
                throw new IllegalArgumentException("Item view must not be null");
            }
            icon = itemView.findViewById(R.id.imageViewIconOpt);
            name = itemView.findViewById(R.id.textViewNameOpt);
            version = itemView.findViewById(R.id.textViewVersionOpt);
            actionBtn = itemView.findViewById(R.id.buttonDownload);
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent must not be null");
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discover_app, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds remote app data to the provided holder.
     *
     * @param holder target view holder.
     * @param position item position.
     * @return void.
     * @throws IllegalArgumentException if {@code holder} is {@code null}.
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder == null) {
            throw new IllegalArgumentException("Holder must not be null");
        }
        RemoteAppInfo app = getItem(position);
        Context context = holder.itemView.getContext();

        holder.icon.setImageResource(R.mipmap.basic_app_icon);
        holder.name.setText(app.name);
        UpdateInfo latest = app.getLatest();
        String versionName = latest != null && latest.versionName != null ? latest.versionName : "N/A";
        holder.version.setText("Version: " + versionName);

        DownloadState state = states.get(app.packageName);
        if (state == null) state = DownloadState.IDLE;

        holder.progressBar.setVisibility(View.GONE);
        holder.actionBtn.setVisibility(View.VISIBLE);
        holder.actionBtn.setEnabled(true);

        switch (state) {
            case IDLE:
            case FAILED:
                holder.actionBtn.setText(state == DownloadState.FAILED ? R.string.retry : R.string.download);
                holder.actionBtn.setOnClickListener(v -> {
                    if (latest == null || latest.apk_url == null) return;
                    DownloadRepository.DownloadEntry entry = new DownloadRepository.DownloadEntry();
                    entry.packageName = app.packageName;
                    entry.versionCode = latest.versionCode;
                    entry.url = latest.apk_url;
                    entry.sha256 = latest.sha256;
                    entry.sizeBytes = latest.sizeBytes;
                    entry.signatureDigest = latest.signatureDigest;
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
        }
    }

    /**
     * Provides stable IDs derived from the package name hash.
     *
     * @param position adapter position.
     * @return stable ID for the item or {@link RecyclerView#NO_ID} when unavailable.
     * @throws IndexOutOfBoundsException if the position is invalid.
     */
    @Override
    public long getItemId(int position) {
        RemoteAppInfo item = getItem(position);
        if (item == null || item.packageName == null) {
            return RecyclerView.NO_ID;
        }
        return item.packageName.hashCode();
    }
}
