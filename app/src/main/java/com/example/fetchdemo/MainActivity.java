package com.example.fetchdemo;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements ItemAdapter.ListNavigationListener {
    private static final String DATA_URL = "https://fetch-hiring.s3.amazonaws.com/hiring.json";

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private ProgressBar progressBar;
    private TextView errorText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView currentListText;
    private ImageButton prevListButton;
    private ImageButton nextListButton;

    private List<Integer> listIds = new ArrayList<>();
    private int currentListIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        currentListText = findViewById(R.id.currentListText);
        prevListButton = findViewById(R.id.prevListButton);
        nextListButton = findViewById(R.id.nextListButton);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new ItemAdapter();
        adapter.setListNavigationListener(this);
        recyclerView.setAdapter(adapter);

        prevListButton.setOnClickListener(v -> navigateToPreviousList());
        nextListButton.setOnClickListener(v -> navigateToNextList());

        updateNavigationControls(false);

        swipeRefreshLayout.setOnRefreshListener(this::fetchData);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );

        fetchData();
    }

    private void updateNavigationControls(boolean hasData) {
        if (!hasData) {
            prevListButton.setVisibility(View.INVISIBLE);
            nextListButton.setVisibility(View.INVISIBLE);
            currentListText.setVisibility(View.INVISIBLE);
            return;
        }

        currentListText.setVisibility(View.VISIBLE);

        if (!listIds.isEmpty()) {
            int currentListId = listIds.get(currentListIndex);
            currentListText.setText("List " + currentListId);

            prevListButton.setVisibility(View.VISIBLE);
            nextListButton.setVisibility(View.VISIBLE);
            prevListButton.setEnabled(currentListIndex > 0);
            nextListButton.setEnabled(currentListIndex < listIds.size() - 1);

            prevListButton.setAlpha(prevListButton.isEnabled() ? 1.0f : 0.5f);
            nextListButton.setAlpha(nextListButton.isEnabled() ? 1.0f : 0.5f);
        }
    }

    private void navigateToPreviousList() {
        if (currentListIndex > 0) {
            currentListIndex--;
            int listId = listIds.get(currentListIndex);
            scrollToList(listId);
            updateNavigationControls(true);
        }
    }

    private void navigateToNextList() {
        if (currentListIndex < listIds.size() - 1) {
            currentListIndex++;
            int listId = listIds.get(currentListIndex);
            scrollToList(listId);
            updateNavigationControls(true);
        }
    }

    private void scrollToList(int listId) {
        int position = adapter.getPositionForListId(listId);
        if (position != -1) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(position, 0);
            }
        }
    }

    @Override
    public void onListVisible(int listId) {
        for (int i = 0; i < listIds.size(); i++) {
            if (listIds.get(i) == listId) {
                currentListIndex = i;
                updateNavigationControls(true);
                break;
            }
        }
    }

    private void fetchData() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        errorText.setVisibility(View.GONE);

        updateNavigationControls(false);

        RequestQueue requestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                DATA_URL,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        processData(jsonArray);
                    } catch (Exception e) {
                        showError("Error Occurred, Try Again...");
                        Log.e("MainActivity", "JSON parsing error", e);
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    showError("Please check your Network Connection");
                    Log.e("MainActivity", "Volley error", error);
                }
        );
        requestQueue.add(stringRequest);
    }

    private void showError(String message) {
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void processData(JSONArray jsonArray) {
        try {
            List<Item> allItems = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    int id = obj.optInt("id", 0);
                    int listId = obj.optInt("listId", 0);

                    String name = null;
                    if (!obj.isNull("name")) {
                        name = obj.optString("name", "");
                    }

                    if (name != null && !name.isEmpty()) {
                        Item item = new Item(id, listId, name);
                        allItems.add(item);
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error processing item: " + e.getMessage());
                }
            }

            if (allItems.isEmpty()) {
                showError("No valid Items Found!");
                return;
            }

            Collections.sort(allItems, (a, b) -> {
                int listIdCompare = Integer.compare(a.getListId(), b.getListId());
                if (listIdCompare != 0) {
                    return listIdCompare;
                }

                String nameA = a.getName();
                String nameB = b.getName();

                try {
                    int numA = extractNumber(nameA);
                    int numB = extractNumber(nameB);
                    return Integer.compare(numA, numB);
                } catch (Exception e) {
                    return nameA.compareTo(nameB);
                }
            });

            Map<Integer, List<Item>> groupedItems = new TreeMap<>();
            for (Item item : allItems) {
                int listId = item.getListId();
                if (!groupedItems.containsKey(listId)) {
                    groupedItems.put(listId, new ArrayList<>());
                }

                List<Item> listItems = groupedItems.get(listId);
                if (listItems != null) {
                    listItems.add(item);
                }
            }

            listIds = new ArrayList<>(groupedItems.keySet());
            currentListIndex = 0;

            adapter.setData(groupedItems);
            updateNavigationControls(true);

        } catch (Exception e) {
            showError("Unexpected Error Occurred, Try Again!");
            Log.e("MainActivity", "Data Processing Error:", e);
        }
    }

    private int extractNumber(String name) {
        try {
            String[] parts = name.split(" ");
            if (parts.length > 1) {
                return Integer.parseInt(parts[parts.length - 1]);
            }
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}