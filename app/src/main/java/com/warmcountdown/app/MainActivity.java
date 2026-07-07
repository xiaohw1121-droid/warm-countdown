package com.warmcountdown.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 44;
    private final int ink = Color.rgb(63, 53, 44);
    private final int muted = Color.rgb(132, 118, 107);
    private final int rose = Color.rgb(244, 143, 139);
    private final int butter = Color.rgb(255, 213, 111);
    private final int mint = Color.rgb(159, 214, 191);
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private AppState state;
    private AppStore store;
    private LinearLayout root;
    private LinearLayout content;
    private TextView daysText;
    private TextView subtitleText;
    private String currentTab = "todos";
    private AppState.RecordItem pendingImageItem;
    private Button pendingImageButton;
    private Bitmap summaryBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AppStore(this);
        state = store.load();
        requestNotificationPermission();
        buildShell();
        render();
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(28));
        root.setBackgroundColor(Color.rgb(255, 249, 239));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = row();
        TextView mark = text("100", 24, Color.rgb(125, 79, 29), true);
        mark.setGravity(Gravity.CENTER);
        header.addView(mark, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView title = text("把最后的日子认真收藏", 25, ink, true);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(14);
        header.addView(title, titleParams);
        root.addView(header);

        root.addView(countdownCard());
        root.addView(tabBar());
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
    }

    private View countdownCard() {
        LinearLayout card = card();
        card.setPadding(dp(18), dp(16), dp(18), dp(18));

        EditText titleInput = input(state.title);
        titleInput.setHint("倒计时名称");
        titleInput.setSingleLine(true);
        titleInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                state.title = titleInput.getText().toString().trim().isEmpty() ? "倒计时 100 天" : titleInput.getText().toString().trim();
                save();
            }
        });
        card.addView(label("倒计时名称"));
        card.addView(titleInput);

        LinearLayout daysRow = row();
        daysRow.setGravity(Gravity.BOTTOM);
        daysText = text("100", 74, Color.rgb(208, 113, 76), true);
        daysText.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        daysRow.addView(daysText);
        TextView day = text("天", 17, muted, true);
        LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(-2, -2);
        dayParams.leftMargin = dp(8);
        dayParams.bottomMargin = dp(12);
        daysRow.addView(day, dayParams);
        card.addView(daysRow);

        subtitleText = text("", 14, muted, false);
        card.addView(subtitleText);

        LinearLayout dateRow = row();
        Button start = softButton("开始 " + state.startDate);
        Button end = softButton("结束 " + state.endDate);
        start.setOnClickListener(v -> pickDate(state.startDate, value -> { state.startDate = value; save(); render(); }));
        end.setOnClickListener(v -> pickDate(state.endDate, value -> { state.endDate = value; save(); render(); }));
        dateRow.addView(start, weightParams(1));
        dateRow.addView(end, weightParams(1));
        card.addView(dateRow);
        return card;
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
        int left = CountdownMath.daysUntil(parseDate(state.endDate), LocalDate.now());
        daysText.setText(String.valueOf(left));
        int passed = Math.max(0, CountdownMath.daysUntil(LocalDate.now(), parseDate(state.startDate)));
        subtitleText.setText(left > 0 ? "已经走过 " + passed + " 天，还剩 " + left + " 天" : left == 0 ? "今天就是最后一天" : "已经过去 " + Math.abs(left) + " 天");
        content.removeAllViews();
        if ("todos".equals(currentTab)) renderTodos();
        if ("records".equals(currentTab)) renderRecords();
        if ("summary".equals(currentTab)) renderSummary();
        if ("timeline".equals(currentTab)) renderTimeline();
    }

    private void renderTodos() {
        sectionTitle("必须完成", "添加事项", v -> showTodoDialog(null));
        List<AppState.Todo> todos = new ArrayList<>(state.todos);
        todos.sort(Comparator.comparing((AppState.Todo todo) -> todo.done).thenComparing(todo -> todo.deadline));
        if (todos.isEmpty()) empty("还没有待办事项");
        for (AppState.Todo todo : todos) {
            LinearLayout card = card();
            LinearLayout top = row();
            TextView title = text((todo.done ? "✓ " : "") + todo.title, 18, ink, true);
            top.addView(title, weightParams(1));
            Button done = softButton(todo.done ? "恢复" : "完成");
            done.setOnClickListener(v -> {
                todo.done = !todo.done;
                ReminderScheduler.schedule(this, todo);
                save();
                render();
            });
            top.addView(done);
            card.addView(top);
            card.addView(text("Deadline " + todo.deadline + " · " + CountdownMath.daysUntil(parseDate(todo.deadline), LocalDate.now()) + " 天", 13, muted, true));
            if (!todo.reminderAt.isEmpty()) card.addView(text("提醒 " + todo.reminderAt.replace('T', ' ') + " · " + repeatLabel(todo.repeat), 13, Color.rgb(60, 107, 88), true));
            if (!todo.note.isEmpty()) card.addView(text(todo.note, 15, ink, false));
            LinearLayout actions = row();
            actions.setGravity(Gravity.RIGHT);
            Button edit = ghostButton("编辑");
            edit.setOnClickListener(v -> showTodoDialog(todo));
            Button delete = ghostButton("删除");
            delete.setOnClickListener(v -> {
                ReminderScheduler.cancel(this, todo.id);
                state.todos.remove(todo);
                save();
                render();
            });
            actions.addView(edit);
            actions.addView(delete);
            card.addView(actions);
            content.addView(card);
        }
    }

    private void showTodoDialog(AppState.Todo editing) {
        AppState.Todo draft = editing == null ? new AppState.Todo() : editing;
        LinearLayout form = form();
        EditText title = input(draft.title);
        EditText deadline = input(draft.deadline.isEmpty() ? state.endDate : draft.deadline);
        EditText reminder = input(draft.reminderAt);
        EditText note = input(draft.note);
        note.setMinLines(3);
        Spinner repeat = spinner(new String[]{"none", "daily", "weekly", "monthly"}, draft.repeat);
        deadline.setOnClickListener(v -> pickDate(deadline.getText().toString(), deadline::setText));
        reminder.setHint("2026-07-07T09:00");
        form.addView(label("事项"));
        form.addView(title);
        form.addView(label("Deadline"));
        form.addView(deadline);
        form.addView(label("提醒时间"));
        form.addView(reminder);
        form.addView(label("提醒周期"));
        form.addView(repeat);
        form.addView(label("备注"));
        form.addView(note);
        new AlertDialog.Builder(this)
            .setTitle(editing == null ? "添加待办" : "编辑待办")
            .setView(form)
            .setPositiveButton("保存", (dialog, which) -> {
                draft.title = title.getText().toString().trim();
                draft.deadline = deadline.getText().toString().trim();
                draft.reminderAt = reminder.getText().toString().trim();
                draft.repeat = repeat.getSelectedItem().toString();
                draft.note = note.getText().toString().trim();
                if (!draft.title.isEmpty() && !state.todos.contains(draft)) state.todos.add(draft);
                ReminderScheduler.schedule(this, draft);
                save();
                render();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void renderRecords() {
        sectionTitle("每日记录", "添加记录", v -> showRecordDialog(null));
        List<AppState.DayRecord> records = new ArrayList<>(state.records);
        records.sort((a, b) -> b.date.compareTo(a.date));
        if (records.isEmpty()) empty("还没有每日记录");
        for (AppState.DayRecord record : records) {
            LinearLayout card = card();
            LinearLayout top = row();
            top.addView(text(record.date, 18, ink, true), weightParams(1));
            Button edit = ghostButton("编辑");
            edit.setOnClickListener(v -> showRecordDialog(record));
            Button delete = ghostButton("删除");
            delete.setOnClickListener(v -> {
                state.records.remove(record);
                save();
                render();
            });
            top.addView(edit);
            top.addView(delete);
            card.addView(top);
            for (AppState.RecordItem item : record.items) {
                TextView label = text(item.label, 15, Color.rgb(204, 98, 93), true);
                label.setPadding(0, dp(10), 0, dp(4));
                card.addView(label);
                if (!item.imagePath.isEmpty()) {
                    ImageView image = new ImageView(this);
                    image.setImageBitmap(BitmapFactory.decodeFile(item.imagePath));
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    card.addView(image, new LinearLayout.LayoutParams(-1, dp(160)));
                }
                if (!item.text.isEmpty()) card.addView(text(item.text, 15, ink, false));
            }
            content.addView(card);
        }
    }

    private void showRecordDialog(AppState.DayRecord editing) {
        AppState.DayRecord draft = editing == null ? new AppState.DayRecord() : editing;
        if (draft.date.isEmpty()) draft.date = LocalDate.now().toString();
        if (draft.items.isEmpty()) {
            AppState.RecordItem photo = new AppState.RecordItem();
            photo.label = "照片";
            AppState.RecordItem words = new AppState.RecordItem();
            words.label = "想说的话";
            draft.items.add(photo);
            draft.items.add(words);
        }

        LinearLayout form = form();
        EditText date = input(draft.date);
        date.setOnClickListener(v -> pickDate(date.getText().toString(), date::setText));
        form.addView(label("日期"));
        form.addView(date);
        LinearLayout itemsBox = form();
        form.addView(itemsBox);
        Runnable drawItems = () -> {
            itemsBox.removeAllViews();
            for (AppState.RecordItem item : draft.items) addRecordItemEditor(itemsBox, item);
        };
        drawItems.run();
        Button add = softButton("添加记录项");
        add.setOnClickListener(v -> {
            draft.items.add(new AppState.RecordItem());
            drawItems.run();
        });
        form.addView(add);
        new AlertDialog.Builder(this)
            .setTitle(editing == null ? "添加每日记录" : "编辑每日记录")
            .setView(form)
            .setPositiveButton("保存", (dialog, which) -> {
                draft.date = date.getText().toString().trim();
                if (!state.records.contains(draft)) state.records.add(draft);
                save();
                render();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void addRecordItemEditor(LinearLayout parent, AppState.RecordItem item) {
        LinearLayout box = card();
        EditText label = input(item.label);
        EditText text = input(item.text);
        text.setMinLines(3);
        label.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) item.label = label.getText().toString().trim(); });
        text.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) item.text = text.getText().toString(); });
        Button image = softButton(item.imagePath.isEmpty() ? "上传图片" : "更换图片");
        image.setOnClickListener(v -> {
            item.label = label.getText().toString().trim().isEmpty() ? "记录项" : label.getText().toString().trim();
            item.text = text.getText().toString();
            pendingImageItem = item;
            pendingImageButton = image;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_IMAGE);
        });
        box.addView(label);
        box.addView(image);
        box.addView(text);
        parent.addView(box);
    }

    private void renderSummary() {
        sectionTitle("进度汇总", "", null);
        List<String> labels = recordLabels();
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
                Bitmap image = BitmapFactory.decodeFile(images.get(i).imagePath);
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
        sectionTitle("关键节点", "添加节点", v -> showNodeDialog(null));
        List<CountdownMath.TimelineNode> custom = new ArrayList<>();
        for (AppState.KeyNode node : state.nodes) {
            custom.add(new CountdownMath.TimelineNode(node.id, parseDate(node.date), node.title, node.note, false));
        }
        List<CountdownMath.TimelineNode> timeline = CountdownMath.timeline(parseDate(state.startDate), parseDate(state.endDate), custom);
        for (CountdownMath.TimelineNode node : timeline) {
            LinearLayout card = card();
            LinearLayout top = row();
            top.addView(text(node.title, 18, ink, true), weightParams(1));
            if (!node.auto) {
                Button edit = ghostButton("编辑");
                edit.setOnClickListener(v -> showNodeDialog(findNode(node.id)));
                Button delete = ghostButton("删除");
                delete.setOnClickListener(v -> {
                    AppState.KeyNode target = findNode(node.id);
                    if (target != null) state.nodes.remove(target);
                    save();
                    render();
                });
                top.addView(edit);
                top.addView(delete);
            }
            card.addView(top);
            card.addView(text(node.date + " · " + CountdownMath.daysUntil(node.date, LocalDate.now()) + " 天", 13, Color.rgb(60, 107, 88), true));
            if (node.note != null && !node.note.isEmpty()) card.addView(text(node.note, 15, ink, false));
            content.addView(card);
        }
    }

    private void showNodeDialog(AppState.KeyNode editing) {
        AppState.KeyNode draft = editing == null ? new AppState.KeyNode() : editing;
        if (draft.date.isEmpty()) draft.date = LocalDate.now().toString();
        LinearLayout form = form();
        EditText date = input(draft.date);
        EditText title = input(draft.title);
        EditText note = input(draft.note);
        note.setMinLines(3);
        date.setOnClickListener(v -> pickDate(date.getText().toString(), date::setText));
        form.addView(label("日期"));
        form.addView(date);
        form.addView(label("标题"));
        form.addView(title);
        form.addView(label("记录"));
        form.addView(note);
        new AlertDialog.Builder(this)
            .setTitle(editing == null ? "添加关键节点" : "编辑关键节点")
            .setView(form)
            .setPositiveButton("保存", (dialog, which) -> {
                draft.date = date.getText().toString().trim();
                draft.title = title.getText().toString().trim();
                draft.note = note.getText().toString().trim();
                if (!draft.title.isEmpty() && !state.nodes.contains(draft)) state.nodes.add(draft);
                save();
                render();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && pendingImageItem != null) {
            pendingImageItem.imagePath = copyImage(data.getData());
            if (pendingImageButton != null) pendingImageButton.setText("已选择图片");
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

    private void sectionTitle(String title, String action, View.OnClickListener listener) {
        LinearLayout row = row();
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(text(title, 22, ink, true), weightParams(1));
        if (listener != null) {
            Button button = softButton(action);
            button.setOnClickListener(listener);
            row.addView(button);
        }
        content.addView(row);
    }

    private LinearLayout card() {
        LinearLayout view = form();
        view.setPadding(dp(16), dp(14), dp(16), dp(14));
        view.setBackgroundColor(Color.rgb(255, 253, 248));
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
        return input;
    }

    private Button softButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.rgb(128, 82, 30));
        button.setBackgroundColor(Color.rgb(255, 245, 219));
        return button;
    }

    private Button ghostButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(ink);
        button.setBackgroundColor(Color.rgb(255, 255, 255));
        return button;
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) spinner.setSelection(i);
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

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception error) {
            return LocalDate.now();
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
        store.save(state);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
