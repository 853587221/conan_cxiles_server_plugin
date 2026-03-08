package com.rconclient;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.model.Category;
import com.rconclient.model.Product;
import com.rconclient.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ShopAdminActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnTabCategories;
    private Button btnTabProducts;
    private LinearLayout containerCategories;
    private LinearLayout containerProducts;
    
    private EditText editCategoryName;
    private Button btnSelectIcon;
    private String selectedIcon = "📁";
    private EditText editCategorySort;
    private Button btnAddCategory;
    private Button btnCancelCategory;
    private RecyclerView recyclerCategories;
    
    private Spinner spinnerProductCategory;
    private EditText editProductName;
    private EditText editProductDesc;
    private Button btnUploadImage;
    private Button btnClearImage;
    private TextView textImageName;
    private ImageView imageProductPreview;
    private String productImage = "";
    private EditText editProductPrice;
    private EditText editProductSort;
    private Button btnSaveProduct;
    private Button btnCancelProduct;
    private RecyclerView recyclerProducts;

    private ApiClient apiClient;
    private List<Category> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private CategoryAdapter categoryAdapter;
    private ProductAdminAdapter productAdapter;
    
    private int editingCategoryId = 0;
    private int editingProductId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_admin);

        apiClient = ApiClient.getInstance(this);

        initViews();
        loadData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnTabCategories = findViewById(R.id.btn_tab_categories);
        btnTabProducts = findViewById(R.id.btn_tab_products);
        containerCategories = findViewById(R.id.container_categories);
        containerProducts = findViewById(R.id.container_products);
        
        editCategoryName = findViewById(R.id.edit_category_name);
        btnSelectIcon = findViewById(R.id.btn_select_icon);
        editCategorySort = findViewById(R.id.edit_category_sort);
        btnAddCategory = findViewById(R.id.btn_add_category);
        btnCancelCategory = findViewById(R.id.btn_cancel_category);
        recyclerCategories = findViewById(R.id.recycler_categories);
        
        spinnerProductCategory = findViewById(R.id.spinner_product_category);
        editProductName = findViewById(R.id.edit_product_name);
        editProductDesc = findViewById(R.id.edit_product_desc);
        btnUploadImage = findViewById(R.id.btn_upload_image);
        btnClearImage = findViewById(R.id.btn_clear_image);
        textImageName = findViewById(R.id.text_image_name);
        imageProductPreview = findViewById(R.id.image_product_preview);
        editProductPrice = findViewById(R.id.edit_product_price);
        editProductSort = findViewById(R.id.edit_product_sort);
        btnSaveProduct = findViewById(R.id.btn_save_product);
        btnCancelProduct = findViewById(R.id.btn_cancel_product);
        recyclerProducts = findViewById(R.id.recycler_products);

        btnBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        
        btnTabCategories.setOnClickListener(v -> {
            containerCategories.setVisibility(View.VISIBLE);
            containerProducts.setVisibility(View.GONE);
            btnTabCategories.setBackgroundTintList(getResources().getColorStateList(R.color.accent));
            btnTabProducts.setBackgroundTintList(getResources().getColorStateList(R.color.text_secondary));
        });
        
        btnTabProducts.setOnClickListener(v -> {
            containerCategories.setVisibility(View.GONE);
            containerProducts.setVisibility(View.VISIBLE);
            btnTabCategories.setBackgroundTintList(getResources().getColorStateList(R.color.text_secondary));
            btnTabProducts.setBackgroundTintList(getResources().getColorStateList(R.color.accent));
        });

        btnSelectIcon.setOnClickListener(v -> showIconPickerDialog());
        btnAddCategory.setOnClickListener(v -> saveCategory());
        btnCancelCategory.setOnClickListener(v -> clearCategoryForm());
        btnUploadImage.setOnClickListener(v -> selectImage());
        btnClearImage.setOnClickListener(v -> clearProductImage());
        btnSaveProduct.setOnClickListener(v -> saveProduct());
        btnCancelProduct.setOnClickListener(v -> clearProductForm());

        categoryAdapter = new CategoryAdapter();
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        productAdapter = new ProductAdminAdapter();
        recyclerProducts.setLayoutManager(new LinearLayoutManager(this));
        recyclerProducts.setAdapter(productAdapter);
    }
    
    private static final int PICK_IMAGE_REQUEST = 1001;
    
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择商品图片"), PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();
                
                int maxSize = 1024;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                if (ratio < 1) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, (int)(width * ratio), (int)(height * ratio), true);
                }
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] imageBytes = baos.toByteArray();
                
                uploadImage(imageBytes);
                
                imageProductPreview.setImageBitmap(bitmap);
                imageProductPreview.setVisibility(View.VISIBLE);
                
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void uploadImage(byte[] imageBytes) {
        apiClient.uploadShopImage(imageBytes, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            productImage = json.optString("filename");
                            textImageName.setText(productImage);
                            btnClearImage.setVisibility(View.VISIBLE);
                            Toast.makeText(ShopAdminActivity.this, "图片上传成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ShopAdminActivity.this, json.optString("message", "上传失败"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ShopAdminActivity.this, "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ShopAdminActivity.this, "上传失败: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void clearProductImage() {
        productImage = "";
        textImageName.setText("");
        imageProductPreview.setVisibility(View.GONE);
        btnClearImage.setVisibility(View.GONE);
    }

    private void loadData() {
        loadCategories();
        loadProducts();
    }

    private void loadCategories() {
        apiClient.getShopCategories(apiClient.getUsername(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("categories");
                            categories.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject catJson = arr.getJSONObject(i);
                                if (!"all".equals(catJson.optString("key"))) {
                                    categories.add(Category.fromJson(catJson));
                                }
                            }
                            categoryAdapter.setCategories(categories);
                            updateCategorySpinner();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void updateCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("选择分类");
        for (Category cat : categories) {
            categoryNames.add(cat.getIcon() + " " + cat.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProductCategory.setAdapter(adapter);
    }

    private void showIconPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_icon_picker, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        RecyclerView recyclerIcons = view.findViewById(R.id.recycler_icons);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        
        recyclerIcons.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 6));
        
        String[] icons = {"📁", "📂", "🗂️", "📊", "📈", "📉", "🎮", "🎲", "🎯", "🏆", "🥇", "💎", 
                "💰", "🪙", "⚔️", "🛡️", "🧪", "🔮", "❤️", "🧡", "💛", "💚", "💙", "💜", 
                "🖤", "🤍", "🍎", "🍊", "🍋", "🍇", "🍉", "🍓", "🍒", "🔑", "🗝️", "🔒", 
                "🔓", "⚡", "🔥", "❄️", "💧", "🌟", "✨", "💫", "🌙", "☀️", "⭐", "🎵", 
                "🎶", "🎬", "📺", "👑", "👗", "👟", "🎒", "👜", "👓", "⌚", "📱", "💻", 
                "🖥️", "🎁", "📦", "🛒", "🚗", "✈️", "🚀", "⚓", "🏠", "🏰", "🏝️", "🌴", 
                "🌺", "🌻", "🌹", "🍀", "🎄", "🎃", "🦄", "🐉", "🦖", "🦕", "🐙", "🦑", 
                "🍕", "🍔", "🍟", "🌭", "🍿", "🧂", "🥤", "☕", "🍵", "🍺", "🍻", "🥂", 
                "🍷", "🍹", "🧋", "🧁", "🍩", "🍪", "🍫", "🍬", "🍭", "🥗", "🍜", "🍣", 
                "🍤", "🥟", "🥠", "🥡", "🍲", "🍳", "🥘", "🍱", "🥢", "🍽️", "🔪", "💳", 
                "🎫", "🎟️", "🃏", "🪁", "🧸", "🤖", "🎩", "💍", "📿", "🎀", "🧣", "👕", 
                "👖", "👘", "👠", "👡", "👢", "👞", "🎿", "🛼", "🛷", "🥌", "🎽", "🛶", 
                "🏹", "🧿", "🎱", "🎳", "🎰", "🧩", "🔭", "🔬", "💊", "💉", "🩺", "🩹", 
                "🧴", "🧽", "🧹", "🧲", "🔌", "🔋", "🔦", "💡", "🕯️", "🕰️", "📲", "🖨️", 
                "⌨️", "🖱️", "🖲️", "💽", "💾", "💿", "📀", "📹", "🎥", "🎞️", "📽️", "📻", 
                "🎙️", "🎚️", "🎛️", "🕹️", "🎨", "🎭", "🎪", "🎗️", "🎠", "🎡", "🎢", "🎤", 
                "🎦", "🎧", "🎷", "🎸", "🎹", "🎺", "🎻", "🎼", "🎾"};
        
        IconAdapter iconAdapter = new IconAdapter(icons, icon -> {
            selectedIcon = icon;
            btnSelectIcon.setText(icon + " 选择图标");
            dialog.dismiss();
        });
        
        recyclerIcons.setAdapter(iconAdapter);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.7)
        );
    }

    private void loadProducts() {
        apiClient.getShopProducts(apiClient.getUsername(), new ApiClient.ApiCallback() {
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
                            productAdapter.setProducts(products);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void saveCategory() {
        String name = editCategoryName.getText().toString().trim();
        String icon = selectedIcon;
        int sortOrder = 0;
        try {
            sortOrder = Integer.parseInt(editCategorySort.getText().toString());
        } catch (NumberFormatException e) {}

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = name.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^\\u4e00-\\u9fa5a-z0-9_]", "");
        if (key.isEmpty()) {
            key = "category_" + System.currentTimeMillis();
        }

        try {
            JSONObject body = new JSONObject();
            if (editingCategoryId > 0) {
                body.put("id", editingCategoryId);
            }
            body.put("key", key);
            body.put("name", name);
            body.put("icon", icon);
            body.put("sort_order", sortOrder);

            String url = editingCategoryId > 0 ? "/api/shop/admin/category/update" : "/api/shop/admin/category/add";
            
            apiClient.post(url, body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                Toast.makeText(ShopAdminActivity.this, editingCategoryId > 0 ? "分类更新成功" : "分类添加成功", Toast.LENGTH_SHORT).show();
                                clearCategoryForm();
                                loadCategories();
                            } else {
                                Toast.makeText(ShopAdminActivity.this, json.optString("message", "操作失败"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ShopAdminActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(ShopAdminActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editCategory(Category category) {
        editingCategoryId = category.getId();
        editCategoryName.setText(category.getName());
        selectedIcon = category.getIcon() != null ? category.getIcon() : "📁";
        btnSelectIcon.setText(selectedIcon + " 选择图标");
        editCategorySort.setText(String.valueOf(category.getSortOrder()));
        btnAddCategory.setText("更新分类");
        btnCancelCategory.setVisibility(View.VISIBLE);
    }

    private void clearCategoryForm() {
        editingCategoryId = 0;
        editCategoryName.setText("");
        selectedIcon = "📁";
        btnSelectIcon.setText("📁 选择图标");
        editCategorySort.setText("0");
        btnAddCategory.setText("添加分类");
        btnCancelCategory.setVisibility(View.GONE);
    }

    private void deleteCategory(int id) {
        new AlertDialog.Builder(this)
                .setTitle("删除分类")
                .setMessage("确定要删除此分类吗？分类下的所有商品也会被删除。")
                .setPositiveButton("删除", (dialog, which) -> {
                    apiClient.post("/api/shop/admin/category/delete/" + id, new JSONObject(), new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                try {
                                    JSONObject json = new JSONObject(response);
                                    if (json.optBoolean("success")) {
                                        Toast.makeText(ShopAdminActivity.this, "分类已删除", Toast.LENGTH_SHORT).show();
                                        loadCategories();
                                    } else {
                                        Toast.makeText(ShopAdminActivity.this, json.optString("message", "删除失败"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(ShopAdminActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(ShopAdminActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveProduct() {
        int categoryPos = spinnerProductCategory.getSelectedItemPosition();
        if (categoryPos == 0) {
            Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String categoryKey = categories.get(categoryPos - 1).getKey();
        String name = editProductName.getText().toString().trim();
        String desc = editProductDesc.getText().toString().trim();
        double price = 0;
        int sortOrder = 0;
        
        try {
            price = Double.parseDouble(editProductPrice.getText().toString());
        } catch (NumberFormatException e) {}
        try {
            sortOrder = Integer.parseInt(editProductSort.getText().toString());
        } catch (NumberFormatException e) {}

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (price <= 0) {
            Toast.makeText(this, "价格必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject body = new JSONObject();
            if (editingProductId > 0) {
                body.put("id", editingProductId);
            }
            body.put("name", name);
            body.put("description", desc);
            body.put("image", productImage);
            body.put("category", categoryKey);
            body.put("price", price);
            body.put("sort_order", sortOrder);

            String url = editingProductId > 0 ? "/api/shop/admin/product/update" : "/api/shop/admin/product/add";

            apiClient.post(url, body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                Toast.makeText(ShopAdminActivity.this, editingProductId > 0 ? "商品更新成功" : "商品添加成功", Toast.LENGTH_SHORT).show();
                                clearProductForm();
                                loadProducts();
                            } else {
                                Toast.makeText(ShopAdminActivity.this, json.optString("message", "操作失败"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ShopAdminActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(ShopAdminActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editProduct(Product product) {
        editingProductId = product.getId();
        
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getKey().equals(product.getCategoryKey())) {
                spinnerProductCategory.setSelection(i + 1);
                break;
            }
        }
        
        editProductName.setText(product.getName());
        editProductDesc.setText(product.getDescription());
        productImage = product.getImage() != null ? product.getImage() : "";
        if (!productImage.isEmpty()) {
            textImageName.setText(productImage);
            btnClearImage.setVisibility(View.VISIBLE);
            String imageUrl = apiClient.getBaseUrl() + "/api/shop/image/" + apiClient.getUsername() + "/" + productImage + "?size=100x100";
            new com.rconclient.utils.ImageLoadTask(imageProductPreview).execute(imageUrl);
            imageProductPreview.setVisibility(View.VISIBLE);
        } else {
            textImageName.setText("");
            btnClearImage.setVisibility(View.GONE);
            imageProductPreview.setVisibility(View.GONE);
        }
        editProductPrice.setText(String.valueOf((int) product.getPrice()));
        editProductSort.setText(String.valueOf(product.getSortOrder()));
        btnSaveProduct.setText("更新商品");
        btnCancelProduct.setVisibility(View.VISIBLE);
    }

    private void clearProductForm() {
        editingProductId = 0;
        spinnerProductCategory.setSelection(0);
        editProductName.setText("");
        editProductDesc.setText("");
        productImage = "";
        textImageName.setText("");
        imageProductPreview.setVisibility(View.GONE);
        btnClearImage.setVisibility(View.GONE);
        editProductPrice.setText("");
        editProductSort.setText("0");
        btnSaveProduct.setText("保存商品");
        btnCancelProduct.setVisibility(View.GONE);
    }

    private void deleteProduct(int id) {
        new AlertDialog.Builder(this)
                .setTitle("删除商品")
                .setMessage("确定要删除此商品吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    apiClient.post("/api/shop/admin/product/delete/" + id, new JSONObject(), new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                try {
                                    JSONObject json = new JSONObject(response);
                                    if (json.optBoolean("success")) {
                                        Toast.makeText(ShopAdminActivity.this, "商品已删除", Toast.LENGTH_SHORT).show();
                                        loadProducts();
                                    } else {
                                        Toast.makeText(ShopAdminActivity.this, json.optString("message", "删除失败"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(ShopAdminActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(ShopAdminActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public interface OnIconSelectedListener {
        void onIconSelected(String icon);
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private List<Category> items = new ArrayList<>();

        public void setCategories(List<Category> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Category item = items.get(position);
            holder.textIcon.setText(item.getIcon() != null ? item.getIcon() : "📁");
            holder.textName.setText(item.getName());
            holder.btnEdit.setOnClickListener(v -> editCategory(item));
            holder.btnDelete.setOnClickListener(v -> deleteCategory(item.getId()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textIcon;
            TextView textName;
            Button btnEdit;
            Button btnDelete;

            ViewHolder(View view) {
                super(view);
                textIcon = view.findViewById(R.id.text_category_icon);
                textName = view.findViewById(R.id.text_category_name);
                btnEdit = view.findViewById(R.id.btn_edit);
                btnDelete = view.findViewById(R.id.btn_delete);
            }
        }
    }

    private class ProductAdminAdapter extends RecyclerView.Adapter<ProductAdminAdapter.ViewHolder> {
        private List<Product> items = new ArrayList<>();

        public void setProducts(List<Product> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Product item = items.get(position);
            holder.textIcon.setText("📦");
            holder.textName.setText(item.getName());
            holder.textPrice.setText("🪙 " + String.format("%.0f", item.getPrice()));
            holder.btnEdit.setOnClickListener(v -> editProduct(item));
            holder.btnDelete.setOnClickListener(v -> deleteProduct(item.getId()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textIcon;
            TextView textName;
            TextView textPrice;
            Button btnEdit;
            Button btnDelete;

            ViewHolder(View view) {
                super(view);
                textIcon = view.findViewById(R.id.text_product_icon);
                textName = view.findViewById(R.id.text_product_name);
                textPrice = view.findViewById(R.id.text_product_price);
                btnEdit = view.findViewById(R.id.btn_edit);
                btnDelete = view.findViewById(R.id.btn_delete);
            }
        }
    }

    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private String[] icons;
        private OnIconSelectedListener listener;

        public IconAdapter(String[] icons, OnIconSelectedListener listener) {
            this.icons = icons;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String icon = icons[position];
            holder.textIcon.setText(icon);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIconSelected(icon);
                }
            });
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textIcon;

            ViewHolder(View view) {
                super(view);
                textIcon = view.findViewById(R.id.text_icon);
            }
        }
    }
}
