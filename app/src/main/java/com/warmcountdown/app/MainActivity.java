package com.warmcountdown.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import com.warmcountdown.core.CountdownMath;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 44;
    private static final String NEW_RECORD_LABEL = "＋ 新建记录项";
    private final int ink = Color.rgb(63, 53, 44);
    private final int muted = Color.rgb(132, 118, 107);
    private final int rose = Color.rgb(244, 143, 139);
    private final int butter = Color.rgb(255, 213, 111);
    private final int mint = Color.rgb(159, 214, 191);
    private final int sky = Color.rgb(137, 185, 218);
    private final int plum = Color.rgb(184, 155, 204);
    private static final int TAG_DEFAULT = 0;
    private static final int TAG_BLUE = 1;
    private static final int TAG_PLUM = 2;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private AppState state;
    private AppState.Workspace workspace;
    private AppStore store;
    private LinearLayout root;
    private LinearLayout content;
    private TextView titleLabel;
    private TextView daysText;
    private TextView subtitleText;
    private TextView startDateBox;
    private TextView endDateBox;
    private View rangeFill;
    private View rangeRest;
    private TextView todoCountText;
    private TextView recordCountText;
    private TextView nodeCountText;
    private String currentTab = "todos";
    private static final String RECORD_FILTER_ALL = "全部记录项";
    private static final String[] WEEKDAYS_ZH = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private final int[] recordChipBg = {
        Color.argb(38, 244, 143, 139),
        Color.argb(46, 255, 213, 111),
        Color.argb(38, 159, 214, 191),
        Color.argb(38, 137, 185, 218),
        Color.argb(38, 184, 155, 204)
    };
    private final int[] recordChipFg = {
        Color.rgb(197, 92, 86),
        Color.rgb(138, 106, 28),
        Color.rgb(58, 130, 100),
        Color.rgb(91, 142, 184),
        Color.rgb(139, 103, 163)
    };
    private String recordFilter = RECORD_FILTER_ALL;
    private AppState.RecordItem pendingImageItem;
    private Runnable pendingImageRefresh;
    private Bitmap summaryBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AppStore(this);
        workspace = store.load();
        state = workspace.current();
        boolean cleaned = false;
        for (AppState project : workspace.projects) {
            if (project.mergeDuplicateRecords()) cleaned = true;
        }
        if (cleaned) store.save(workspace);
        requestNotificationPermission();
        buildShell();
        render();
    }

    private void buildShell() {
        FrameLayout frame = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(160));
        root.setBackground(screenBackground());
        scroll.addView(root);
        frame.addView(scroll);
        setContentView(frame);

        root.addView(topbar());
        root.addView(heroCard());
        root.addView(quickStrip());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);

        Button fab = new Button(this);
        fab.setText("+");
        fab.setTextSize(28);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setTextColor(Color.WHITE);
        fab.setBackground(roundRect(rose, Color.TRANSPARENT, dp(52)));
        fab.setOnClickListener(v -> {
            if ("todos".equals(currentTab)) showTodoDialog(null);
            else if ("records".equals(currentTab)) showRecordDialog(null);
            else if ("timeline".equals(currentTab)) showNodeDialog(null);
            else currentTab = "records";
        });
        flattenButton(fab);
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.RIGHT | Gravity.BOTTOM);
        fabParams.setMargins(0, 0, dp(20), dp(96));
        frame.addView(fab, fabParams);

        View nav = bottomNav();
        nav.setElevation(dp(8));
        frame.addView(nav, new FrameLayout.LayoutParams(-1, dp(76), Gravity.BOTTOM));
    }

    private View topbar() {
        LinearLayout header = row();
        header.setPadding(0, 0, 0, dp(14));
        TextView mark = text("100", 16, Color.rgb(121, 80, 36), true);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(serifBold());
        mark.setBackground(markShape());
        header.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout brand = form();
        brand.setPadding(dp(10), 0, 0, 0);
        brand.addView(text("暖暖倒计时", 17, ink, true));
        brand.addView(text("把最后的日子认真收藏 · v" + BuildConfig.VERSION_NAME + " / " + BuildConfig.VERSION_CODE, 12, muted, true));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        header.addView(brand, titleParams);
        TextView date = text(LocalDate.now().getMonthValue() + "月" + LocalDate.now().getDayOfMonth() + "日", 12, muted, true);
        date.setPadding(dp(10), dp(8), dp(10), dp(8));
        date.setBackground(roundRect(Color.argb(178, 255, 255, 255), Color.rgb(232, 220, 206), dp(999)));
        header.addView(date);

        Button switcher = new Button(this);
        switcher.setText("⇄");
        switcher.setTextSize(15);
        switcher.setTypeface(Typeface.DEFAULT_BOLD);
        switcher.setTextColor(Color.rgb(121, 80, 36));
        switcher.setPadding(0, 0, 0, 0);
        switcher.setBackground(roundRect(Color.argb(178, 255, 255, 255), Color.rgb(232, 220, 206), dp(999)));
        flattenButton(switcher);
        switcher.setOnClickListener(v -> showProjectSwitcher());
        LinearLayout.LayoutParams switcherParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        switcherParams.setMargins(dp(8), 0, 0, 0);
        header.addView(switcher, switcherParams);
        return header;
    }

    private View heroCard() {
        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(-1, -2);
        wrapParams.setMargins(0, dp(8), 0, dp(10));
        wrap.setLayoutParams(wrapParams);

        LinearLayout card = countdownCard();
        card.setBackground(roundRect(Color.argb(224, 255, 253, 248), Color.rgb(226, 211, 193), dp(8)));
        card.setElevation(dp(3));
        wrap.addView(card, new FrameLayout.LayoutParams(-1, -2));

        View tape = new View(this);
        tape.setBackground(roundRect(Color.argb(158, 255, 211, 110), Color.TRANSPARENT, dp(3)));
        FrameLayout.LayoutParams tapeParams = new FrameLayout.LayoutParams(dp(72), dp(15), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tapeParams.topMargin = -dp(6);
        tape.setLayoutParams(tapeParams);
        tape.setRotation(-2f);
        wrap.addView(tape);

        return wrap;
    }

    private LinearLayout countdownCard() {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        titleLabel = text("距离" + state.title + "还剩", 14, rose, true);
        titleLabel.setPadding(0, 0, 0, dp(2));
        titleLabel.setOnClickListener(v -> showRenameProjectDialog());
        card.addView(titleLabel);

        LinearLayout daysRow = row();
        daysRow.setGravity(Gravity.BOTTOM);
        daysText = text("100", 46, Color.rgb(208, 113, 76), true);
        daysText.setTypeface(serifBold());
        daysRow.addView(daysText);
        TextView day = text("天", 14, muted, true);
        LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(-2, -2);
        dayParams.leftMargin = dp(6);
        dayParams.bottomMargin = dp(6);
        daysRow.addView(day, dayParams);
        subtitleText = text("", 11, muted, false);
        subtitleText.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(0, -2, 1);
        subtitleParams.leftMargin = dp(10);
        subtitleParams.bottomMargin = dp(5);
        daysRow.addView(subtitleText, subtitleParams);
        card.addView(daysRow);

        LinearLayout dateRow = row();
        dateRow.setPadding(0, dp(9), 0, 0);
        startDateBox = compactDate(state.startDate, v -> pickDate(state.startDate, value -> {
            if (parseDate(value).isAfter(parseDate(state.endDate))) {
                toast("开始时间不能晚于结束时间");
                return;
            }
            state.startDate = value;
            save();
            render();
        }));
        dateRow.addView(startDateBox);

        FrameLayout trackWrap = new FrameLayout(this);
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(roundRect(Color.rgb(238, 227, 214), Color.TRANSPARENT, dp(999)));
        track.setClipToOutline(true);
        rangeFill = new View(this);
        rangeFill.setBackground(gradient(rose, butter, dp(999)));
        track.addView(rangeFill, new LinearLayout.LayoutParams(0, dp(4), 0f));
        rangeRest = new View(this);
        track.addView(rangeRest, new LinearLayout.LayoutParams(0, dp(4), 1f));
        trackWrap.addView(track, new FrameLayout.LayoutParams(-1, dp(4), Gravity.CENTER_VERTICAL));
        View dotStart = new View(this);
        dotStart.setBackground(pill(Color.rgb(200, 95, 88)));
        trackWrap.addView(dotStart, new FrameLayout.LayoutParams(dp(7), dp(7), Gravity.CENTER_VERTICAL | Gravity.LEFT));
        View dotEnd = new View(this);
        dotEnd.setBackground(pill(Color.rgb(200, 95, 88)));
        trackWrap.addView(dotEnd, new FrameLayout.LayoutParams(dp(7), dp(7), Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        LinearLayout.LayoutParams trackWrapParams = new LinearLayout.LayoutParams(0, dp(10), 1);
        trackWrapParams.setMargins(dp(7), 0, dp(7), 0);
        dateRow.addView(trackWrap, trackWrapParams);

        endDateBox = compactDate(state.endDate, v -> pickDate(state.endDate, value -> {
            if (parseDate(value).isBefore(parseDate(state.startDate))) {
                toast("结束时间不能早于开始时间");
                return;
            }
            state.endDate = value;
            save();
            render();
        }));
        dateRow.addView(endDateBox);
        card.addView(dateRow);
        return card;
    }

    private TextView compactDate(String value, View.OnClickListener listener) {
        TextView view = text(value, 10, muted, true);
        view.setPadding(dp(2), dp(2), dp(2), dp(2));
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnClickListener(listener);
        return view;
    }

    private View quickStrip() {
        LinearLayout strip = row();
        strip.setPadding(0, dp(14), 0, dp(2));
        View todoBox = quickStat(String.valueOf(state.todos.size()), "待办事项");
        todoCountText = (TextView) todoBox.getTag();
        strip.addView(todoBox, weightParams(1));
        View recordBox = quickStat(String.valueOf(state.records.size()), "每日记录");
        recordCountText = (TextView) recordBox.getTag();
        strip.addView(recordBox, weightParams(1));
        View nodeBox = quickStat(String.valueOf(state.nodes.size()), "关键节点");
        nodeCountText = (TextView) nodeBox.getTag();
        strip.addView(nodeBox, weightParams(1));
        return strip;
    }

    private View quickStat(String value, String label) {
        LinearLayout box = form();
        box.setPadding(dp(11), dp(10), dp(9), dp(10));
        box.setBackground(roundRect(Color.argb(184, 255, 255, 255), Color.rgb(232, 220, 206), dp(8)));
        box.setElevation(dp(1));
        TextView valueView = text(value, 18, ink, true);
        box.addView(valueView);
        box.addView(text(label, 11, muted, true));
        box.setTag(valueView);
        return box;
    }

    private View bottomNav() {
        LinearLayout shell = row();
        shell.setPadding(dp(12), dp(8), dp(12), dp(12));
        shell.setGravity(Gravity.CENTER);
        shell.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout nav = row();
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackground(roundRect(Color.argb(245, 255, 253, 248), Color.rgb(232, 220, 206), dp(22)));
        addBottomTab(nav, "todos", "待办");
        addBottomTab(nav, "records", "记录");
        addBottomTab(nav, "summary", "汇总");
        addBottomTab(nav, "timeline", "节点");
        shell.addView(nav, new LinearLayout.LayoutParams(-1, -1));
        return shell;
    }

    private void addBottomTab(LinearLayout nav, String id, String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(id.equals(currentTab) ? ink : muted);
        button.setBackground(id.equals(currentTab)
            ? gradient(Color.rgb(255, 233, 194), Color.rgb(255, 220, 215), dp(16))
            : roundRect(Color.TRANSPARENT, Color.TRANSPARENT, dp(16)));
        flattenButton(button);
        button.setOnClickListener(v -> {
            currentTab = id;
            buildShell();
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        nav.addView(button, params);
    }

    private View tabBar() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = row();
        tabs.setPadding(0, dp(12), 0, dp(10));
        addTab(tabs, "todos", "待办事项");
        addTab(tabs, "records", "每日记录");
        addTab(tabs, "summary", "进度汇总");
        addTab(tabs, "timeline", "关键节点");
        scroll.addView(tabs);
        return scroll;
    }

    private void addTab(LinearLayout tabs, String id, String label) {
        Button button = softButton(label);
        button.setOnClickListener(v -> {
            currentTab = id;
            render();
        });
        tabs.addView(button, new LinearLayout.LayoutParams(dp(112), dp(48)));
    }

    private void render() {
        LocalDate today = LocalDate.now();
        LocalDate start = parseDate(state.startDate);
        LocalDate anchor = today.isBefore(start) ? start : today;
        int left = CountdownMath.daysUntil(parseDate(state.endDate), anchor);
        titleLabel.setText("距离" + state.title + "还剩");
        daysText.setText(String.valueOf(left));
        int passed = CountdownMath.daysUntil(anchor, start);
        subtitleText.setText(left > 0 ? "已经走过 " + passed + " 天" : left == 0 ? "今天就是最后一天" : "已经过去 " + Math.abs(left) + " 天");
        startDateBox.setText(state.startDate);
        endDateBox.setText(state.endDate);
        int totalSpan = Math.max(1, CountdownMath.daysUntil(parseDate(state.endDate), start));
        float ratio = Math.max(0f, Math.min(1f, passed / (float) totalSpan));
        LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) rangeFill.getLayoutParams();
        fillParams.weight = ratio;
        rangeFill.setLayoutParams(fillParams);
        LinearLayout.LayoutParams restParams = (LinearLayout.LayoutParams) rangeRest.getLayoutParams();
        restParams.weight = 1f - ratio;
        rangeRest.setLayoutParams(restParams);
        todoCountText.setText(String.valueOf(state.todos.size()));
        recordCountText.setText(String.valueOf(state.records.size()));
        nodeCountText.setText(String.valueOf(state.nodes.size()));
        content.removeAllViews();
        if ("todos".equals(currentTab)) renderTodos();
        if ("records".equals(currentTab)) renderRecords();
        if ("summary".equals(currentTab)) renderSummary();
        if ("timeline".equals(currentTab)) renderTimeline();
    }

    private void renderTodos() {
        int undone = 0;
        for (AppState.Todo todo : state.todos) if (!todo.done) undone++;
        String todoSubtitle = state.todos.isEmpty() ? null : (undone > 0 ? "还有 " + undone + " 项未完成" : "全部完成啦");
        sectionTitle("待办事项", todoSubtitle, rose, "添加", v -> showTodoDialog(null));
        List<AppState.Todo> todos = new ArrayList<>(state.todos);
        todos.sort(Comparator.comparing((AppState.Todo todo) -> todo.done).thenComparing(todo -> todo.deadline));
        if (todos.isEmpty()) empty("还没有待办事项");
        for (AppState.Todo todo : todos) {
            content.addView(todoCard(todo));
        }
    }

    private View todoCard(AppState.Todo todo) {
        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(-1, -2);
        wrapParams.setMargins(0, dp(8), 0, dp(10));
        wrap.setLayoutParams(wrapParams);

        boolean urgent = !todo.done && CountdownMath.daysUntil(parseDate(todo.deadline), LocalDate.now()) <= 3;
        int bg = todo.done ? Color.rgb(239, 248, 243) : (urgent ? Color.rgb(255, 244, 235) : Color.rgb(255, 253, 248));
        int accent = todo.done ? mint : (urgent ? rose : butter);

        LinearLayout surface = row();
        surface.setBackground(roundRect(bg, Color.rgb(238, 224, 207), dp(8)));

        View bar = new View(this);
        bar.setBackgroundColor(accent);
        surface.addView(bar, new LinearLayout.LayoutParams(dp(5), -1));

        LinearLayout body = form();
        body.setPadding(dp(14), dp(14), dp(104), dp(14));
        body.addView(text((todo.done ? "✓ " : "") + todo.title, 17, ink, true));
        body.addView(todoMetaLine(todo));
        if (!todo.note.isEmpty()) body.addView(text(todo.note, 14, ink, false));
        surface.addView(body, new LinearLayout.LayoutParams(0, -2, 1));

        wrap.addView(surface, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout cluster = row();
        cluster.setPadding(dp(6), dp(4), dp(6), dp(4));
        cluster.setBackground(roundRect(Color.argb(235, 255, 255, 255), Color.rgb(235, 220, 200), dp(999)));
        Button edit = clusterButton("✎", "edit");
        edit.setOnClickListener(v -> showTodoDialog(todo));
        Button done = clusterButton(todo.done ? "↺" : "✓", "complete");
        done.setOnClickListener(v -> {
            todo.done = !todo.done;
            ReminderScheduler.schedule(this, todo);
            save();
            render();
        });
        Button delete = clusterButton("×", "delete");
        delete.setOnClickListener(v -> {
            ReminderScheduler.cancel(this, todo.id);
            state.todos.remove(todo);
            save();
            render();
        });
        cluster.addView(edit);
        cluster.addView(done);
        cluster.addView(delete);
        FrameLayout.LayoutParams clusterParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        clusterParams.setMargins(0, dp(10), dp(10), 0);
        wrap.addView(cluster, clusterParams);

        return wrap;
    }

    private Button clusterButton(String value, String type) {
        int bg;
        int color;
        int stroke;
        if ("complete".equals(type)) {
            bg = Color.argb(219, 236, 250, 244);
            color = Color.rgb(63, 135, 104);
            stroke = Color.rgb(138, 199, 170);
        } else if ("delete".equals(type)) {
            bg = Color.argb(173, 255, 227, 223);
            color = Color.rgb(191, 92, 85);
            stroke = bg;
        } else {
            bg = Color.argb(184, 255, 240, 209);
            color = Color.rgb(107, 106, 51);
            stroke = bg;
        }
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(11);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(color);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setBackground(roundRect(bg, stroke, dp(999)));
        flattenButton(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(24), dp(24));
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void showTodoDialog(AppState.Todo editing) {
        AppState.Todo draft = editing == null ? new AppState.Todo() : editing;
        LinearLayout form = form();

        LinearLayout basicCard = card();
        EditText title = titleInput(draft.title);
        title.setHint("例如：交房前最后一次验房");
        EditText note = input(draft.note);
        note.setHint("备注（选填）");
        note.setMinLines(3);
        basicCard.addView(label("事项"));
        basicCard.addView(title);
        basicCard.addView(label("备注"));
        basicCard.addView(note);

        EditText deadline = input(draft.deadline.isEmpty() ? state.endDate : draft.deadline);
        makeDateOnly(deadline);
        deadline.setOnClickListener(v -> pickDate(deadline.getText().toString(), deadline::setText));
        basicCard.addView(label("Deadline"));
        basicCard.addView(deadline);

        EditText reminder = input(draft.reminderAt);
        makeDateOnly(reminder);
        reminder.setHint("点击选择提醒时间");
        reminder.setOnClickListener(v -> {
            String base = reminder.getText().toString().trim().isEmpty()
                ? deadline.getText().toString().trim() + "T09:00"
                : reminder.getText().toString();
            pickDateTime(base, reminder::setText);
        });
        LinearLayout reminderRow = row();
        reminderRow.addView(reminder, new LinearLayout.LayoutParams(0, -2, 1));
        Button clearReminder = circleButton("×", rose);
        LinearLayout.LayoutParams clearReminderParams = new LinearLayout.LayoutParams(dp(52), dp(34));
        clearReminderParams.setMargins(dp(8), 0, 0, 0);
        clearReminder.setLayoutParams(clearReminderParams);
        clearReminder.setOnClickListener(v -> reminder.setText(""));
        reminderRow.addView(clearReminder);
        basicCard.addView(label("提醒时间"));
        basicCard.addView(reminderRow);

        Spinner repeat = spinner(new String[]{"none", "daily", "weekly", "monthly"}, draft.repeat);
        basicCard.addView(label("提醒周期"));
        basicCard.addView(repeat);
        form.addView(basicCard);

        int daysLeft = CountdownMath.daysUntil(parseDate(deadline.getText().toString()), LocalDate.now());
        showEditorDialog(
            editing == null ? "今天要推进哪件事？" : "编辑待办事项",
            "还剩 " + daysLeft + " 天",
            form,
            () -> {
                draft.title = title.getText().toString().trim();
                draft.deadline = deadline.getText().toString().trim();
                draft.reminderAt = reminder.getText().toString().trim();
                draft.repeat = repeat.getSelectedItem().toString();
                draft.note = note.getText().toString().trim();
                if (!draft.title.isEmpty() && !state.todos.contains(draft)) state.todos.add(draft);
                ReminderScheduler.schedule(this, draft);
                save();
                render();
            }
        );
    }

    private void renderRecords() {
        String recordSubtitle = state.records.isEmpty() ? null : "已记录 " + state.records.size() + " 条";
        sectionTitle("每日记录", recordSubtitle, sky, "添加", v -> showRecordDialog(null));

        List<String> labels = recordLabels();
        if (!labels.isEmpty()) {
            if (!RECORD_FILTER_ALL.equals(recordFilter) && !labels.contains(recordFilter)) recordFilter = RECORD_FILTER_ALL;
            List<String> filterOptions = new ArrayList<>();
            filterOptions.add(RECORD_FILTER_ALL);
            filterOptions.addAll(labels);
            LinearLayout filterRow = row();
            filterRow.setGravity(Gravity.CENTER_VERTICAL);
            filterRow.setPadding(0, 0, 0, dp(10));
            filterRow.addView(text("筛选", 13, muted, true));
            Spinner filter = spinner(filterOptions.toArray(new String[0]), recordFilter);
            filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String value = filterOptions.get(position);
                    if (!value.equals(recordFilter)) {
                        recordFilter = value;
                        render();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(0, -2, 1);
            filterParams.setMargins(dp(8), 0, 0, 0);
            filterRow.addView(filter, filterParams);
            content.addView(filterRow);
        } else {
            recordFilter = RECORD_FILTER_ALL;
        }

        List<AppState.DayRecord> records = new ArrayList<>(state.records);
        records.sort((a, b) -> b.date.compareTo(a.date));
        if (!RECORD_FILTER_ALL.equals(recordFilter)) {
            List<AppState.DayRecord> filtered = new ArrayList<>();
            for (AppState.DayRecord record : records) {
                for (AppState.RecordItem item : record.items) {
                    if (recordFilter.equals(item.label)) { filtered.add(record); break; }
                }
            }
            records = filtered;
        }
        if (records.isEmpty()) empty("还没有每日记录");
        for (AppState.DayRecord record : records) {
            content.addView(recordCard(record));
        }
    }

    private View recordCard(AppState.DayRecord record) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(-1, -2);
        wrapParams.setMargins(0, dp(4), 0, dp(6));
        wrap.setLayoutParams(wrapParams);

        LocalDate parsedDate = parseDate(record.date);

        LinearLayout dayMarker = row();
        LinearLayout.LayoutParams dayMarkerParams = new LinearLayout.LayoutParams(-1, -2);
        dayMarkerParams.setMargins(0, 0, 0, dp(12));
        dayMarker.setLayoutParams(dayMarkerParams);

        View dotBig = new View(this);
        dotBig.setBackground(roundRect(sky, Color.TRANSPARENT, dp(999)));
        LinearLayout.LayoutParams dotBigParams = new LinearLayout.LayoutParams(dp(11), dp(11));
        dotBigParams.setMargins(0, 0, dp(9), 0);
        dayMarker.addView(dotBig, dotBigParams);

        dayMarker.addView(text(parsedDate.getMonthValue() + "月" + parsedDate.getDayOfMonth() + "日", 15, ink, true));
        TextView weekText = text(WEEKDAYS_ZH[parsedDate.getDayOfWeek().getValue() - 1], 11, muted, true);
        LinearLayout.LayoutParams weekParams = new LinearLayout.LayoutParams(-2, -2);
        weekParams.setMargins(dp(8), 0, 0, 0);
        dayMarker.addView(weekText, weekParams);

        View spacer = new View(this);
        dayMarker.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1));

        LinearLayout cluster = row();
        Button edit = clusterButton("✎", "edit");
        edit.setOnClickListener(v -> showRecordDialog(record));
        Button delete = clusterButton("×", "delete");
        delete.setOnClickListener(v -> {
            state.records.remove(record);
            save();
            render();
        });
        cluster.addView(edit);
        cluster.addView(delete);
        dayMarker.addView(cluster);
        wrap.addView(dayMarker);

        List<AppState.RecordItem> displayItems = new ArrayList<>();
        for (AppState.RecordItem item : record.items) {
            if (RECORD_FILTER_ALL.equals(recordFilter) || recordFilter.equals(item.label)) displayItems.add(item);
        }
        displayItems.sort(Comparator.comparing(item -> item.time));

        FrameLayout timelineFrame = new FrameLayout(this);
        wrap.addView(timelineFrame, new LinearLayout.LayoutParams(-1, -2));

        if (displayItems.size() > 1) {
            View axisLine = dashedVerticalLine(Color.argb(120, Color.red(sky), Color.green(sky), Color.blue(sky)), dp(2));
            FrameLayout.LayoutParams axisLineParams = new FrameLayout.LayoutParams(dp(3), -1);
            axisLineParams.leftMargin = dp(71);
            axisLineParams.topMargin = dp(4);
            axisLineParams.bottomMargin = dp(18);
            timelineFrame.addView(axisLine, axisLineParams);
        }

        LinearLayout entriesList = new LinearLayout(this);
        entriesList.setOrientation(LinearLayout.VERTICAL);
        timelineFrame.addView(entriesList, new FrameLayout.LayoutParams(-1, -2));

        for (int i = 0; i < displayItems.size(); i++) {
            AppState.RecordItem item = displayItems.get(i);
            boolean last = i == displayItems.size() - 1;

            LinearLayout entry = row();
            entry.setGravity(Gravity.TOP);
            entry.setBaselineAligned(false);
            LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(-1, -2);
            entryParams.setMargins(0, 0, 0, last ? 0 : dp(16));
            entry.setLayoutParams(entryParams);

            LinearLayout typeCol = new LinearLayout(this);
            typeCol.setOrientation(LinearLayout.VERTICAL);
            typeCol.addView(recordChip(item.label));
            entry.addView(typeCol, new LinearLayout.LayoutParams(dp(52), -2));

            LinearLayout axisCol = new LinearLayout(this);
            axisCol.setOrientation(LinearLayout.VERTICAL);
            axisCol.setGravity(Gravity.CENTER_HORIZONTAL);
            axisCol.setBaselineAligned(false);
            entry.addView(axisCol, new LinearLayout.LayoutParams(dp(42), -2));

            View dot = new View(this);
            dot.setBackground(roundRect(Color.rgb(255, 250, 240), sky, dp(999)));
            axisCol.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));

            TextView timeLabel = text(item.time, 10, muted, true);
            LinearLayout.LayoutParams timeLabelParams = new LinearLayout.LayoutParams(-2, -2);
            timeLabelParams.topMargin = dp(5);
            axisCol.addView(timeLabel, timeLabelParams);

            LinearLayout contentCol = new LinearLayout(this);
            contentCol.setOrientation(LinearLayout.HORIZONTAL);
            contentCol.setGravity(Gravity.TOP);
            contentCol.setBaselineAligned(false);
            if (!item.imagePath.isEmpty()) {
                ImageView thumb = new ImageView(this);
                thumb.setImageBitmap(decodeSampledBitmap(item.imagePath, dp(58)));
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setClipToOutline(true);
                thumb.setBackground(roundRect(Color.TRANSPARENT, Color.rgb(238, 224, 207), dp(8)));
                LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(58), dp(58));
                thumbParams.setMargins(0, 0, dp(8), 0);
                contentCol.addView(thumb, thumbParams);
            }
            if (!item.text.isEmpty()) {
                TextView itemText = text(item.text, 13, ink, false);
                contentCol.addView(itemText, new LinearLayout.LayoutParams(0, -2, 1));
            }
            entry.addView(contentCol, new LinearLayout.LayoutParams(0, -2, 1));

            entriesList.addView(entry);
        }

        return wrap;
    }

    private void showRecordDialog(AppState.DayRecord editing) {
        AppState.DayRecord draft = editing == null ? new AppState.DayRecord() : editing;
        if (draft.date.isEmpty()) draft.date = LocalDate.now().toString();
        if (draft.items.isEmpty()) {
            draft.items.add(new AppState.RecordItem());
        }

        LinearLayout form = form();
        EditText date = input(draft.date);
        makeDateOnly(date);
        date.setOnClickListener(v -> pickDate(date.getText().toString(), date::setText));
        form.addView(label("日期"));
        form.addView(date);
        form.addView(sectionLabel("记录项", draft.items.size() + " 项"));
        LinearLayout itemsBox = form();
        form.addView(itemsBox);
        Runnable[] drawItemsRef = new Runnable[1];
        drawItemsRef[0] = () -> {
            itemsBox.removeAllViews();
            for (int i = 0; i < draft.items.size(); i++) {
                addRecordItemEditor(itemsBox, draft.items.get(i), i, draft, drawItemsRef[0]);
            }
        };
        drawItemsRef[0].run();
        Button add = ghostButton("＋ 添加记录项");
        add.setOnClickListener(v -> {
            draft.items.add(new AppState.RecordItem());
            drawItemsRef[0].run();
        });
        form.addView(add);
        showEditorDialog(
            editing == null ? "给今天留一页" : "编辑每日记录",
            null,
            form,
            () -> {
                draft.date = date.getText().toString().trim();
                boolean isNew = !state.records.contains(draft);
                AppState.DayRecord existing = null;
                for (AppState.DayRecord candidate : state.records) {
                    if (candidate != draft && candidate.date.equals(draft.date)) {
                        existing = candidate;
                        break;
                    }
                }
                if (existing != null) {
                    existing.items.addAll(draft.items);
                    state.records.remove(draft);
                } else if (isNew) {
                    state.records.add(draft);
                }
                save();
                render();
            }
        );
    }

    private void addRecordItemEditor(LinearLayout parent, AppState.RecordItem item, int index, AppState.DayRecord draft, Runnable drawItems) {
        LinearLayout box = card();

        LinearLayout head = row();
        head.setPadding(0, 0, 0, dp(10));
        TextView badge = text(String.valueOf(index + 1), 13, Color.rgb(121, 80, 36), true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(pill(Color.rgb(255, 240, 209)));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        badgeParams.setMargins(0, 0, dp(9), 0);
        badge.setLayoutParams(badgeParams);
        head.addView(badge);

        Spinner labelSpinner = recordLabelSpinner(item, drawItems);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, dp(34), 1);
        labelParams.setMargins(0, 0, dp(8), 0);
        head.addView(labelSpinner, labelParams);

        TextView timeChip = text(item.time, 12, Color.rgb(121, 80, 36), true);
        timeChip.setGravity(Gravity.CENTER);
        timeChip.setBackground(pill(Color.rgb(255, 240, 209)));
        timeChip.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-2, dp(34));
        head.addView(timeChip, timeParams);
        timeChip.setOnClickListener(v -> pickTime(item.time, value -> {
            item.time = value;
            timeChip.setText(value);
        }));

        Button delete = circleButton("×", rose);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(52), dp(34));
        deleteParams.setMargins(dp(8), 0, 0, 0);
        delete.setLayoutParams(deleteParams);
        delete.setOnClickListener(v -> {
            draft.items.remove(item);
            drawItems.run();
        });
        head.addView(delete);
        box.addView(head);

        LinearLayout mediaRow = row();
        mediaRow.setGravity(Gravity.TOP);

        FrameLayout uploadSlot = new FrameLayout(this);
        LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(dp(118), dp(118));
        slotParams.setMargins(0, 0, dp(10), 0);
        uploadSlot.setLayoutParams(slotParams);
        uploadSlot.setClickable(true);
        uploadSlot.setFocusable(true);

        if (!item.imagePath.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setImageBitmap(decodeSampledBitmap(item.imagePath, dp(118)));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setClipToOutline(true);
            image.setBackground(roundRect(Color.TRANSPARENT, Color.argb(153, 255, 255, 255), dp(8)));
            uploadSlot.addView(image, new FrameLayout.LayoutParams(-1, -1));
            TextView selected = text("已选照片", 11, Color.WHITE, true);
            selected.setPadding(dp(8), dp(6), dp(8), dp(6));
            uploadSlot.addView(selected, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START));
        } else {
            uploadSlot.setBackground(dashedRoundRect(Color.argb(158, 255, 255, 255), Color.argb(51, 78, 57, 42), dp(8)));
            TextView hint = text("上传图片\n或保留文字", 12, Color.rgb(121, 80, 36), true);
            hint.setGravity(Gravity.CENTER);
            uploadSlot.addView(hint, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        }
        uploadSlot.setOnClickListener(v -> {
            pendingImageItem = item;
            pendingImageRefresh = drawItems;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_IMAGE);
        });
        mediaRow.addView(uploadSlot);

        EditText text = input(item.text);
        text.setMinLines(4);
        text.setHint("写下今天");
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                item.text = s.toString();
            }
        });
        mediaRow.addView(text, new LinearLayout.LayoutParams(0, dp(118), 1));
        box.addView(mediaRow);
        parent.addView(box);
    }

    private Spinner recordLabelSpinner(AppState.RecordItem item, Runnable drawItems) {
        List<String> options = new ArrayList<>(recordLabels());
        if (!item.label.isEmpty() && !options.contains(item.label)) options.add(item.label);
        if (options.isEmpty()) options.add(item.label);
        options.add(NEW_RECORD_LABEL);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(13);
                view.setTypeface(Typeface.DEFAULT_BOLD);
                view.setTextColor(ink);
                view.setPadding(0, 0, 0, 0);
                view.setMinimumHeight(0);
                view.setBackgroundColor(Color.TRANSPARENT);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextSize(13);
                view.setTypeface(Typeface.DEFAULT_BOLD);
                view.setTextColor(ink);
                view.setPadding(dp(12), dp(10), dp(12), dp(10));
                view.setBackgroundColor(Color.rgb(255, 253, 248));
                return view;
            }
        };
        spinner.setAdapter(adapter);
        int selected = options.indexOf(item.label);
        spinner.setSelection(selected >= 0 ? selected : 0);
        spinner.setPadding(dp(10), dp(4), dp(10), dp(4));
        spinner.setMinimumWidth(0);
        spinner.setMinimumHeight(0);
        spinner.setBackground(roundRect(Color.argb(140, 255, 255, 255), Color.rgb(232, 220, 206), dp(8)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = options.get(position);
                if (value.equals(NEW_RECORD_LABEL)) {
                    promptNewRecordLabel(item, drawItems);
                } else {
                    item.label = value;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return spinner;
    }

    private void promptNewRecordLabel(AppState.RecordItem item, Runnable drawItems) {
        EditText nameEdit = titleInput("");
        nameEdit.setHint("例如：心情、天气");
        nameEdit.setSingleLine(true);

        Dialog dialog = new Dialog(this);
        LinearLayout shell = form();
        shell.setPadding(dp(16), dp(16), dp(16), dp(16));
        shell.setBackground(gradient(Color.rgb(255, 242, 216), Color.rgb(238, 247, 241)));
        shell.addView(text("新建记录项", 18, ink, true));
        shell.addView(label("记录项名称"));
        shell.addView(nameEdit);

        LinearLayout actions = row();
        actions.setPadding(0, dp(14), 0, 0);
        Button cancel = ghostButton("取消");
        cancel.setOnClickListener(v -> {
            dialog.dismiss();
            drawItems.run();
        });
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(48), 0.9f);
        cancelParams.setMargins(0, 0, dp(10), 0);
        actions.addView(cancel, cancelParams);
        Button confirm = new Button(this);
        confirm.setText("添加");
        confirm.setTextColor(Color.WHITE);
        confirm.setBackground(gradient(rose, Color.rgb(247, 161, 95), dp(999)));
        flattenButton(confirm);
        confirm.setOnClickListener(v -> {
            String value = nameEdit.getText().toString().trim();
            if (value.isEmpty()) {
                toast("请输入记录项名称");
                return;
            }
            item.label = value;
            dialog.dismiss();
            drawItems.run();
        });
        actions.addView(confirm, new LinearLayout.LayoutParams(0, dp(48), 1.1f));
        shell.addView(actions);

        dialog.setContentView(shell);
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -2);
        dialog.setOnCancelListener(d -> drawItems.run());
        dialog.show();
    }

    private void renderSummary() {
        List<String> labels = recordLabels();
        sectionTitle("进度汇总", labels.isEmpty() ? null : "共 " + labels.size() + " 项记录可选", butter, null, null);
        if (labels.isEmpty()) {
            empty("保存每日记录后可以生成汇总");
            return;
        }
        Spinner field = spinner(labels.toArray(new String[0]), labels.get(0));
        Spinner mode = spinner(new String[]{"图片拼图", "文字排版"}, "图片拼图");
        Button build = softButton("生成汇总");
        ImageView preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        build.setOnClickListener(v -> {
            summaryBitmap = drawSummary(field.getSelectedItem().toString(), mode.getSelectedItem().toString());
            preview.setImageBitmap(summaryBitmap);
        });
        Button saveButton = softButton("保存到相册");
        saveButton.setOnClickListener(v -> saveSummaryToGallery());
        content.addView(label("记录项"));
        content.addView(field);
        content.addView(label("排版"));
        content.addView(mode);
        content.addView(build);
        content.addView(saveButton);
        content.addView(preview);
    }

    private Bitmap drawSummary(String label, String mode) {
        Bitmap bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(Color.rgb(255, 250, 240));
        paint.setColor(ink);
        paint.setTextSize(46);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(state.title, 70, 82, paint);
        paint.setColor(Color.rgb(204, 98, 93));
        paint.setTextSize(26);
        canvas.drawText(label + " · " + mode, 72, 124, paint);

        List<AppState.RecordItem> items = matchingItems(label);
        if ("图片拼图".equals(mode)) {
            List<AppState.RecordItem> images = new ArrayList<>();
            for (AppState.RecordItem item : items) if (!item.imagePath.isEmpty()) images.add(item);
            List<CountdownMath.Cell> cells = CountdownMath.collageCells(images.size(), 1080, 650);
            for (int i = 0; i < images.size(); i++) {
                CountdownMath.Cell cell = cells.get(i);
                Bitmap image = decodeSampledBitmap(images.get(i).imagePath, Math.max(cell.width, cell.height));
                if (image != null) canvas.drawBitmap(image, null, new RectF(cell.x + 60, cell.y + 170, cell.x + 60 + cell.width, cell.y + 170 + cell.height), paint);
            }
        } else {
            paint.setColor(ink);
            paint.setTextSize(25);
            int y = 190;
            for (AppState.RecordItem item : items) {
                if (item.text.isEmpty()) continue;
                canvas.drawText(item.label + "：" + item.text, 74, y, paint);
                y += 62;
            }
        }
        return bitmap;
    }

    private void saveSummaryToGallery() {
        if (summaryBitmap == null) {
            toast("请先生成汇总");
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "warm-countdown-summary-" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            OutputStream out = getContentResolver().openOutputStream(uri);
            summaryBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            toast("已保存到相册");
        } catch (Exception error) {
            toast("保存失败");
        }
    }

    private void renderTimeline() {
        String nodeSubtitle = state.nodes.isEmpty() ? null : "共 " + state.nodes.size() + " 个节点";
        sectionTitle("关键节点", nodeSubtitle, plum, "添加节点", v -> showNodeDialog(null));
        List<CountdownMath.TimelineNode> custom = new ArrayList<>();
        for (AppState.KeyNode node : state.nodes) {
            custom.add(new CountdownMath.TimelineNode(node.id, parseDate(node.date), node.title, node.note, false));
        }
        List<CountdownMath.TimelineNode> timeline = new ArrayList<>();
        for (CountdownMath.TimelineNode node : CountdownMath.timeline(parseDate(state.startDate), parseDate(state.endDate), custom)) {
            if (!node.auto) timeline.add(node);
        }
        if (timeline.isEmpty()) {
            empty("还没有关键节点");
            return;
        }

        LocalDate start = parseDate(state.startDate);
        LocalDate end = parseDate(state.endDate);
        int totalDays = Math.max(1, CountdownMath.daysUntil(end, start));
        LocalDate today = LocalDate.now();
        LocalDate anchor = today.isBefore(start) ? start : (today.isAfter(end) ? end : today);
        int passedDays = CountdownMath.daysUntil(anchor, start);
        float todayPct = passedDays / (float) totalDays;

        int[] palette = {butter, mint, sky, plum, rose};
        int[] nodeColors = new int[timeline.size()];
        float[] nodePct = new float[timeline.size()];
        for (int i = 0; i < timeline.size(); i++) {
            CountdownMath.TimelineNode node = timeline.get(i);
            nodeColors[i] = palette[i % palette.length];
            int offset = Math.max(0, Math.min(totalDays, CountdownMath.daysUntil(node.date, start)));
            nodePct[i] = offset / (float) totalDays;
        }

        LinearLayout wrap = form();
        wrap.setBackground(roundRect(Color.TRANSPARENT, Color.TRANSPARENT, 0));

        LinearLayout overview = card();
        LinearLayout head = row();
        head.addView(text("整体进度", 12, muted, true), weightParams(1));
        head.addView(text("第 " + passedDays + " 天 · " + Math.round(todayPct * 100) + "%", 15, Color.rgb(200, 95, 88), true));
        overview.addView(head);
        View track = progressTrack(nodeColors, nodePct, todayPct);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(-1, dp(22));
        trackParams.topMargin = dp(8);
        overview.addView(track, trackParams);
        LinearLayout caps = row();
        caps.addView(text(monthDay(start), 10, muted, true), weightParams(1));
        caps.addView(text(monthDay(end), 10, muted, true));
        overview.addView(caps);
        wrap.addView(overview);

        for (int i = 0; i < timeline.size(); i++) {
            CountdownMath.TimelineNode node = timeline.get(i);
            int color = nodeColors[i];

            LinearLayout outer = new LinearLayout(this);
            outer.setOrientation(LinearLayout.HORIZONTAL);
            outer.setBaselineAligned(false);
            outer.setBackground(roundRect(Color.rgb(255, 253, 248), Color.rgb(238, 224, 207), dp(10)));
            outer.setPadding(dp(6), dp(2), 0, dp(2));
            LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(-1, -2);
            outerParams.setMargins(0, 0, 0, dp(9));
            outer.setLayoutParams(outerParams);

            View accent = new View(this);
            accent.setBackground(pill(color));
            LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(4), -1);
            accentParams.setMargins(0, dp(6), dp(10), dp(6));
            outer.addView(accent, accentParams);

            LinearLayout body = form();
            body.setPadding(0, dp(9), dp(12), dp(9));

            LinearLayout top = row();
            top.setBaselineAligned(false);
            top.addView(text(node.title, 16, ink, true), weightParams(1));
            TextView pctChip = text(Math.round(nodePct[i] * 100) + "%", 10, Color.WHITE, true);
            pctChip.setPadding(dp(7), dp(2), dp(7), dp(2));
            pctChip.setBackground(pill(color));
            top.addView(pctChip);
            LinearLayout cluster = row();
            Button edit = clusterButton("✎", "edit");
            edit.setOnClickListener(v -> showNodeDialog(findNode(node.id)));
            Button delete = clusterButton("×", "delete");
            delete.setOnClickListener(v -> {
                AppState.KeyNode target = findNode(node.id);
                if (target != null) state.nodes.remove(target);
                save();
                render();
            });
            cluster.addView(edit);
            cluster.addView(delete);
            LinearLayout.LayoutParams clusterParams = new LinearLayout.LayoutParams(-2, -2);
            clusterParams.setMargins(dp(6), 0, 0, 0);
            top.addView(cluster, clusterParams);
            body.addView(top);

            TextView meta = text(node.date + " · " + CountdownMath.daysUntil(node.date, LocalDate.now()) + " 天", 13, Color.rgb(60, 107, 88), true);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-2, -2);
            metaParams.topMargin = dp(4);
            body.addView(meta, metaParams);
            if (node.note != null && !node.note.isEmpty()) body.addView(text(node.note, 15, ink, false));

            outer.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
            wrap.addView(outer);
        }

        content.addView(wrap);
    }

    private String monthDay(LocalDate date) {
        return date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
    }

    private View progressTrack(int[] colors, float[] positions, float todayPct) {
        View view = new View(this) {
            private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint tickStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint todayFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint todayStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                bgPaint.setColor(Color.argb(60, Color.red(plum), Color.green(plum), Color.blue(plum)));
                tickStrokePaint.setColor(Color.rgb(255, 253, 248));
                tickStrokePaint.setStyle(Paint.Style.STROKE);
                tickStrokePaint.setStrokeWidth(dp(2));
                todayFillPaint.setColor(Color.WHITE);
                todayStrokePaint.setColor(Color.rgb(200, 95, 88));
                todayStrokePaint.setStyle(Paint.Style.STROKE);
                todayStrokePaint.setStrokeWidth(dp(3));
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                float left = getPaddingLeft();
                float right = w - getPaddingRight();
                fillPaint.setShader(new LinearGradient(left, 0, right, 0, Color.rgb(238, 126, 115), butter, Shader.TileMode.CLAMP));
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float left = getPaddingLeft();
                float right = getWidth() - getPaddingRight();
                float contentW = right - left;
                float cy = getHeight() / 2f;
                float barHalf = dp(4);
                canvas.drawRoundRect(left, cy - barHalf, right, cy + barHalf, barHalf, barHalf, bgPaint);
                float fillX = left + contentW * todayPct;
                if (fillX > left) canvas.drawRoundRect(left, cy - barHalf, fillX, cy + barHalf, barHalf, barHalf, fillPaint);
                for (int i = 0; i < positions.length; i++) {
                    float x = left + contentW * positions[i];
                    tickPaint.setColor(colors[i]);
                    canvas.drawCircle(x, cy, dp(5), tickPaint);
                    canvas.drawCircle(x, cy, dp(5), tickStrokePaint);
                }
                float tx = left + contentW * todayPct;
                canvas.drawCircle(tx, cy, dp(6), todayFillPaint);
                canvas.drawCircle(tx, cy, dp(6), todayStrokePaint);
            }
        };
        view.setPadding(dp(8), 0, dp(8), 0);
        return view;
    }

    private void showNodeDialog(AppState.KeyNode editing) {
        AppState.KeyNode draft = editing == null ? new AppState.KeyNode() : editing;
        if (draft.date.isEmpty()) draft.date = LocalDate.now().toString();
        LinearLayout form = form();
        EditText date = input(draft.date);
        EditText title = input(draft.title);
        EditText note = input(draft.note);
        note.setMinLines(3);
        makeDateOnly(date);
        date.setOnClickListener(v -> pickDate(date.getText().toString(), date::setText));
        form.addView(label("日期"));
        form.addView(date);
        form.addView(label("标题"));
        form.addView(title);
        form.addView(label("记录"));
        form.addView(note);
        showEditorDialog(
            editing == null ? "在时间轴上插一枚书签" : "编辑关键节点",
            null,
            form,
            () -> {
                draft.date = date.getText().toString().trim();
                draft.title = title.getText().toString().trim();
                draft.note = note.getText().toString().trim();
                if (!draft.title.isEmpty() && !state.nodes.contains(draft)) state.nodes.add(draft);
                save();
                render();
            }
        );
    }

    private void showProjectSwitcher() {
        Dialog dialog = new Dialog(this);
        LinearLayout shell = form();
        shell.setPadding(dp(16), dp(16), dp(16), dp(16));
        shell.setBackground(gradient(Color.rgb(255, 242, 216), Color.rgb(238, 247, 241)));
        shell.addView(text("切换倒计时项目", 18, ink, true));

        LinearLayout list = form();
        list.setPadding(0, dp(12), 0, dp(4));
        for (AppState project : workspace.projects) {
            boolean active = project.id.equals(workspace.currentProjectId);
            LinearLayout item = row();
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            item.setBackground(roundRect(active ? Color.rgb(255, 240, 209) : Color.rgb(255, 253, 248), Color.rgb(232, 220, 206), dp(8)));
            item.setClickable(true);
            item.setFocusable(true);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, -2);
            itemParams.setMargins(0, 0, 0, dp(8));
            item.setLayoutParams(itemParams);
            LinearLayout texts = form();
            texts.setPadding(0, 0, 0, 0);
            texts.addView(text(project.title, 15, ink, true));
            texts.addView(text(project.startDate + " ~ " + project.endDate, 12, muted, false));
            item.addView(texts, weightParams(1));
            if (active) item.addView(text("当前", 12, Color.rgb(121, 80, 36), true));
            item.setOnClickListener(v -> {
                if (!active) {
                    workspace.currentProjectId = project.id;
                    state = project;
                    save();
                    buildShell();
                    render();
                }
                dialog.dismiss();
            });
            list.addView(item);
        }
        shell.addView(list);

        Button add = ghostButton("＋ 新建倒计时项目");
        add.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateProjectDialog();
        });
        shell.addView(add);

        dialog.setContentView(shell);
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -2);
        dialog.show();
    }

    private void showCreateProjectDialog() {
        EditText nameEdit = titleInput("");
        nameEdit.setHint("例如：搬家倒计时");
        nameEdit.setSingleLine(true);

        Dialog dialog = new Dialog(this);
        LinearLayout shell = form();
        shell.setPadding(dp(16), dp(16), dp(16), dp(16));
        shell.setBackground(gradient(Color.rgb(255, 242, 216), Color.rgb(238, 247, 241)));
        shell.addView(text("新建倒计时项目", 18, ink, true));
        shell.addView(label("项目名称"));
        shell.addView(nameEdit);

        LinearLayout actions = row();
        actions.setPadding(0, dp(14), 0, 0);
        Button cancel = ghostButton("取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(48), 0.9f);
        cancelParams.setMargins(0, 0, dp(10), 0);
        actions.addView(cancel, cancelParams);
        Button confirm = new Button(this);
        confirm.setText("创建");
        confirm.setTextColor(Color.WHITE);
        confirm.setBackground(gradient(rose, Color.rgb(247, 161, 95), dp(999)));
        flattenButton(confirm);
        confirm.setOnClickListener(v -> {
            String projectName = nameEdit.getText().toString().trim();
            if (projectName.isEmpty()) {
                toast("请输入项目名称");
                return;
            }
            AppState project = new AppState();
            project.title = projectName;
            project.startDate = LocalDate.now().toString();
            project.endDate = LocalDate.now().plusDays(100).toString();
            workspace.projects.add(project);
            workspace.currentProjectId = project.id;
            state = project;
            save();
            buildShell();
            render();
            dialog.dismiss();
        });
        actions.addView(confirm, new LinearLayout.LayoutParams(0, dp(48), 1.1f));
        shell.addView(actions);

        dialog.setContentView(shell);
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -2);
        dialog.show();
    }

    private void showRenameProjectDialog() {
        EditText nameEdit = titleInput(state.title);
        nameEdit.setHint("倒计时名称");
        nameEdit.setSingleLine(true);

        Dialog dialog = new Dialog(this);
        LinearLayout shell = form();
        shell.setPadding(dp(16), dp(16), dp(16), dp(16));
        shell.setBackground(gradient(Color.rgb(255, 242, 216), Color.rgb(238, 247, 241)));
        shell.addView(text("编辑倒计时名称", 18, ink, true));
        shell.addView(label("项目名称"));
        shell.addView(nameEdit);

        LinearLayout actions = row();
        actions.setPadding(0, dp(14), 0, 0);
        Button cancel = ghostButton("取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(48), 0.9f);
        cancelParams.setMargins(0, 0, dp(10), 0);
        actions.addView(cancel, cancelParams);
        Button confirm = new Button(this);
        confirm.setText("保存");
        confirm.setTextColor(Color.WHITE);
        confirm.setBackground(gradient(rose, Color.rgb(247, 161, 95), dp(999)));
        flattenButton(confirm);
        confirm.setOnClickListener(v -> {
            String newTitle = nameEdit.getText().toString().trim();
            state.title = newTitle.isEmpty() ? "倒计时 100 天" : newTitle;
            save();
            render();
            dialog.dismiss();
        });
        actions.addView(confirm, new LinearLayout.LayoutParams(0, dp(48), 1.1f));
        shell.addView(actions);

        dialog.setContentView(shell);
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -2);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && pendingImageItem != null) {
            pendingImageItem.imagePath = copyImage(data.getData());
            if (pendingImageRefresh != null) pendingImageRefresh.run();
        }
    }

    private String copyImage(Uri uri) {
        try {
            File file = new File(getFilesDir(), "record-" + System.currentTimeMillis() + ".jpg");
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            in.close();
            out.close();
            return file.getAbsolutePath();
        } catch (Exception error) {
            toast("图片读取失败");
            return "";
        }
    }

    private void sectionTitle(String title, String subtitle, int accent, String action, View.OnClickListener listener) {
        LinearLayout row = row();
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout left = row();
        View tab = new View(this);
        tab.setBackground(pill(accent));
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(dp(6), dp(24));
        left.addView(tab, tabParams);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        textWrap.setPadding(dp(10), 0, 0, 0);
        TextView titleView = text(title, 21, ink, true);
        titleView.setTypeface(serifBold());
        titleView.setLetterSpacing(0.02f);
        textWrap.addView(titleView);
        if (subtitle != null && !subtitle.isEmpty()) {
            textWrap.addView(text(subtitle, 11, muted, true));
        }
        left.addView(textWrap);
        row.addView(left, weightParams(1));

        if (listener != null) {
            Button button = actionChip(action);
            button.setOnClickListener(listener);
            row.addView(button);
        }
        content.addView(row);
    }

    private void showEditorDialog(String title, String hint, View body, Runnable onSave) {
        Dialog dialog = new Dialog(this);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(topRoundRect(Color.rgb(255, 246, 228), Color.rgb(243, 248, 242), dp(20)));

        View grabber = new View(this);
        grabber.setBackground(pill(Color.argb(60, 120, 84, 58)));
        LinearLayout.LayoutParams grabberParams = new LinearLayout.LayoutParams(dp(34), dp(4));
        grabberParams.gravity = Gravity.CENTER_HORIZONTAL;
        grabberParams.topMargin = dp(8);
        root.addView(grabber, grabberParams);

        LinearLayout header = row();
        header.setPadding(dp(14), dp(8), dp(14), dp(10));
        Button back = circleButton("‹", muted);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(30)));
        back.setOnClickListener(v -> dialog.dismiss());
        header.addView(back);
        TextView titleView = text(title, 17, ink, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(8);
        header.addView(titleView, titleParams);
        if (hint != null && !hint.isEmpty()) {
            TextView hintChip = text(hint, 11, Color.rgb(200, 95, 88), true);
            hintChip.setPadding(dp(9), dp(4), dp(9), dp(4));
            hintChip.setBackground(roundRect(Color.argb(36, 238, 126, 115), Color.TRANSPARENT, dp(999)));
            header.addView(hintChip);
        }
        root.addView(header);
        root.addView(divider());

        int maxBodyHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.68f);
        ScrollView scroll = new ScrollView(this) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(maxBodyHeight, View.MeasureSpec.AT_MOST));
            }
        };
        LinearLayout shell = form();
        shell.setPadding(dp(14), dp(10), dp(14), dp(4));
        shell.addView(body);
        scroll.addView(shell);
        root.addView(scroll);

        root.addView(divider());
        LinearLayout bottomBar = row();
        bottomBar.setPadding(dp(14), dp(10), dp(14), dp(14));
        Button cancelButton = ghostButton("取消");
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(46), 0.9f);
        cancelParams.setMargins(0, 0, dp(10), 0);
        bottomBar.addView(cancelButton, cancelParams);
        Button primaryButton = new Button(this);
        primaryButton.setText("保存");
        primaryButton.setTextColor(Color.WHITE);
        primaryButton.setBackground(gradient(rose, Color.rgb(247, 161, 95), dp(999)));
        flattenButton(primaryButton);
        primaryButton.setOnClickListener(v -> {
            onSave.run();
            dialog.dismiss();
        });
        bottomBar.addView(primaryButton, new LinearLayout.LayoutParams(0, dp(46), 1.1f));
        root.addView(bottomBar);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(-1, -2);
        }
        dialog.show();
    }

    private View divider() {
        View line = new View(this);
        line.setBackground(new android.graphics.drawable.ColorDrawable(Color.argb(36, 78, 57, 42)));
        line.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        return line;
    }

    private GradientDrawable topRoundRect(int start, int end, int radius) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{start, end});
        shape.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        return shape;
    }

    private LinearLayout todoMetaLine(AppState.Todo todo) {
        LinearLayout meta = row();
        meta.setPadding(0, dp(30), 0, dp(4));
        meta.addView(tag("Deadline " + todo.deadline, TAG_DEFAULT));
        if (todo.done) {
            meta.addView(tag("已完成", TAG_DEFAULT));
        } else {
            meta.addView(tag(CountdownMath.daysUntil(parseDate(todo.deadline), LocalDate.now()) + " 天", TAG_PLUM));
        }
        if (!todo.reminderAt.isEmpty()) {
            meta.addView(tag(repeatLabel(todo.repeat), TAG_BLUE));
        }
        return meta;
    }

    private TextView tag(String value, int variant) {
        int bg;
        int color;
        if (variant == TAG_BLUE) {
            bg = Color.argb(56, 137, 185, 218);
            color = Color.rgb(60, 100, 128);
        } else if (variant == TAG_PLUM) {
            bg = Color.argb(51, 184, 155, 204);
            color = Color.rgb(104, 81, 120);
        } else {
            bg = Color.rgb(255, 240, 209);
            color = Color.rgb(121, 80, 36);
        }
        TextView view = text(value, 11, color, true);
        view.setPadding(dp(8), dp(5), dp(8), dp(5));
        view.setBackground(pill(bg));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 0, dp(7), dp(6));
        view.setLayoutParams(params);
        return view;
    }

    private TextView sectionChip(String value) {
        TextView view = text(value, 12, Color.rgb(56, 107, 87), true);
        view.setPadding(dp(10), dp(4), dp(10), dp(4));
        view.setBackground(pill(Color.argb(56, 138, 199, 170)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 0, dp(7), dp(6));
        view.setLayoutParams(params);
        return view;
    }

    private TextView chip(String value) {
        return tag(value, TAG_DEFAULT);
    }

    private TextView recordChip(String label) {
        int idx = Math.abs(label.hashCode()) % recordChipBg.length;
        TextView view = text(label, 10, recordChipFg[idx], true);
        view.setPadding(dp(9), dp(2), dp(9), dp(2));
        view.setBackground(pill(recordChipBg[idx]));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 0, 0, dp(4));
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout card() {
        LinearLayout view = form();
        view.setPadding(dp(16), dp(14), dp(16), dp(14));
        view.setBackground(roundRect(Color.rgb(255, 253, 248), Color.rgb(238, 224, 207), dp(8)));
        view.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, dp(10));
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout form() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 13, muted, true);
        view.setPadding(0, dp(10), 0, dp(6));
        return view;
    }

    private LinearLayout sectionLabel(String title, String chipText) {
        LinearLayout row = row();
        row.setPadding(0, dp(14), 0, dp(2));
        row.addView(text(title, 16, ink, true), weightParams(1));
        if (chipText != null && !chipText.isEmpty()) {
            row.addView(sectionChip(chipText));
        }
        return row;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1.05f);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private EditText input(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setTextColor(ink);
        input.setTextSize(15);
        input.setSingleLine(false);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(roundRect(Color.argb(140, 255, 255, 255), Color.rgb(232, 220, 206), dp(10)));
        return input;
    }

    private EditText titleInput(String value) {
        EditText view = input(value);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(12), dp(15), dp(12), dp(15));
        view.setBackground(roundRect(Color.argb(219, 255, 251, 240), Color.rgb(232, 220, 206), dp(8)));
        return view;
    }

    private Button softButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.rgb(128, 82, 30));
        button.setBackground(roundRect(Color.rgb(255, 245, 219), Color.rgb(235, 214, 178), dp(999)));
        flattenButton(button);
        return button;
    }

    private Button actionChip(String value) {
        Button button = new Button(this);
        button.setText("＋ " + value);
        button.setTextSize(12.5f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.rgb(138, 74, 42));
        button.setPadding(dp(10), 0, dp(12), 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setBackground(gradient(Color.rgb(255, 224, 146), Color.rgb(255, 215, 194), dp(999)));
        flattenButton(button);
        button.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(28)));
        return button;
    }

    private Button ghostButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(ink);
        button.setBackground(roundRect(Color.rgb(255, 255, 255), Color.rgb(230, 218, 205), dp(999)));
        flattenButton(button);
        return button;
    }

    private Button circleButton(String value, int accent) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(accent == rose ? Color.rgb(150, 65, 60) : Color.rgb(54, 92, 78));
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setBackground(roundRect(Color.WHITE, accent, dp(999)));
        flattenButton(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
        params.setMargins(dp(8), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void flattenButton(Button button) {
        button.setStateListAnimator(null);
        button.setElevation(0);
    }

    private GradientDrawable roundRect(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private GradientDrawable pill(int fill) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(999));
        return shape;
    }

    private GradientDrawable dashedRoundRect(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(dp(1), stroke, dp(4), dp(3));
        return shape;
    }

    private View dashedVerticalLine(int color, int width) {
        return new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                paint.setColor(color);
                paint.setStrokeWidth(width);
                paint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float x = getWidth() / 2f;
                canvas.drawLine(x, 0, x, getHeight(), paint);
            }
        };
    }

    private GradientDrawable markShape() {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(255, 224, 146), Color.rgb(255, 248, 220)});
        shape.setCornerRadii(new float[]{dp(21), dp(21), dp(20), dp(20), dp(18), dp(18), dp(22), dp(22)});
        shape.setStroke(dp(1), Color.argb(31, 92, 64, 42));
        return shape;
    }

    private Drawable screenBackground() {
        GradientDrawable base = gradient(Color.rgb(255, 243, 216), Color.rgb(238, 247, 240));
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientRadius(dp(85));
        glow.setColors(new int[]{Color.argb(130, 255, 211, 110), Color.argb(0, 255, 211, 110)});
        LayerDrawable layered = new LayerDrawable(new Drawable[]{base, glow});
        layered.setLayerSize(1, dp(170), dp(170));
        layered.setLayerGravity(1, Gravity.TOP | Gravity.RIGHT);
        layered.setLayerInsetTop(1, -dp(60));
        layered.setLayerInsetRight(1, -dp(60));
        return layered;
    }

    private GradientDrawable gradient(int start, int end) {
        return gradient(start, end, 0);
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{start, end});
        shape.setCornerRadius(radius);
        return shape;
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) spinner.setSelection(i);
        spinner.setPadding(dp(12), dp(8), dp(12), dp(8));
        spinner.setBackground(roundRect(Color.argb(140, 255, 255, 255), Color.rgb(232, 220, 206), dp(10)));
        return spinner;
    }

    private LinearLayout.LayoutParams weightParams(float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, weight);
        params.setMargins(0, 0, dp(8), 0);
        return params;
    }

    private void empty(String message) {
        TextView view = text(message, 15, muted, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(42), 0, dp(42));
        content.addView(view);
    }

    private void pickDate(String current, DateCallback callback) {
        LocalDate date = parseDate(current);
        new DatePickerDialog(this, (picker, year, month, day) -> callback.set(LocalDate.of(year, month + 1, day).toString()), date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()).show();
    }

    private void pickDateTime(String current, DateCallback callback) {
        LocalDateTime dateTime = parseDateTime(current);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            LocalDate pickedDate = LocalDate.of(year, month + 1, day);
            new TimePickerDialog(this, (timePicker, hour, minute) ->
                callback.set(LocalDateTime.of(pickedDate, LocalTime.of(hour, minute)).format(dateTimeFormatter)),
                dateTime.getHour(), dateTime.getMinute(), true).show();
        }, dateTime.getYear(), dateTime.getMonthValue() - 1, dateTime.getDayOfMonth()).show();
    }

    private void pickTime(String current, DateCallback callback) {
        LocalTime time = parseTimeOfDay(current);
        new TimePickerDialog(this, (picker, hour, minute) ->
            callback.set(String.format("%02d:%02d", hour, minute)),
            time.getHour(), time.getMinute(), true).show();
    }

    private LocalTime parseTimeOfDay(String value) {
        try {
            return LocalTime.parse(value);
        } catch (Exception error) {
            return LocalTime.now();
        }
    }

    private void makeDateOnly(EditText field) {
        field.setFocusable(false);
        field.setCursorVisible(false);
        field.setKeyListener(null);
        field.setClickable(true);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception error) {
            return LocalDate.now();
        }
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, dateTimeFormatter);
        } catch (Exception error) {
            return LocalDateTime.now().withMinute(0);
        }
    }

    private List<String> recordLabels() {
        List<String> labels = new ArrayList<>();
        for (AppState.DayRecord record : state.records) {
            for (AppState.RecordItem item : record.items) {
                if (!labels.contains(item.label)) labels.add(item.label);
            }
        }
        return labels;
    }

    private List<AppState.RecordItem> matchingItems(String label) {
        List<AppState.RecordItem> items = new ArrayList<>();
        for (AppState.DayRecord record : state.records) {
            for (AppState.RecordItem item : record.items) {
                if (label.equals(item.label)) items.add(item);
            }
        }
        return items;
    }

    private AppState.KeyNode findNode(String id) {
        for (AppState.KeyNode node : state.nodes) if (node.id.equals(id)) return node;
        return null;
    }

    private String repeatLabel(String repeat) {
        if ("daily".equals(repeat)) return "每天";
        if ("weekly".equals(repeat)) return "每周";
        if ("monthly".equals(repeat)) return "每月";
        return "仅一次";
    }

    private void save() {
        store.save(workspace);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Bitmap decodeSampledBitmap(String path, int reqSize) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, options);
    }

    private Typeface serifBold() {
        return Typeface.create(Typeface.SERIF, Typeface.BOLD);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 18);
        }
    }

    interface DateCallback {
        void set(String date);
    }
}
