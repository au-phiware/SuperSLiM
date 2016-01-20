package com.tonicartos.superslim.restexample;

import static android.util.Log.d;
import static java.lang.String.format;
import static java.util.Collections.binarySearch;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;
import com.tonicartos.superslim.SectionAdapter;
import com.tonicartos.superslim.restexample.GitHubClient.UserBatch;
import com.tonicartos.superslim.restexample.GitHubClient.RepoBatch;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This adapter will present a potentially endless list of GitHub users.
 * Users are batched into groups of 30 (top level sections have 30 users
 * each); this is because GitHub's API is paginated, 30 items to a page.
 * Each user is itself a section, with one item and subsections for each
 * repository, which are also batched into lots of 30.
 */
public class GitHubAdapter extends SectionAdapter<TextViewHolder> {
    private static String TAG = "GitHubAdapter";

    public static final int TYPE_USER    = 0x01;
    public static final int TYPE_REPO    = 0x02;
    public static final int TYPE_COMMIT  = 0x03;
    public static final int TYPE_CONTRIB = 0x04;

    private final Context mContext;

    private GitHubClient github = new GitHubClient(this);
    private int userBatchCount = 0; // Begin with just one.
    private int totalItemCount = 0;
    // Map batch position to `since` index
    private List<Long> userBatchNumber = new ArrayList<Long>(5 * UserBatch.COUNT);
    private SparseIntArray repoCounts = new SparseIntArray(10 * RepoBatch.COUNT);

    public GitHubAdapter(Context context) {
        mContext = context;
        github.getUserBatch(0L);
    }

    @Override
    public int getItemCount() {
        return totalItemCount;
    }

    @Override
    public int getItemCount(int... path) {
        switch(path.length) {
            case 2:       // User header
            case 5:       // Repo header
                return 1;
            default: return 0;
        }
    }

    @Override
    public int getSectionCount(int... path) {
        UserBatch userBatch;
        RepoBatch repoBatch;
        JSONObject user;
        try {
            switch(path.length) {
                case 0:
                    return userBatchCount;
                case 1:       // User batches
                    return UserBatch.COUNT; // TODO: truncate the last one
                case 3:       // Repo batches
                    if (path[0] < userBatchNumber.size()) {
                        long i = userBatchNumber.get(path[0]);
                        userBatch = github.getUserBatch(i);
                        if (userBatch != null) {
                            user = userBatch.getUser(path[1]);
                            repoBatch = github.getRepoBatch(user.getLong("id"), path[2] + 1);
                            if (repoBatch != null) {
                                return repoBatch.repos.length();
                            }
                        }
                    }
                    break;
                case 2:       // Number of repo batches per user
                    if (path[0] < userBatchNumber.size()) {
                        long i = userBatchNumber.get(path[0]);
                        userBatch = github.getUserBatch(i);
                        if (userBatch != null) {
                            user = userBatch.getUser(path[1]);
                            repoBatch = github.getRepoBatch(user.getLong("id"), path[2] + 1);
                            if (repoBatch != null) {
                                return repoBatch.lastPageNumber;
                            }
                        }
                    }
                    break;
            }
        } catch(JSONException ignored) {}
        return 0;
    }

    @Override
    public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.text_line_item, parent, false);
        return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TextViewHolder holder, int... path) {
        UserBatch userBatch;
        RepoBatch repoBatch;
        JSONObject user;
        JSONObject repo;
        try {
            switch(path.length) {
                case 3:       // User header
                    userBatch = github.getUserBatch(userBatchNumber.get(path[0]));
                    user = userBatch.getUser(path[1]);
                    holder.bindText(user.getString("name"));
                    break;

                case 6:       // Repo header
                    userBatch = github.getUserBatch(userBatchNumber.get(path[0]));
                    user = userBatch.getUser(path[1]);
                    repoBatch = github.getRepoBatch(user.getLong("id"), path[2] + 1);
                    repo = repoBatch.getRepo(path[3]);
                    holder.bindText(repo.getString("full_name"));
                    break;
            }
        } catch(JSONException ignored) {}
    }

    @Override
    public int getItemViewType(int... path) {
        return 0;
    }

    public final void notifyUserChanged(UserBatch batch) {
        d(TAG, format("Ente notifyUserChanged(%s)", batch));
        int position = binarySearch(userBatchNumber, batch.batchNumber);
        userBatchNumber.set(position + 1, batch.nextBatchNumber);
        notifyItemChanged(new int[] {position});
        d(TAG, format("Exit notifyUserChanged(%s)", batch));
    }

    public final void notifyRepoChanged(RepoBatch batch) {
        int position = binarySearch(userBatchNumber, batch.userId);
        int userIndex = UserBatch.COUNT;
        if (position < 0) {
            position = -1 * (position + 2);
        }
        notifyItemChanged(new int[] {position});
    }
}
