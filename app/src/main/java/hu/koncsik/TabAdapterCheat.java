package hu.koncsik;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabAdapterCheat extends FragmentStateAdapter {
    public TabAdapterCheat(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        HomeFragment homeFragment = new HomeFragment();
        GroupListFragment groupFragment = new GroupListFragment();
        switch (position) {
            case 0:
                return homeFragment;
            case 1:
                return groupFragment;
            default:
                return homeFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
