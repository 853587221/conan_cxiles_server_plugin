package com.rconclient.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "RconSession";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_HOST = "host";
    private static final String KEY_RCON_PASSWORD = "rcon_password";
    private static final String KEY_PORT = "port";
    private static final String KEY_RCON_MODE = "rcon_mode";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String username) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    public void saveConnectionInfo(String host, String password, String port, String rconMode) {
        editor.putString(KEY_HOST, host);
        editor.putString(KEY_RCON_PASSWORD, password);
        editor.putString(KEY_PORT, port);
        editor.putString(KEY_RCON_MODE, rconMode);
        editor.apply();
    }

    public ConnectionInfo getConnectionInfo() {
        return new ConnectionInfo(
            pref.getString(KEY_HOST, ""),
            pref.getString(KEY_RCON_PASSWORD, ""),
            pref.getString(KEY_PORT, "25575"),
            pref.getString(KEY_RCON_MODE, "direct")
        );
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public static class ConnectionInfo {
        public String host;
        public String password;
        public String port;
        public String rconMode;

        public ConnectionInfo(String host, String password, String port, String rconMode) {
            this.host = host;
            this.password = password;
            this.port = port;
            this.rconMode = rconMode;
        }
    }
}
