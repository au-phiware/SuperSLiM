package com.tonicartos.superslim;

import static net.java.quickcheck.generator.CombinedGenerators.arrays;
import static net.java.quickcheck.generator.PrimitiveGenerators.printableStrings;
import static net.java.quickcheck.generator.PrimitiveGenerators.integers;
import static java.lang.String.format;
import net.java.quickcheck.Generator;

public class SectionGenerator implements Generator<SectionGenerator.Section> {
    static Generator<String[]> items = arrays(printableStrings(), String.class);
    static Generator<Section[]> subsections = arrays(SectionGenerator.sections(), integers(-10, 5), Section.class);
    public static class Section {
        Section[] subsections;
        String[] items;
        transient int[] path;

        public String toString() {
            StringBuilder out = new StringBuilder(format("([%d]", items.length));
            for (Section subsection : subsections)
                out.append(" " + subsection.toString());
            out.append(")");
            return out.toString();
        }
    }

    public static SectionGenerator sections() {
        return new SectionGenerator();
    }

    public Section next() {
        Section section = new Section();
        section.items = items.next();
        section.subsections = subsections.next();
        return section;
    }
}
