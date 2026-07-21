package com.warmcountdown.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.time.LocalDate;

public class AppStore {
    private static final String PREF = "warm_countdown_store";
    private static final String KEY = "state";
    private final SharedPreferences preferences;

    public AppStore(Context context) {
        preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public AppState.Workspace load() {
        String raw = preferences.getString(KEY, "");
        try {
            if (!raw.isEmpty()) {
                JSONObject json = new JSONObject(raw);
                if (json.has("projects")) {
                    AppState.Workspace workspace = AppState.Workspace.fromJson(json);
                    if (!workspace.projects.isEmpty()) return workspace;
                } else {
                    AppState legacy = AppState.fromJson(json);
                    AppState.Workspace workspace = new AppState.Workspace();
                    workspace.projects.add(legacy);
                    workspace.currentProjectId = legacy.id;
                    return workspace;
                }
            }
        } catch (Exception ignored) {
        }
        return freshWorkspace();
    }

    public void save(AppState.Workspace workspace) {
        try {
            preferences.edit().putString(KEY, workspace.toJson().toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private AppState.Workspace freshWorkspace() {
        AppState state = new AppState();
        state.startDate = LocalDate.now().toString();
        state.endDate = LocalDate.now().plusDays(100).toString();
        AppState.Workspace workspace = new AppState.Workspace();
        workspace.projects.add(state);
        workspace.currentProjectId = state.id;
        return workspace;
    }
}
