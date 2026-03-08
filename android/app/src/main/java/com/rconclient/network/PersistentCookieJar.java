package com.rconclient.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class PersistentCookieJar implements CookieJar {
    private static final String COOKIE_PREFS = "cookie_prefs";
    private SharedPreferences cookiePrefs;
    
    public PersistentCookieJar(Context context) {
        cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE);
    }
    
    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        SharedPreferences.Editor editor = cookiePrefs.edit();
        for (Cookie cookie : cookies) {
            editor.putString(cookie.name(), cookie.value());
        }
        editor.apply();
    }
    
    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = new ArrayList<>();
        Map<String, ?> allEntries = cookiePrefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Cookie cookie = new Cookie.Builder()
                    .name(entry.getKey())
                    .value(entry.getValue().toString())
                    .domain(url.host())
                    .path("/")
                    .build();
            cookies.add(cookie);
        }
        return cookies;
    }
    
    public void clear() {
        cookiePrefs.edit().clear().apply();
    }
}
