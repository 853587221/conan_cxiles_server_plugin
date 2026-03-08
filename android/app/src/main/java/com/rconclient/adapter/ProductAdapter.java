package com.rconclient.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rconclient.R;
import com.rconclient.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<Product> products = new ArrayList<>();
    private OnProductClickListener listener;
    private String baseUrl;
    private String username;
    
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }
    
    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.nameText.setText(product.getName());
        holder.priceText.setText(String.format("%.0f", product.getPrice()));
        
        String image = product.getImage();
        android.util.Log.d("ProductAdapter", "Product: " + product.getName() + ", Image: " + image + ", baseUrl: " + baseUrl + ", username: " + username);
        
        if (image != null && !image.isEmpty() && baseUrl != null && !baseUrl.isEmpty() && username != null && !username.isEmpty()) {
            String imageUrl = baseUrl + "/api/shop/image/" + username + "/" + image;
            android.util.Log.d("ProductAdapter", "Loading image: " + imageUrl);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_package)
                    .error(R.drawable.ic_package)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_package);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return products.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText;
        TextView priceText;
        
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_product);
            nameText = itemView.findViewById(R.id.text_product_name);
            priceText = itemView.findViewById(R.id.text_product_price);
        }
    }
}
