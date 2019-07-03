package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public abstract class RecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    protected Context context;
    LayoutInflater inflater;
    private ArrayList<T> values;
    private ArrayList<T> cleanValues;
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
        return getValues() == null ? 0 : getValues().size();
    }

    public void insert(ArrayList<T> items) {
        getValues().addAll(items);
    }

    public T getLastItem() {
        if (getItemCount() == 0) return getItem(0);
        return getItem(getItemCount() - 1);
    }

    public T getFirstItem() {
        return getItem(0);
    }

    public int getPosition(T item) {
        return getValues().indexOf(item);
    }

    public void changeItems(ArrayList<T> items) {
        this.values.clear();
        notifyDataSetChanged();
        this.values = new ArrayList<>(items);
    }

    public void add(int index, T item) {
        getValues().add(index, item);
    }

    public void add(T item) {
        getValues().add(item);
    }

    public void remove(int position) {
        getValues().remove(position);
    }

    public void clear() {
        getValues().clear();
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

        notifyItemRangeChanged(0, getItemCount());
    }

    protected boolean onQueryItem(T item, String lowerQuery) {
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

    Drawable getDrawable(int res) {
        return ContextCompat.getDrawable(context, res);
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public int getLastPosition() {
        return getItemCount() - 1;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View v, int position);
    }

}