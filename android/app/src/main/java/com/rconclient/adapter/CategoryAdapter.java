package com.rconclient.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.R;
import com.rconclient.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> categories = new ArrayList<>();
    private int selectedPosition = 0;
    private OnCategoryClickListener listener;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }
    
    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }
    
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        String icon = category.getIcon() != null ? category.getIcon() : "📁";
        holder.nameText.setText(icon + " " + category.getName());
        holder.countText.setText(String.valueOf(category.getProductCount()));
        
        holder.itemView.setSelected(position == selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category, position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView countText;
        
        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_category_name);
            countText = itemView.findViewById(R.id.text_category_count);
        }
    }
}
