package com.tonicartos.superslim;

import static net.java.quickcheck.generator.CombinedGenerators.arrays;
import static org.junit.Assert.*;
import static java.lang.String.format;
import static java.util.Arrays.copyOf;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.tonicartos.superslim.SectionGenerator.Section;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import net.java.quickcheck.Generator;
import net.java.quickcheck.junit.ForAll;
import net.java.quickcheck.junit4.QuickCheckRunner;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(QuickCheckRunner.class)
public class SectionAdapterTest {
    static final int HEADER_VIEW_TYPE = 0;
    static final int   ITEM_VIEW_TYPE = 1;

    static class TestViewHolder extends RecyclerView.ViewHolder {
        String item;
        public TestViewHolder(View v) {
            super(v);
        }
    }

    class TestTreeBackedSectionAdapter extends SectionAdapter<TestViewHolder> {
        Section data;

        public TestTreeBackedSectionAdapter(Section data) {
            super();
            this.data = data;
        }

        public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {}

        public int getSectionCount(int... sectionIndexPath) {
            Section section = data;
            for (int i : sectionIndexPath) {
                section = section.subsections[i];
            }
            return section.subsections.length;
        }

        public int getItemCount(int... sectionIndexPath) {
            Section section = data;
            for (int i : sectionIndexPath) {
                section = section.subsections[i];
            }
            return section.items.length;
        }

        public int getItemViewType(int... itemIndexPath) {
            if (itemIndexPath[itemIndexPath.length - 1] == 0) {
                return HEADER_VIEW_TYPE;
            }
            return ITEM_VIEW_TYPE;
        }

        public void onBindViewHolder(TestViewHolder holder, int... itemIndexPath) {
            Section section = data;
            int i = 0;
            while (i < itemIndexPath.length - 1) {
                section = section.subsections[itemIndexPath[i++]];
            }

            assertTrue("item index is within bounds",
                itemIndexPath[i] < section.items.length);

            holder.item = section.items[itemIndexPath[i]];
        }

        public TestViewHolder onCreateViewHolder(ViewGroup view, int viewType) {
            return new TestViewHolder(view);
        }
    }

    int countItems(Section root) {
        int count = 0;
        Section section = root;
        Deque<Section> q = new ArrayDeque<Section>();
        q.push(section);
        while (!q.isEmpty()) {
            section = q.pop();
            count += section.items.length;
            for (int i = section.subsections.length; i > 0;)
                q.push(section.subsections[--i]);
        }
        return count;
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionGenerator.sections")
    public void getItemCountWithTreeBackedSectionAdapter(Section data) {
        TestTreeBackedSectionAdapter adapter = new TestTreeBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        assertEquals("count",
                count, adapter.getItemCount());
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionGenerator.sections")
    public void onBindViewHolderWithTreeBackedSectionAdapter(Section data) {
        TestTreeBackedSectionAdapter adapter = new TestTreeBackedSectionAdapter(data);

        TestViewHolder holder = new TestViewHolder(new View(null));
        int count = countItems(adapter.data);
        int position = 0;
        Section section = adapter.data;
        Deque<Section> q = new ArrayDeque<Section>();
        q.push(section);
        while (!q.isEmpty()) {
            section = q.pop();
            for (String item : section.items) {
                adapter.onBindViewHolder(holder, position);
                assertEquals("item",
                        item, holder.item);
                position++;
            }
            for (int i = section.subsections.length; i > 0;)
                q.push(section.subsections[--i]);
        }
        assertEquals("count",
                count, position);
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionGenerator.sections")
    public void getSectionDataWithTreeBackedSectionAdapter(Section data) {
        TestTreeBackedSectionAdapter adapter = new TestTreeBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0;
        int firstPosition = 0, lastPosition = 0;
        Section section = adapter.data;
        Deque<Section> q = new ArrayDeque<Section>();
        q.push(section);
        while (!q.isEmpty()) {
            section = q.pop();
            firstPosition = position;
            lastPosition = firstPosition + countItems(section) - 1;
            for (String item : section.items) {
                SectionData sd = adapter.getSectionData(position);
                assertEquals("firstPosition",
                        firstPosition, sd.firstPosition);
                assertEquals("lastPosition",
                        lastPosition, sd.lastPosition);
                position++;
            }
            for (int i = section.subsections.length; i > 0;)
                q.push(section.subsections[--i]);
        }
        assertEquals("count",
                count, position);
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionGenerator.sections")
    public void getPositionWithTreeBackedSectionAdapter(Section data) {
        TestTreeBackedSectionAdapter adapter = new TestTreeBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0, i = 0;
        Section section = adapter.data;
        int[] path = new int[0];
        Deque<Section> q = new ArrayDeque<Section>();
        section.path = path;
        q.push(section);
        while (!q.isEmpty()) {
            section = q.pop();
            i = section.path.length;
            path = copyOf(section.path, i + 1);
            for (path[i] = 0; path[i] < section.items.length; path[i]++) {
                assertEquals(
                        format("tree:<%s> itemIndexPath:<%s>", adapter.data.toString(), Arrays.toString(path)),
                        position, adapter.getPosition(path));
                position++;
            }
            for (int j = section.subsections.length; j > 0;) {
                Section subsection = section.subsections[--j];
                path[i] = j;
                subsection.path = copyOf(path, path.length);
                q.push(subsection);
            }
        }
        assertEquals("count",
                count, position);
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionGenerator.sections")
    public void getPathWithTreeBackedSectionAdapter(Section data) {
        TestTreeBackedSectionAdapter adapter = new TestTreeBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0, i = 0;
        Section section = adapter.data;
        int[] path = new int[0];
        Deque<Section> q = new ArrayDeque<Section>();
        section.path = path;
        q.push(section);
        while (!q.isEmpty()) {
            section = q.pop();
            i = section.path.length;
            path = copyOf(section.path, i + 1);
            for (path[i] = 0; path[i] < section.items.length; path[i]++) {
                SectionAdapter.Cursor cursor = adapter.root.getPath(position);
                assertArrayEquals(
                        format("tree:<%s> itemIndexPath:<%s>", adapter.data.toString(), Arrays.toString(path)),
                        path, cursor.path);
                assertEquals(
                        format("tree:<%s> itemIndexPath:<%s>", adapter.data.toString(), Arrays.toString(path)),
                        position, cursor.position);
                position++;
            }
            for (int j = section.subsections.length; j > 0;) {
                Section subsection = section.subsections[--j];
                path[i] = j;
                subsection.path = copyOf(path, path.length);
                q.push(subsection);
            }
        }
        assertEquals("count",
                count, position);
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionGenerator.sections")
    public void getItemViewTypeWithTreeBackedSectionAdapter(Section data) {
        TestTreeBackedSectionAdapter adapter = new TestTreeBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0;
        int firstPosition = 0, lastPosition = 0;
        Section section = adapter.data;
        Deque<Section> q = new ArrayDeque<Section>();
        q.push(section);
        while (!q.isEmpty() && position < count) {
            section = q.pop();
            assertEquals(
                    format("position:<%d> count:<%d>", position, count),
                    0, adapter.getItemViewType(position));
            if (section.items.length > 0) {
                position++;
                for (int i = 1; i < section.items.length; i++) {
                    assertEquals(
                            format("position:<%d> count:<%d>", position, count),
                            1, adapter.getItemViewType(position));
                    position++;
                }
            }
            for (int i = section.subsections.length; i > 0;)
                q.push(section.subsections[--i]);
        }
        assertEquals("count",
                count, position);
    }

    class TestArrayBackedSectionAdapter extends SectionAdapter<TestViewHolder> {
        String[][][] data;

        public TestArrayBackedSectionAdapter(String[][][] data) {
            super();
            this.data = data;
        }

        public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {}

        public int getSectionCount(int... i) {
            switch(i.length) {
                case 0: return data.length;
                case 1: return data[i[0]].length;
                case 2: return 0;
            }
            fail(format("Adapter should have no reason to query section count at level %d", i.length));
            return 0;
        }

        public int getItemCount(int... i) {
            switch(i.length) {
                case 0:
                case 1: return 0;
                case 2: return data[i[0]][i[1]].length;
            }
            fail(format("Adapter should have no reason to query item count for at %d", i.length));
            return 0;
        }

        public int getItemViewType(int... i) {
            assertEquals(
                    format("Adapter should have no reason to query item view type at level %d", i.length),
                    i.length, 3);
            if (i[2] == 0) {
                return HEADER_VIEW_TYPE;
            }
            return ITEM_VIEW_TYPE;
        }

        public void onBindViewHolder(TestViewHolder holder, int... itemIndexPath) {
            assertEquals(itemIndexPath.length, 3);
        }

        public TestViewHolder onCreateViewHolder(ViewGroup view, int viewType) {
            return new TestViewHolder(view);
        }
    }

    public static Generator<String[][][]> arrayOfArrays() {
        return arrays(arrays(SectionGenerator.items, String[].class), String[][].class);
    }

    int countItems(String[][] section) {
        int count = 0;
        for (String[] subsection : section)
            count += subsection.length;
        return count;
    }

    int countItems(String[][][] data) {
        int count = 0;
        for (String[][] section : data)
            count += countItems(section);
        return count;
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionAdapterTest.arrayOfArrays")
    public void getItemCountWithArrayBackedSectionAdapter(String[][][] data) {
        TestArrayBackedSectionAdapter adapter = new TestArrayBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        assertEquals("count",
                count, adapter.getItemCount());
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionAdapterTest.arrayOfArrays")
    public void getSectionDataWithArrayBackedSectionAdapter(String[][][] data) {
        TestArrayBackedSectionAdapter adapter = new TestArrayBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0;
        int firstPosition = 0, lastPosition = 0;
        for (int i = 0; i < adapter.data.length; i++) {
            for (int j = 0; j < adapter.data[i].length; j++) {
                lastPosition = firstPosition + adapter.data[i][j].length - 1;
                for (int k = 0; k < adapter.data[i][j].length; k++) {
                    SectionData sd = adapter.getSectionData(position);
                    assertEquals(
                            "firstPosition",
                            firstPosition, sd.firstPosition);
                    assertEquals(
                            "lastPosition",
                            lastPosition, sd.lastPosition);
                    position++;
                }
                firstPosition += adapter.data[i][j].length;
            }
        }
        assertEquals("count",
                count, position);
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionAdapterTest.arrayOfArrays")
    public void getPositionWithArrayBackedSectionAdapter(String[][][] data) {
        TestArrayBackedSectionAdapter adapter = new TestArrayBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0;
        for (int i = 0; i < adapter.data.length; i++) {
            for (int j = 0; j < adapter.data[i].length; j++) {
                for (int k = 0; k < adapter.data[i][j].length; k++) {
                    assertEquals(
                            format("itemIndexPath:<[%d,%d,%d]>", i, j, k),
                            position, adapter.getPosition(i, j, k));
                    position++;
                }
            }
        }
        assertEquals("count",
                count, position);
    }

    @ForAll(generatorMethod = "com.tonicartos.superslim.SectionAdapterTest.arrayOfArrays")
    public void getItemViewTypeWithArrayBackedSectionAdapter(String[][][] data) {
        TestArrayBackedSectionAdapter adapter = new TestArrayBackedSectionAdapter(data);

        int count = countItems(adapter.data);
        int position = 0;
        for (String[][] section : adapter.data) {
            for (String[] subsection : section) {
                if (subsection.length > 0) {
                    assertEquals(
                            format("position:<%d> count:<%d>", position, count),
                            0, adapter.getItemViewType(position));
                    position++;
                    for (int i = 1; i < subsection.length; i++) {
                        assertEquals(
                                format("position:<%d> count:<%d>", position, count),
                                1, adapter.getItemViewType(position));
                        position++;
                    }
                }
            }
        }
        assertEquals(
                count, position);
    }
}
