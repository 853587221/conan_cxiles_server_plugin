package com.rconclient;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.adapter.CategoryAdapter;
import com.rconclient.adapter.ProductAdapter;
import com.rconclient.model.Category;
import com.rconclient.model.Product;
import com.rconclient.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnBack;
    private ImageButton btnMenu;
    private Button btnQueryGold;
    private Button btnShare;
    private Button btnAdmin;
    private EditText editSearch;
    private TextView textCategoryTitle;
    private RecyclerView recyclerCategories;
    private RecyclerView recyclerProducts;

    private ApiClient apiClient;
    private SharedPreferences prefs;
    private String username;
    private String baseUrl;
    private boolean isShopOwner = false;
    private List<Category> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private int selectedCategoryPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        apiClient = ApiClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);
        username = prefs.getString("username", "");
        baseUrl = prefs.getString("base_url", "");
        
        android.util.Log.d("ShopActivity", "onCreate: baseUrl=" + baseUrl + ", username=" + username);

        initViews();
        setupRecyclerViews();
        checkAdminPermission();
        loadData();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        btnBack = findViewById(R.id.btn_back);
        btnMenu = findViewById(R.id.btn_menu);
        btnQueryGold = findViewById(R.id.btn_query_gold);
        btnShare = findViewById(R.id.btn_share);
        btnAdmin = findViewById(R.id.btn_admin);
        editSearch = findViewById(R.id.edit_search);
        textCategoryTitle = findViewById(R.id.text_category_title);
        recyclerCategories = findViewById(R.id.recycler_categories);
        recyclerProducts = findViewById(R.id.recycler_products);

        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawer_menu)));
        btnQueryGold.setOnClickListener(v -> showQueryGoldDialog());
        
        btnShare.setOnClickListener(v -> shareShop());
        btnAdmin.setOnClickListener(v -> openAdminPage());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s.toString().trim();
                if (keyword.isEmpty()) {
                    loadProducts();
                } else {
                    searchProducts(keyword);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.drawer_menu))) {
            drawerLayout.closeDrawer(findViewById(R.id.drawer_menu));
        } else {
            super.onBackPressed();
        }
    }

    private void checkAdminPermission() {
        apiClient.verifySession(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String sessionUsername = json.optString("username");
                            if (sessionUsername.equals(username)) {
                                isShopOwner = true;
                                btnAdmin.setVisibility(View.VISIBLE);
                            } else {
                                isShopOwner = false;
                                btnAdmin.setVisibility(View.GONE);
                            }
                        } else {
                            isShopOwner = false;
                            btnAdmin.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        isShopOwner = false;
                        btnAdmin.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isShopOwner = false;
                    btnAdmin.setVisibility(View.GONE);
                });
            }
        });
    }

    private void shareShop() {
        String url = baseUrl;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String shopUrl = url + "/shop/" + username;
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("商店链接", shopUrl);
        clipboard.setPrimaryClip(clip);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "游戏商城");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "欢迎访问我的游戏商城: " + shopUrl);
        startActivity(Intent.createChooser(shareIntent, "分享商店链接"));
        
        Toast.makeText(this, "已复制商店链接，分享给好友访问吧！", Toast.LENGTH_SHORT).show();
    }

    private void openAdminPage() {
        Intent intent = new Intent(this, ShopAdminActivity.class);
        startActivityForResult(intent, 100);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            loadData();
        }
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryAdapter();
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        categoryAdapter.setOnCategoryClickListener((category, position) -> {
            selectedCategoryPosition = position;
            categoryAdapter.setSelectedPosition(position);
            textCategoryTitle.setText("📂 " + category.getName());
            drawerLayout.closeDrawer(findViewById(R.id.drawer_menu));
            loadProducts();
        });

        productAdapter = new ProductAdapter();
        productAdapter.setBaseUrl(baseUrl);
        productAdapter.setUsername(username);
        recyclerProducts.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerProducts.setAdapter(productAdapter);

        productAdapter.setOnProductClickListener(product -> {
            showProductDetailDialog(product);
        });
    }

    private void loadData() {
        loadCategories();
        loadProducts();
    }

    private void loadCategories() {
        apiClient.getShopCategories(username, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("categories");
                            categories.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                categories.add(Category.fromJson(arr.getJSONObject(i)));
                            }
                            categoryAdapter.setCategories(categories);
                            if (!categories.isEmpty()) {
                                textCategoryTitle.setText("📂 " + categories.get(0).getName());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void loadProducts() {
        apiClient.getShopProducts(username, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("products");
                            products.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                products.add(Product.fromJson(arr.getJSONObject(i)));
                            }
                            filterAndDisplayProducts();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void filterAndDisplayProducts() {
        if (selectedCategoryPosition == 0) {
            productAdapter.setProducts(products);
        } else {
            String categoryKey = categories.get(selectedCategoryPosition).getKey();
            List<Product> filtered = new ArrayList<>();
            for (Product p : products) {
                if (categoryKey.equals(p.getCategoryKey())) {
                    filtered.add(p);
                }
            }
            productAdapter.setProducts(filtered);
        }
    }

    private void searchProducts(String keyword) {
        apiClient.searchProducts(username, keyword, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("products");
                            List<Product> searchResults = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                searchResults.add(Product.fromJson(arr.getJSONObject(i)));
                            }
                            productAdapter.setProducts(searchResults);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void showQueryGoldDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("查询金额");

        View view = getLayoutInflater().inflate(R.layout.dialog_query_gold, null);
        EditText editSearchPlayer = view.findViewById(R.id.edit_search_player);
        TextView textResult = view.findViewById(R.id.text_result);

        editSearchPlayer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadPlayerGold(textResult);

        builder.setView(view);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }

    private void loadPlayerGold(TextView textView) {
        apiClient.getPlayers(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("players");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject p = arr.getJSONObject(i);
                                String name = p.optString("char_name", p.optString("CharName"));
                                double gold = p.optDouble("gold", p.optDouble("Gold"));
                                sb.append(name).append(": ").append(String.format("%.0f", gold)).append(" 🪙\n");
                            }
                            textView.setText(sb.toString());
                        }
                    } catch (Exception e) {
                        textView.setText("加载失败");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> textView.setText("加载失败: " + error));
            }
        });
    }

    private void showProductDetailDialog(Product product) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(product.getName());

        View view = getLayoutInflater().inflate(R.layout.dialog_product_detail, null);
        ImageView imageProduct = view.findViewById(R.id.image_product);
        TextView textDesc = view.findViewById(R.id.text_product_desc);
        TextView textPrice = view.findViewById(R.id.text_product_price);

        String imageUrl = null;
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            imageUrl = baseUrl + "/api/shop/image/" + username + "/" + product.getImage();
            com.bumptech.glide.Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_package)
                    .into(imageProduct);
        } else {
            imageProduct.setImageResource(R.drawable.ic_package);
        }

        textDesc.setText(product.getDescription());
        textPrice.setText("🪙 " + String.format("%.0f", product.getPrice()));

        final String finalImageUrl = imageUrl;
        imageProduct.setOnClickListener(v -> {
            if (finalImageUrl != null) {
                showImagePreviewDialog(finalImageUrl);
            }
        });

        builder.setView(view);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }

    private void showImagePreviewDialog(String imageUrl) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_package)
                .into(imageView);
        
        builder.setView(imageView);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }
}
