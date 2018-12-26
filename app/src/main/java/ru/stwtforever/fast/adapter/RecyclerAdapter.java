package ru.stwtforever.fast.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private ArrayList<T> values;
    private ArrayList<T> cleanValues;

    protected Context context;
    LayoutInflater inflater;

    private OnItemClickListener click;
    private OnItemLongClickListener long_click;

    RecyclerAdapter(Context context, ArrayList<T> values) {
        this.context = context;
        this.values = values;

        this.inflater = LayoutInflater.from(context);
    }

    protected @ColorInt
    int getColor(int resId) {
        if (context == null) return -1;

        return context.getResources().getColor(resId);
    }

    @Nullable
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        updateListeners(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public void insert(ArrayList<T> items) {
        getValues().addAll(items);
    }

    public void changeItems(ArrayList<T> items) {
        this.values = items;
    }

    public void add(T item) {
        getValues().add(item);
    }

    public void remove(int position) {
        getValues().remove(position);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.click = l;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        this.long_click = l;
    }

    private void updateListeners(View v, final int position) {
        if (click != null) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    click.onItemClick(v, position);
                }
            });
        }

        if (long_click != null) {
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    long_click.onItemLongClick(v, position);
                    return click != null;
                }
            });
        }
    }

    public T getItem(int position) {
        return values.get(position);
    }

    public void filter(String query) {
        String lowerQuery = query.toLowerCase();

        if (cleanValues == null) {
            cleanValues = new ArrayList<>(values);
        }
        values.clear();

        if (query.isEmpty()) {
            values.addAll(cleanValues);
        } else {
            for (T value : cleanValues) {
                if (onQueryItem(value, lowerQuery)) {
                    values.add(value);
                }
            }
        }

        notifyDataSetChanged();
    }

    private boolean onQueryItem(T item, String lowerQuery) {
        return false;
    }

    public ArrayList<T> getValues() {
        return values;
    }

    public String getString(int res) {
        return context.getString(res);
    }

    public String getString(int res, Object... args) {
        return context.getString(res, args);
    }

    public Drawable getDrawable(int res) {
        return context.getDrawable(res);
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View v, int position);
    }

}