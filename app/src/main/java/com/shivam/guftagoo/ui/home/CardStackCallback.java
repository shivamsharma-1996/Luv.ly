package com.shivam.guftagoo.ui.home;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

import com.shivam.guftagoo.models.ItemModel;
import com.shivam.guftagoo.models.User;

public class CardStackCallback extends DiffUtil.Callback {

    private List<User> old, baru;

    public CardStackCallback(List<User> old, List<User> baru) {
        this.old = old;
        this.baru = baru;
    }

    @Override
    public int getOldListSize() {
        return old.size();
    }

    @Override
    public int getNewListSize() {
        return baru.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return old.get(oldItemPosition).getUid().equals(baru.get(newItemPosition).getUid());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return old.get(oldItemPosition) == baru.get(newItemPosition);
    }
}
