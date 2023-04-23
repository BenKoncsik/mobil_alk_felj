package hu.koncsik.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import hu.koncsik.HomeActivity;
import hu.koncsik.R;
import hu.koncsik.bindInterface.OnEmailSelectionChangedListener;


public class GroupItemAdapter extends RecyclerView.Adapter<GroupItemAdapter.ViewHolder> implements Filterable {
    private static final String LOG_TAG = GroupItemAdapter.class.toString();
    private ArrayList<ChatItem> mGroupData;
    private ArrayList<ChatItem> mGroupDataAll;
    private Context mContext;
    private int lastPosition = -1;
    private boolean isGroup = false;
    private List<String> selectedEmails = new ArrayList<>();
    private OnEmailSelectionChangedListener listener;

    public GroupItemAdapter(Context context, ArrayList<ChatItem> itemsData) {
        this.mGroupData = itemsData;
        this.mGroupDataAll = itemsData;
        this.mContext = context;
    }

    public GroupItemAdapter(Context context, ArrayList<ChatItem> itemsData, OnEmailSelectionChangedListener listener) {
        this.mGroupData = itemsData;
        this.mGroupDataAll = itemsData;
        this.mContext = context;
        this.listener = listener;
    }
    public void updateGroups(ArrayList<ChatItem> userItemList) {
        mGroupData = userItemList;
        notifyDataSetChanged();
    }
   @NonNull
    @Override
    public GroupItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupItemAdapter.ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.activity_group_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupItemAdapter.ViewHolder holder, int position) {
        ChatItem currentItem = mGroupData.get(position);
        holder.bindTo(currentItem);
        if(holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return mGroupData.size();
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            HashSet<ChatItem> filteredList = new HashSet<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0) {
                results.count = mGroupDataAll.size();
                results.values = mGroupDataAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for(ChatItem item : mGroupDataAll) {
                    String size = String.valueOf(item.getMembers().size());
                    if(item.getName().toLowerCase().contains(filterPattern))filteredList.add(item);
                    if(size.contains(filterPattern)) filteredList.add(item);
                    for (String email: item.getMembers()){
                        if(email.toLowerCase().contains(filterPattern)) filteredList.add(item);
                    }

                }
                results.count = filteredList.size();
                results.values = new ArrayList<>(filteredList);
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mGroupData = (ArrayList)filterResults.values;
            notifyDataSetChanged();
        }
    };
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView groupName;
        private TextView members;
        private String id;


        public ViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.name);
            members = itemView.findViewById(R.id.members);


            mFirestore = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            user = FirebaseAuth.getInstance().getCurrentUser();
            mItems = mFirestore.collection("messages");


            itemView.findViewById(R.id.start_message).setOnClickListener(view -> mItems
                    .whereArrayContains("members", user.getEmail())
                    .whereEqualTo("id", id)
                    .whereEqualTo("group", true)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<ChatItem> filteredCheatItems = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ChatItem cheatItem = document.toObject(ChatItem.class);
                                if (cheatItem.getId().equals(id) && cheatItem.getMembers().contains(user.getEmail())) {
                                    Log.d(LOG_TAG, "Find Group message: " + cheatItem.getId());
                                    filteredCheatItems.add(cheatItem);
                                    ((HomeActivity) mContext).startChatFragment(cheatItem, document.getId());
                                }
                            }
                        } else {
                            Log.d(LOG_TAG, "Error getting documents: ", task.getException());
                        }
                    }));
        }

        public void bindTo(ChatItem currentItem) {
            groupName.setText(currentItem.getName());
            members.setText(currentItem.getMembers().size() + " members");
            id = currentItem.getId();

        }
    }
 }

