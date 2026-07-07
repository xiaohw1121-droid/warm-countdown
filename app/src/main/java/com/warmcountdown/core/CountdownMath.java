package com.warmcountdown.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CountdownMath {
    private CountdownMath() {}

    public static int daysUntil(LocalDate target, LocalDate from) {
        return (int) ChronoUnit.DAYS.between(from, target);
    }

    public static LocalDateTime nextReminder(LocalDateTime first, String repeat, LocalDateTime now) {
        if (first == null || "none".equals(repeat)) return null;
        LocalDateTime next = first;
        while (!next.isAfter(now)) {
            next = addRepeat(next, repeat);
        }
        return next;
    }

    public static LocalDateTime addRepeat(LocalDateTime dateTime, String repeat) {
        if ("daily".equals(repeat)) return dateTime.plusDays(1);
        if ("weekly".equals(repeat)) return dateTime.plusWeeks(1);
        if ("monthly".equals(repeat)) return dateTime.plusMonths(1);
        return dateTime;
    }

    public static List<TimelineNode> timeline(LocalDate start, LocalDate end, List<TimelineNode> customNodes) {
        List<TimelineNode> custom = customNodes == null ? new ArrayList<>() : new ArrayList<>(customNodes);
        Set<LocalDate> customDates = new HashSet<>();
        for (TimelineNode node : custom) customDates.add(node.date);

        List<TimelineNode> generated = new ArrayList<>();
        generated.add(new TimelineNode("auto-start", start, "倒计时开始", "从这一天开始，把重要的事一点点安放好。", true));
        long total = ChronoUnit.DAYS.between(start, end);
        if (total >= 10) {
            generated.add(new TimelineNode("auto-half", start.plusDays(Math.round(total / 2.0)), "倒计时过半", "回头看一眼已经完成的记录，再继续向前。", true));
        }
        if (!start.equals(end)) {
            generated.add(new TimelineNode("auto-end", end, "最后一天", "把想留下的画面和文字都收好。", true));
        }

        List<TimelineNode> result = new ArrayList<>();
        for (TimelineNode node : generated) {
            if (!customDates.contains(node.date)) result.add(node);
        }
        result.addAll(custom);
        result.sort(Comparator.comparing((TimelineNode node) -> node.date).thenComparing(node -> node.title));
        return result;
    }

    public static List<Cell> collageCells(int count, int width, int height) {
        List<Cell> cells = new ArrayList<>();
        if (count <= 0) return cells;
        int gap = Math.max(12, Math.round(width * 0.018f));
        int columns = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / columns);
        int cellWidth = (width - gap * (columns + 1)) / columns;
        int cellHeight = (height - gap * (rows + 1)) / rows;
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            cells.add(new Cell(gap + column * (cellWidth + gap), gap + row * (cellHeight + gap), cellWidth, cellHeight));
        }
        return cells;
    }

    public static final class TimelineNode {
        public final String id;
        public final LocalDate date;
        public final String title;
        public final String note;
        public final boolean auto;

        public TimelineNode(String id, LocalDate date, String title, String note, boolean auto) {
            this.id = id;
            this.date = date;
            this.title = title;
            this.note = note;
            this.auto = auto;
        }
    }

    public static final class Cell {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Cell(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
