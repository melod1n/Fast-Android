package ru.stwtforever.fast.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import ru.stwtforever.fast.MainActivity;
import ru.stwtforever.fast.R;

public class FragmentNavDrawer extends BottomSheetDialogFragment {

    private int selectedId;

    public static FragmentNavDrawer display(FragmentManager manager, int selectedId) {
        FragmentNavDrawer drawer = new FragmentNavDrawer();
        drawer.selectedId = selectedId;
        drawer.show(manager, "");
        return drawer;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottomsheet_navdrawer, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        NavigationView navigationView = getView().findViewById(R.id.navigation_drawer);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).itemClick(item);
                return true;
            }
        });

        if (selectedId == -1) return;
        navigationView.setCheckedItem(selectedId);
    }
}