// 2015-11-21: Modified by Corin Lawson <corin@phiware.com.au> (@au-phiware)
package com.tonicartos.superslim;

import static android.util.Log.d;
import static java.lang.String.format;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

import android.support.v7.widget.RecyclerView;
import java.util.Arrays;

public abstract class SectionAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private static boolean DEBUG = true;
    private static String TAG = "SectionAdapter";
    public static int NO_POSITION = -1;

    Node root = new Node();
    Observer observer = new Observer();

    public SectionAdapter() {
        super();
        registerAdapterDataObserver(observer);
    }

    public void setHasStableIds (boolean hasStableIds) {
        unregisterAdapterDataObserver(observer);
        super.setHasStableIds(hasStableIds);
        registerAdapterDataObserver(observer);
    }

    class Observer extends RecyclerView.AdapterDataObserver {
        public void onChanged() {
            if (DEBUG) d(TAG, "onChanged");
            root.data = null;
            root.children = null;
            root.totalItemCount = -1;
            root.itemCount = -1;
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            //TODO: find closest node to positionStart and clear it and everything afterwards
            onChanged();
        }

        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            onItemRangeChanged(positionStart, itemCount);
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            // Unable to tell if itemCount spans multiple sections.
            onItemRangeChanged(positionStart, itemCount);
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            //TODO: find closest node to earliest position and invalidate everything inbetween
            onChanged();
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            //TODO: find closest node(s) to positionStart and invalidate it and then adjust the counts
            onChanged();
        }
    }

    class Cursor {
        Node node; int[] path; int position;

        Cursor(Node n, int[] p, int po) {
            node = n;
            path = p;
            position = po;
        }

        SectionData getSectionData() {
            if (node.data == null) {
                int i = path[path.length - 1];
                int start = position - i;
                int end = start + node.getTotalItemCount(copyOf(path, path.length - 1)) - 1;
                node.data = new SectionData(start, end);
            }
            return node.data;
        }

        int getItemCount() {
            if (node.itemCount < 0) {
                node.itemCount = SectionAdapter.this.getItemCount(path);
            }
            return node.itemCount;
        }

        int getTotalItemCount() {
            return node.getTotalItemCount(path);
        }

        void descend(int index) {
            if (DEBUG) d(TAG, format("Enter Cursor(%s, %d).descend(%d)", Arrays.toString(path), position, index));
            int last = path.length;
            if (node.children == null) {
                int count = getSectionCount(copyOf(path, last));
                node.children = new SectionAdapter.Node[count];
            }
            position += getItemCount();
            int[] childPath = copyOf(path, last + 1);
            for (int i = 0; i < index; i++) {
                childPath[last] = i;
                if (node.children[i] == null) {
                    node.children[i] = new Node();
                }
                position += node.children[i].getTotalItemCount(childPath);
            }
            if (node.children[index] == null) {
                node.children[index] = new Node();
            }
            node = node.children[index];
            path = childPath;
            path[path.length - 1] = index;
            if (DEBUG) d(TAG, format("Exit  Cursor(%s, %d).descend(%d)", Arrays.toString(path), position, index));
        }
    }

    class Node {
        SectionData data;
        int totalItemCount = -1;
        int itemCount = -1;
        Node[] children;

        int getTotalItemCount(int... path) {
            if (DEBUG) d(TAG, format("Enter getTotalItemCount(%s)", Arrays.toString(path)));
            if (totalItemCount < 0) {
                if (itemCount < 0) {
                    itemCount = getItemCount(copyOf(path, path.length));
                }
                totalItemCount = itemCount;
                int count;
                if (children == null) {
                    count = getSectionCount(copyOf(path, path.length));
                    children = new SectionAdapter.Node[count];
                } else {
                    count = children.length;
                }
                int last = path.length;
                int[] childPath = copyOf(path, last + 1);
                for (int i = 0; i < count; i++) {
                    childPath[last] = i;
                    if (children[i] == null) {
                        children[i] = new Node();
                    }
                    totalItemCount += children[i].getTotalItemCount(childPath);
                }
            }
            if (DEBUG) d(TAG, format("Exit  getTotalItemCount(%s) -> %d", Arrays.toString(path), totalItemCount));
            return totalItemCount;
        }

        Cursor getPath(int position, int... path) {
            if (DEBUG) d(TAG, format("Enter getPath(%d, %s)", position, Arrays.toString(path)));
            if (position < 0) {
                if (DEBUG) d(TAG, format("Exit  getPath(%d, %s) -> null", position, Arrays.toString(path)));
                return null;
            }
            int last = path.length;
            int[] childPath = copyOf(path, last + 1);
            if (itemCount < 0) {
                itemCount = getItemCount(copyOf(path, last));
            }
            if (itemCount > 0 && position < itemCount) {
                childPath[last] = position;
                if (DEBUG) d(TAG, format("Exit  getPath(%d, %s) -> Cursor(%s, %d)", position, Arrays.toString(path), Arrays.toString(childPath), position));
                return new Cursor(this, childPath, position);
            }
            if (totalItemCount >= 0 && position >= totalItemCount) {
                if (DEBUG) d(TAG, format("Exit  getPath(%d, %s) -> null", position, Arrays.toString(path)));
                return null;
            }
            int total = itemCount;
            int childCount;
            if (children == null) {
                childCount = getSectionCount(copyOf(path, last));
                children = new SectionAdapter.Node[childCount];
            } else {
                childCount = children.length;
            }
            Cursor cursor;
            for (int i = 0; i < childCount; i++) {
                childPath[last] = i;
                if (children[i] == null) {
                    children[i] = new Node();
                }
                cursor = children[i].getPath(position - total, copyOf(childPath, last + 1));
                if (cursor != null) {
                    cursor.position += total;
                    if (DEBUG) d(TAG, format("Exit  getPath(%d, %s) -> Cursor(%s, %d)", position, Arrays.toString(path), Arrays.toString(cursor.path), cursor.position));
                    return cursor;
                }
                total += children[i].totalItemCount;
            }
            totalItemCount = total;
            if (DEBUG) d(TAG, format("Exit  getPath(%d, %s) -> null", position, Arrays.toString(path)));
            return null;
        }
    }

    public SectionData getSectionData(int position) {
        Cursor cursor = root.getPath(position);
        if (cursor != null)
            return cursor.getSectionData();
        return null;
    }

    public int[] getPath(int position) {
        Cursor cursor = root.getPath(position);
        if (cursor != null)
            return cursor.path;
        return null;
    }

    public void onBindViewHolder(VH holder, int position) {
        Cursor cursor = root.getPath(position);
        if (cursor != null)
            onBindViewHolder(holder, cursor.path);
    }

    public abstract int getSectionCount(int... indexPath);
    public abstract int getItemCount(int... indexPath);
    public abstract int getItemViewType(int... indexPath);
    public abstract void onBindViewHolder(VH holder, int... indexPathToItem);

    public final void notifyItemChanged(int... path) {
        if (path.length > 0) {
            super.notifyItemChanged(getPosition(path));
        }
    }

    public int getPosition(int... indexPath) {
        if (DEBUG) d(TAG, format("Enter getPosition(%s)", Arrays.toString(indexPath)));
        int position = 0;
        if (indexPath.length > 0) {
            int i = 0;
            Cursor cursor = new Cursor(root, new int[0], 0);
            while (i < indexPath.length - 1) {
                cursor.descend(indexPath[i++]);
            }
            position = cursor.position + indexPath[i];
        }
        if (DEBUG) d(TAG, format("Exit  getPosition(%s) -> %d", Arrays.toString(indexPath), position));
        return position;
    }

    /**
     * Default implementation walks the section graph, collecting the values that are returned from getItemCount(int[])
     */
    public int getItemCount() {
        return root.getTotalItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = root.getPath(position);
        if (cursor != null)
            return getItemViewType(cursor.path);
        return -1;
    }
}
