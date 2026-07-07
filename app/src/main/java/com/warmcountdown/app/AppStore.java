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

    public AppState load() {
        String raw = preferences.getString(KEY, "");
        try {
            if (!raw.isEmpty()) return AppState.fromJson(new JSONObject(raw));
        } catch (Exception ignored) {
        }
        AppState state = new AppState();
        state.startDate = LocalDate.now().toString();
        state.endDate = LocalDate.now().plusDays(100).toString();
        return state;
    }

    public void save(AppState state) {
        try {
            preferences.edit().putString(KEY, state.toJson().toString()).apply();
        } catch (Exception ignored) {
        }
    }
}
