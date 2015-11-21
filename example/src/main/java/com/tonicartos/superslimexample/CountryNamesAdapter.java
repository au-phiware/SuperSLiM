// 2015-11-22: Modified by Corin Lawson <corin@phiware.com.au> (@au-phiware)
package com.tonicartos.superslimexample;

import static java.util.Arrays.copyOfRange;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;
import com.tonicartos.superslim.SectionAdapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CountryNamesAdapter extends SectionAdapter<CountryViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0x01;

    private static final int VIEW_TYPE_CONTENT = 0x00;

    private static final int LINEAR = 0;

    private int mHeaderDisplay;

    private boolean mMarginsFixed;

    private final Context mContext;

    private int sectionCount = 0;
    private String[][] countryNames = new String[26][];

    public CountryNamesAdapter(Context context, int headerMode) {
        mContext = context;

        final String[] _countryNames = context.getResources().getStringArray(R.array.country_names);
        mHeaderDisplay = headerMode;

        String lastHeaderText = "";
        int sectionManager = -1;
        int sectionFirstPosition = 0;
        int i;
        for (i = 0; i < _countryNames.length; i++) {
            String headerText = _countryNames[i].substring(0, 1);
            if (!TextUtils.equals(lastHeaderText, headerText)) {
                if (lastHeaderText.length() > 0) {
                    countryNames[sectionCount++] = copyOfRange(_countryNames, sectionFirstPosition, i);
                }
                sectionFirstPosition = i;
                lastHeaderText = headerText;
            }
        }
        countryNames[sectionCount++] = copyOfRange(_countryNames, sectionFirstPosition, i);
    }

    @Override
    public int getItemCount(int... path) {
        if (path.length == 1)
            return countryNames[path[0]].length + 1;
        return 0;
    }

    @Override
    public int getSectionCount(int... path) {
        if (path.length == 0)
            return sectionCount;
        return 0;
    }

    @Override
    public CountryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_HEADER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_item, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.text_line_item, parent, false);
        }
        return new CountryViewHolder(view);
    }

    public String itemToString(int... path) {
        if (path.length == 2) {
            if (path[1] == 0) {
                return countryNames[path[0]][0].substring(0, 1);
            } else {
                return countryNames[path[0]][path[1] - 1];
            }
        }
        return "";
    }

    public String itemToString(int position) {
        return itemToString(getPath(position));
    }

    @Override
    public void onBindViewHolder(CountryViewHolder holder, int... path) {
        final View itemView = holder.itemView;

        if (path[1] == 0) {
            holder.bindItem(countryNames[path[0]][0].substring(0, 1));
        } else {
            holder.bindItem(countryNames[path[0]][path[1] - 1]);
        }

        final GridSLM.LayoutParams lp = new GridSLM.LayoutParams(
                itemView.getLayoutParams());
        // Overrides xml attrs, could use different layouts too.
        if (path[1] == 0) {
            lp.headerDisplay = mHeaderDisplay;
            if (lp.isHeaderInline() || (mMarginsFixed && !lp.isHeaderOverlay())) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        }

        if (path[1] == 0) {
            lp.setSlm(LinearSLM.ID);
            lp.marginEnd = mMarginsFixed ? mContext.getResources()
                    .getDimensionPixelSize(R.dimen.default_section_marginEnd)
                    : LayoutManager.LayoutParams.MARGIN_AUTO;
            lp.marginStart = mMarginsFixed ? mContext.getResources()
                    .getDimensionPixelSize(R.dimen.default_section_marginStart)
                    : LayoutManager.LayoutParams.MARGIN_AUTO;
            lp.setColumnWidth(mContext.getResources().getDimensionPixelSize(R.dimen.grid_column_width));
        }

        itemView.setLayoutParams(lp);
    }

    @Override
    public int getItemViewType(int... path) {
        return path[1] == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    public boolean isItemHeader(int position) {
        return getItemViewType(position) == VIEW_TYPE_HEADER;
    }

    public void setHeaderDisplay(int headerDisplay) {
        mHeaderDisplay = headerDisplay;
        notifyHeaderChanges();
    }

    public void setMarginsFixed(boolean marginsFixed) {
        mMarginsFixed = marginsFixed;
        notifyHeaderChanges();
    }

    private void notifyHeaderChanges() {
        for (int i = 0; i < sectionCount; i++) {
            notifyItemChanged(i, 0);
        }
    }
}
