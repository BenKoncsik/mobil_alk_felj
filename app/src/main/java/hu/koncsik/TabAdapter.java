package hu.koncsik;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import hu.koncsik.adapter.ChatItem;

public class TabAdapter extends FragmentStateAdapter {
    private ChatItem chatItem = null;
    public TabAdapter(FragmentActivity fragmentActivity, ChatItem  chatItem) {
        super(fragmentActivity);
        this.chatItem = chatItem;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        BaseSettingsFragment baseSettingsFragment;
        if(chatItem != null) baseSettingsFragment =  new BaseSettingsFragment(chatItem.getName());
        else baseSettingsFragment = new BaseSettingsFragment();

        switch (position) {
            case 0:
                return baseSettingsFragment;
            case 1:
                return new MembersFragment();
            default:
                return baseSettingsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
