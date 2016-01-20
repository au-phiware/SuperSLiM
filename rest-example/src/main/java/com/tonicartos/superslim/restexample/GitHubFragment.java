package com.tonicartos.superslim.restexample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.ItemDecorator;
import com.tonicartos.superslim.LayoutHelper;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

import java.util.Random;

/**
 * Fragment that displays a list of country names.
 */
public class GitHubFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        ItemDecorator decor = new ItemDecorator.Builder(getActivity())
                .setDrawableBelow(R.drawable.divider_horizontal, ItemDecorator.INTERNAL)
                .decorateSlm(LinearSLM.ID)
                .decorateSlm(GridSLM.ID)
                .build();

        mRecyclerView.addItemDecoration(decor);

        LayoutManager layoutManager = new LayoutManager.Builder(getActivity())
                .build();
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setAdapter(new GitHubAdapter(getActivity()));
    }
}
