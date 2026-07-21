package com.warmcountdown.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppState {
    public String id = id("project");
    public String title = "倒计时 100 天";
    public String startDate = "";
    public String endDate = "";
    public final List<Todo> todos = new ArrayList<>();
    public final List<DayRecord> records = new ArrayList<>();
    public final List<KeyNode> nodes = new ArrayList<>();

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("startDate", startDate);
        json.put("endDate", endDate);
        json.put("todos", array(todos));
        json.put("records", array(records));
        json.put("nodes", array(nodes));
        return json;
    }

    public static AppState fromJson(JSONObject json) throws JSONException {
        AppState state = new AppState();
        state.id = json.optString("id", state.id);
        state.title = json.optString("title", state.title);
        state.startDate = json.optString("startDate", state.startDate);
        state.endDate = json.optString("endDate", state.endDate);
        readArray(json.optJSONArray("todos"), state.todos, Todo::fromJson);
        readArray(json.optJSONArray("records"), state.records, DayRecord::fromJson);
        readArray(json.optJSONArray("nodes"), state.nodes, KeyNode::fromJson);
        return state;
    }

    private static JSONArray array(List<? extends JsonModel> items) throws JSONException {
        JSONArray array = new JSONArray();
        for (JsonModel item : items) array.put(item.toJson());
        return array;
    }

    private static <T> void readArray(JSONArray array, List<T> out, JsonFactory<T> factory) throws JSONException {
        if (array == null) return;
        for (int index = 0; index < array.length(); index++) out.add(factory.fromJson(array.getJSONObject(index)));
    }

    interface JsonModel {
        JSONObject toJson() throws JSONException;
    }

    interface JsonFactory<T> {
        T fromJson(JSONObject json) throws JSONException;
    }

    public static class Todo implements JsonModel {
        public String id = id("todo");
        public String title = "";
        public String deadline = "";
        public String reminderAt = "";
        public String repeat = "none";
        public String note = "";
        public boolean done = false;

        static Todo fromJson(JSONObject json) {
            Todo todo = new Todo();
            todo.id = json.optString("id", todo.id);
            todo.title = json.optString("title", "");
            todo.deadline = json.optString("deadline", "");
            todo.reminderAt = json.optString("reminderAt", "");
            todo.repeat = json.optString("repeat", "none");
            todo.note = json.optString("note", "");
            todo.done = json.optBoolean("done", false);
            return todo;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("title", title);
            json.put("deadline", deadline);
            json.put("reminderAt", reminderAt);
            json.put("repeat", repeat);
            json.put("note", note);
            json.put("done", done);
            return json;
        }
    }

    public static class DayRecord implements JsonModel {
        public String id = id("record");
        public String date = "";
        public final List<RecordItem> items = new ArrayList<>();

        static DayRecord fromJson(JSONObject json) throws JSONException {
            DayRecord record = new DayRecord();
            record.id = json.optString("id", record.id);
            record.date = json.optString("date", "");
            readArray(json.optJSONArray("items"), record.items, RecordItem::fromJson);
            return record;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("date", date);
            json.put("items", array(items));
            return json;
        }
    }

    public static class RecordItem implements JsonModel {
        public String id = id("field");
        public String label = "记录项";
        public String text = "";
        public String imagePath = "";
        public String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        static RecordItem fromJson(JSONObject json) {
            RecordItem item = new RecordItem();
            item.id = json.optString("id", item.id);
            item.label = json.optString("label", item.label);
            item.text = json.optString("text", "");
            item.imagePath = json.optString("imagePath", "");
            item.time = json.optString("time", item.time);
            return item;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("label", label);
            json.put("text", text);
            json.put("imagePath", imagePath);
            json.put("time", time);
            return json;
        }
    }

    public static class KeyNode implements JsonModel {
        public String id = id("node");
        public String date = "";
        public String title = "";
        public String note = "";

        static KeyNode fromJson(JSONObject json) {
            KeyNode node = new KeyNode();
            node.id = json.optString("id", node.id);
            node.date = json.optString("date", "");
            node.title = json.optString("title", "");
            node.note = json.optString("note", "");
            return node;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("date", date);
            json.put("title", title);
            json.put("note", note);
            return json;
        }
    }

    public boolean mergeDuplicateRecords() {
        List<DayRecord> merged = new ArrayList<>();
        boolean changed = false;
        for (DayRecord record : records) {
            DayRecord existing = null;
            for (DayRecord candidate : merged) {
                if (candidate.date.equals(record.date)) {
                    existing = candidate;
                    break;
                }
            }
            if (existing != null) {
                existing.items.addAll(record.items);
                changed = true;
            } else {
                merged.add(record);
            }
        }
        records.clear();
        records.addAll(merged);
        return changed;
    }

    public static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static class Workspace {
        public String currentProjectId = "";
        public final List<AppState> projects = new ArrayList<>();

        public AppState current() {
            for (AppState project : projects) {
                if (project.id.equals(currentProjectId)) return project;
            }
            return projects.isEmpty() ? null : projects.get(0);
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("currentProjectId", currentProjectId);
            JSONArray projectsArray = new JSONArray();
            for (AppState project : projects) projectsArray.put(project.toJson());
            json.put("projects", projectsArray);
            return json;
        }

        public static Workspace fromJson(JSONObject json) throws JSONException {
            Workspace workspace = new Workspace();
            readArray(json.optJSONArray("projects"), workspace.projects, AppState::fromJson);
            workspace.currentProjectId = json.optString("currentProjectId", "");
            if (workspace.current() == null && !workspace.projects.isEmpty()) {
                workspace.currentProjectId = workspace.projects.get(0).id;
            }
            return workspace;
        }
    }
}
