package com.rconclient;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.rconclient.model.ChatMessage;
import com.rconclient.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerChatMessages;
    private SwipeRefreshLayout swipeRefresh;
    private TextView textEmpty;
    private ProgressBar progressLoading;
    private Button btnBack;

    private ApiClient apiClient;
    private ChatMessageAdapter adapter;
    private int currentOffset = 0;
    private int limit = 20;
    private boolean hasMore = true;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        apiClient = ApiClient.getInstance(this);

        initViews();
        setupRecyclerView();
        loadChatMessages();
    }

    private void initViews() {
        recyclerChatMessages = findViewById(R.id.recycler_chat_messages);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        textEmpty = findViewById(R.id.text_empty);
        progressLoading = findViewById(R.id.progress_loading);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(() -> {
            currentOffset = 0;
            hasMore = true;
            loadChatMessages();
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChatMessages.setLayoutManager(layoutManager);
        recyclerChatMessages.setAdapter(adapter);

        recyclerChatMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItemPosition == 0 && hasMore && !isLoading) {
                        loadMoreMessages();
                    }
                }
            }
        });
    }

    private void loadChatMessages() {
        if (isLoading) return;
        isLoading = true;

        if (currentOffset == 0) {
            progressLoading.setVisibility(View.VISIBLE);
            textEmpty.setVisibility(View.GONE);
        }

        apiClient.getChatMessages(currentOffset, limit, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("messages");
                            List<ChatMessage> messageList = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                messageList.add(ChatMessage.fromJson(arr.getJSONObject(i)));
                            }

                            hasMore = json.optBoolean("has_more", false);
                            currentOffset = json.optInt("offset", 0) + json.optInt("limit", limit);

                            Collections.reverse(messageList);
                            adapter.setMessages(messageList);

                            if (messageList.isEmpty()) {
                                textEmpty.setVisibility(View.VISIBLE);
                            } else {
                                textEmpty.setVisibility(View.GONE);
                                recyclerChatMessages.post(() -> {
                                    recyclerChatMessages.scrollToPosition(adapter.getItemCount() - 1);
                                });
                            }
                        } else {
                            textEmpty.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        textEmpty.setVisibility(View.VISIBLE);
                    }

                    progressLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    isLoading = false;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    textEmpty.setVisibility(View.VISIBLE);
                    isLoading = false;
                });
            }
        });
    }

    private void loadMoreMessages() {
        if (isLoading || !hasMore) return;
        isLoading = true;

        apiClient.getChatMessages(currentOffset, limit, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("messages");
                            List<ChatMessage> messageList = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                messageList.add(ChatMessage.fromJson(arr.getJSONObject(i)));
                            }

                            hasMore = json.optBoolean("has_more", false);
                            currentOffset = json.optInt("offset", 0) + json.optInt("limit", limit);

                            Collections.reverse(messageList);
                            adapter.addMessagesAtStart(messageList);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isLoading = false;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isLoading = false;
                });
            }
        });
    }
}
