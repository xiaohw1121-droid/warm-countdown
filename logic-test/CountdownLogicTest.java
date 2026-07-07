import com.warmcountdown.core.CountdownMath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class CountdownLogicTest {
    public static void main(String[] args) {
        assertEquals(3, CountdownMath.daysUntil(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 7)), "daysUntil future");
        assertEquals(0, CountdownMath.daysUntil(LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 7)), "daysUntil today");
        assertEquals(-6, CountdownMath.daysUntil(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7)), "daysUntil past");

        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 10, 0);
        assertEquals(
            LocalDateTime.of(2026, 7, 8, 9, 0),
            CountdownMath.nextReminder(LocalDateTime.of(2026, 7, 7, 9, 0), "daily", now),
            "daily reminder"
        );
        assertEquals(
            LocalDateTime.of(2026, 7, 8, 8, 30),
            CountdownMath.nextReminder(LocalDateTime.of(2026, 7, 1, 8, 30), "weekly", now),
            "weekly reminder"
        );
        assertEquals(
            LocalDateTime.of(2026, 7, 30, 18, 0),
            CountdownMath.nextReminder(LocalDateTime.of(2026, 6, 30, 18, 0), "monthly", now),
            "monthly reminder"
        );

        List<CountdownMath.TimelineNode> nodes = Arrays.asList(
            new CountdownMath.TimelineNode("custom-a", LocalDate.of(2026, 7, 11), "交付日", "最后确认", false),
            new CountdownMath.TimelineNode("custom-b", LocalDate.of(2026, 7, 5), "纪念", "", false)
        );
        List<CountdownMath.TimelineNode> timeline = CountdownMath.timeline(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 11), nodes);
        assertEquals(4, timeline.size(), "timeline size");
        assertEquals(LocalDate.of(2026, 7, 1), timeline.get(0).date, "timeline start");
        assertEquals(LocalDate.of(2026, 7, 11), timeline.get(3).date, "timeline custom end");
        assertEquals("交付日", timeline.get(3).title, "custom node wins same date");

        List<CountdownMath.Cell> cells = CountdownMath.collageCells(5, 1000, 800);
        assertEquals(5, cells.size(), "cell count");
        for (CountdownMath.Cell cell : cells) {
            assertTrue(cell.width > 0 && cell.height > 0, "cell positive");
            assertTrue(cell.x + cell.width <= 1000, "cell inside width");
            assertTrue(cell.y + cell.height <= 800, "cell inside height");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
