package com.habitrpg.android.habitica.ui.fragments.inventory;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.events.ContentReloadedEvent;
import com.habitrpg.android.habitica.events.ReloadContentEvent;
import com.habitrpg.android.habitica.ui.DividerItemDecoration;
import com.habitrpg.android.habitica.ui.activities.MainActivity;
import com.habitrpg.android.habitica.ui.adapter.inventory.ItemRecyclerAdapter;
import com.habitrpg.android.habitica.ui.adapter.inventory.StableRecyclerAdapter;
import com.habitrpg.android.habitica.ui.fragments.BaseFragment;
import com.habitrpg.android.habitica.ui.helpers.MarginDecoration;
import com.habitrpg.android.habitica.ui.helpers.RecyclerViewEmptySupport;
import com.magicmicky.habitrpgwrapper.lib.models.HabitRPGUser;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.Animal;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.Egg;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.Food;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.HatchingPotion;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.Item;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.Mount;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.Pet;
import com.magicmicky.habitrpgwrapper.lib.models.inventory.QuestContent;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.From;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.Where;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;

public class StableRecyclerFragment extends BaseFragment {
    @Bind(R.id.recyclerView)
    public RecyclerViewEmptySupport recyclerView;

    @Bind(R.id.empty_view)
    public TextView emptyView;

    public StableRecyclerAdapter adapter;
    public String itemType;
    public String itemTypeText;
    public HabitRPGUser user;
    private static final String ITEM_TYPE_KEY = "CLASS_TYPE_KEY";
    GridLayoutManager layoutManager = null;

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_recyclerview, container, false);

            ButterKnife.bind(this, view);

            recyclerView.setEmptyView(emptyView);
            emptyView.setText(getString(R.string.empty_items, itemTypeText));

            android.support.v4.app.FragmentActivity context = getActivity();

            layoutManager = new GridLayoutManager(getActivity(), 2);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (adapter.getItemViewType(position) == 0) {
                        return layoutManager.getSpanCount();
                    } else {
                        return 1;
                    }
                }
            });
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new MarginDecoration(getActivity()));

            adapter = (StableRecyclerAdapter)recyclerView.getAdapter();
            if (adapter == null) {
                adapter = new StableRecyclerAdapter();
                adapter.activity = (MainActivity)this.getActivity();
                adapter.itemType = this.itemType;
                recyclerView.setAdapter(adapter);
                this.loadItems();

            }
        }

        if (savedInstanceState != null){
            this.itemType = savedInstanceState.getString(ITEM_TYPE_KEY, "");
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final View finalView = view;
        finalView.post(() -> setGridSpanCount(finalView.getWidth()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ITEM_TYPE_KEY, this.itemType);
    }



    private void setGridSpanCount(int width) {
        float itemWidth;
        itemWidth = getContext().getResources().getDimension(R.dimen.pet_width);

        int spanCount = (int) (width / itemWidth);
        if (spanCount == 0) {
            spanCount = 1;
        }
        layoutManager.setSpanCount(spanCount);
    }

    private void loadItems() {
        Runnable itemsRunnable = () -> {
            List<Object> items = new ArrayList<>();

            Where<? extends Animal> query = null;
            switch (itemType) {
                case "pets":
                    query = new Select().from(Pet.class).orderBy(true, "animalGroup", "animal", "color");
                    break;
                case "mounts":
                    query = new Select().from(Mount.class).orderBy(true, "animalGroup", "animal", "color");
                    break;
            }
            if (query == null) {
                return;
            }
            List<? extends Animal> unsortedAnimals = query.queryList();
            if (unsortedAnimals.size() == 0) {
                ReloadContentEvent event = new ReloadContentEvent();
                EventBus.getDefault().post(event);
                return;
            }
            String lastSectionTitle = "";

            Animal lastAnimal = unsortedAnimals.get(0);
            for (Animal animal : unsortedAnimals) {
                if (!animal.getAnimal().equals(lastAnimal.getAnimal())) {
                    if (!((lastAnimal.getAnimalGroup().equals("premiumPets") || lastAnimal.getAnimalGroup().equals("specialPets")
                            || lastAnimal.getAnimalGroup().equals("specialMounts"))
                            && lastAnimal.getNumberOwned() == 0)) {
                        items.add(lastAnimal);
                    }
                    lastAnimal = animal;
                }
                if (!animal.getAnimalGroup().equals(lastSectionTitle)) {
                    if (items.size() > 0 && items.get(items.size()-1).getClass().equals(String.class)) {
                        items.remove(items.size()-1);
                    }
                    items.add(animal.getAnimalGroup());
                    lastSectionTitle = animal.getAnimalGroup();
                }
                switch (itemType) {
                    case "pets":
                        if (user.getItems().getPets() != null && user.getItems().getPets().containsKey(animal.getKey()) && user.getItems().getPets().get(animal.getKey()) != null) {
                            if (lastAnimal.getNumberOwned() == 0) {
                                lastAnimal.setColor(animal.getColor());
                            }
                            lastAnimal.setNumberOwned(lastAnimal.getNumberOwned() + 1);
                        }
                        break;
                    case "mounts":
                        if (user.getItems().getMounts() != null && user.getItems().getMounts().containsKey(animal.getKey()) && user.getItems().getMounts().get(animal.getKey()) != null) {
                            if (lastAnimal.getNumberOwned() == 0) {
                                lastAnimal.setColor(animal.getColor());
                            }
                            lastAnimal.setNumberOwned(lastAnimal.getNumberOwned() + 1);
                        }
                        break;
                }
            }
            adapter.setItemList(items);
        };
        itemsRunnable.run();
    }

    @Subscribe
    public void reloadedContent(ContentReloadedEvent event) {
        this.loadItems();
    }
}
