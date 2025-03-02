package com.example.fetchdemo;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface ListNavigationListener {
        void onListVisible(int listId);
    }

    private final List<Object> combinedItems = new ArrayList<>();
    private final Map<Integer, List<Item>> originalGroupedItems = new HashMap<>();
    private final Map<Integer, Integer> listIdToPosition = new HashMap<>();

    private ListNavigationListener navigationListener;

    public void setListNavigationListener(ListNavigationListener listener) {
        this.navigationListener = listener;
    }

    public void setData(Map<Integer, List<Item>> groupedItems) {
        originalGroupedItems.clear();
        originalGroupedItems.putAll(groupedItems);
        rebuildList();
    }

    private void rebuildList() {
        combinedItems.clear();
        listIdToPosition.clear();

        for (Map.Entry<Integer, List<Item>> entry : originalGroupedItems.entrySet()) {
            Integer listId = entry.getKey();
            List<Item> items = entry.getValue();

            listIdToPosition.put(listId, combinedItems.size());

            combinedItems.add(listId);

            combinedItems.addAll(items);
        }

        notifyDataSetChanged();
    }

    public int getPositionForListId(int listId) {
        return listIdToPosition.getOrDefault(listId, -1);
    }

    public void checkVisibleListAtPosition(int position) {
        if (position >= 0 && position < combinedItems.size()) {
            Object item = combinedItems.get(position);

            // If it's a header, notify the listener
            if (item instanceof Integer) {
                Integer listId = (Integer) item;
                if (navigationListener != null) {
                    navigationListener.onListVisible(listId);
                }
            }
            else if (item instanceof Item) {
                Item dataItem = (Item) item;
                int listId = dataItem.getListId();
                if (navigationListener != null) {
                    navigationListener.onListVisible(listId);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return combinedItems.get(position) instanceof Integer ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            View headerView = inflater.inflate(R.layout.header_item, parent, false);
            return new HeaderViewHolder(headerView);
        } else {
            View itemView = inflater.inflate(R.layout.list_item, parent, false);
            return new ItemViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = combinedItems.get(position);

        if (holder instanceof HeaderViewHolder && item instanceof Integer) {
            Integer listId = (Integer) item;
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            headerHolder.headerTitle.setText("List " + listId);

            // Set the item count
            int itemCount = originalGroupedItems.get(listId).size();
            headerHolder.itemCount.setText(itemCount + " item" + (itemCount != 1 ? "s" : ""));
        } else if (holder instanceof ItemViewHolder && item instanceof Item) {
            Item dataItem = (Item) item;
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.itemTitle.setText(dataItem.getName());
            itemHolder.itemId.setText("ID: " + dataItem.getId());
        }
    }

    @Override
    public int getItemCount() {
        return combinedItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;
        TextView itemCount;

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.headerTitle);
            itemCount = itemView.findViewById(R.id.itemCount);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemTitle;
        TextView itemId;

        ItemViewHolder(View itemView) {
            super(itemView);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemId = itemView.findViewById(R.id.itemId);
        }
    }
}