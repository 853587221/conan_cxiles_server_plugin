package com.rconclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.rconclient.network.ApiClient;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final String DEFAULT_SERVER_URL = "https://www.xiaolang.icu";

    private EditText editUsername;
    private EditText editPassword;
    private Button btnLogin;
    private TextView textRegister;
    
    private ApiClient apiClient;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiClient = ApiClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);

        initViews();
        loadSavedData();
        checkSession();
    }

    private void initViews() {
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        textRegister = findViewById(R.id.text_register);

        apiClient.setBaseUrl(DEFAULT_SERVER_URL);

        btnLogin.setOnClickListener(v -> attemptLogin());
        textRegister.setOnClickListener(v -> showRegisterDialog());
    }

    private void loadSavedData() {
        String savedUsername = prefs.getString("username", "");
        
        if (!TextUtils.isEmpty(savedUsername)) {
            editUsername.setText(savedUsername);
        }
    }

    private void checkSession() {
        apiClient.setBaseUrl(DEFAULT_SERVER_URL);
        prefs.edit().putString("base_url", DEFAULT_SERVER_URL).apply();

        apiClient.verifySession(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success")) {
                        runOnUiThread(() -> {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void attemptLogin() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            editUsername.setError("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editPassword.setError("请输入密码");
            return;
        }

        prefs.edit()
                .putString("base_url", DEFAULT_SERVER_URL)
                .putString("username", username)
                .apply();

        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");

        apiClient.login(username, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            String message = json.optString("message", "登录失败");
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                });
            }
        });
    }

    private void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("注册账号");

        View view = getLayoutInflater().inflate(R.layout.dialog_register, null);
        EditText editRegUsername = view.findViewById(R.id.edit_reg_username);
        EditText editRegPassword = view.findViewById(R.id.edit_reg_password);
        EditText editRegConfirm = view.findViewById(R.id.edit_reg_confirm);

        builder.setView(view);
        builder.setPositiveButton("注册", (dialog, which) -> {
            String username = editRegUsername.getText().toString().trim();
            String password = editRegPassword.getText().toString().trim();
            String confirm = editRegConfirm.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            register(username, password);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void register(String username, String password) {
        apiClient.register(username, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(LoginActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                            editUsername.setText(username);
                        } else {
                            String message = json.optString("message", "注册失败");
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "注册失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
