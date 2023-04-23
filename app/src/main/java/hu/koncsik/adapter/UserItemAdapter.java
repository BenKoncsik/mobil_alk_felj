package hu.koncsik.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import hu.koncsik.Chat;
import hu.koncsik.HomeActivity;
import hu.koncsik.bindInterface.OnEmailSelectionChangedListener;
import hu.koncsik.R;
import hu.koncsik.extensions.GenerateId;

public class UserItemAdapter extends RecyclerView.Adapter<UserItemAdapter.ViewHolder> implements Filterable {

    private static final String LOG_TAG = UserItemAdapter.class.toString();
    private ArrayList<UserItem> mUserData;
    private ArrayList<UserItem> mUserDataAll;
    private Context mContext;
    private int lastPosition = -1;
    private boolean isGroup = false;
    private HashSet<String> selectedEmails = new HashSet<>();
    private OnEmailSelectionChangedListener listener;

    public UserItemAdapter(Context context, ArrayList<UserItem> itemsData, boolean isGroup) {
        this.mUserData = itemsData;
        this.mUserDataAll = itemsData;
        this.mContext = context;
        this.isGroup = isGroup;
    }

    public UserItemAdapter(Context context, ArrayList<UserItem> itemsData, boolean isGroup, OnEmailSelectionChangedListener listener) {
        this.mUserData = itemsData;
        this.mUserDataAll = itemsData;
        this.mContext = context;
        this.isGroup = isGroup;
        this.listener = listener;
    }
    public UserItemAdapter(Context context, ArrayList<UserItem> itemsData, boolean isGroup, HashSet<String> selectedEmails, OnEmailSelectionChangedListener listener) {
        this.mUserData = itemsData;
        this.mUserDataAll = itemsData;
        this.mContext = context;
        this.isGroup = isGroup;
        this.listener = listener;
        this.selectedEmails = selectedEmails;
    }
    public void updateUsers(ArrayList<UserItem> userItemList) {
        mUserData = userItemList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.activity_user_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserItemAdapter.ViewHolder holder, int position) {
        UserItem currentItem = mUserData.get(position);
        holder.bindTo(currentItem);
        if(holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return mUserData.size();
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            HashSet<UserItem> filteredList = new HashSet<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0) {
                results.count = mUserDataAll.size();
                results.values = mUserDataAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for(UserItem item : mUserDataAll) {
                    if(item.getName().toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                    if(item.getEmail().toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                }

                results.count = filteredList.size();
                results.values = new ArrayList<>(filteredList);
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mUserData = (ArrayList)filterResults.values;
            notifyDataSetChanged();
        }
    };
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView userName;
        private TextView userLastActive;
        private ImageView userImage;
        private String userEmail;
        private CheckBox activeIndicator;
        private CheckBox groupAddIndicator;

        private Button startMessage;
        private RelativeLayout layoutItem;

        public ViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.name);
            userLastActive = itemView.findViewById(R.id.last_active);
            userImage = itemView.findViewById(R.id.itemImage);
            activeIndicator = itemView.findViewById(R.id.active_indicator);
            groupAddIndicator = itemView.findViewById(R.id.group_add);
            startMessage = itemView.findViewById(R.id.start_message);
            layoutItem = itemView.findViewById(R.id.layout_item);


            mFirestore = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            user = FirebaseAuth.getInstance().getCurrentUser();
            mItems = mFirestore.collection("messages");

            if(isGroup) {
                itemView.findViewById(R.id.active_label).setVisibility(View.GONE);
                startMessage.setVisibility(View.GONE);
                activeIndicator.setVisibility(View.GONE);
                groupAddIndicator.setVisibility(View.VISIBLE);
                userImage.setVisibility(View.GONE);
            }else {
                itemView.findViewById(R.id.active_label).setVisibility(View.VISIBLE);
                startMessage.setVisibility(View.VISIBLE);
                activeIndicator.setVisibility(View.VISIBLE);
                groupAddIndicator.setVisibility(View.GONE);
            }
            ArrayList<String> usersArray = new ArrayList<>();
            usersArray.add(userEmail);
            usersArray.add(user.getEmail());
            itemView.findViewById(R.id.start_message).setOnClickListener(view -> mItems
                    .whereEqualTo("group", false)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<ChatItem> filteredCheatItems = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ChatItem cheatItem = document.toObject(ChatItem.class);
                                List<String> members = cheatItem.getMembers();
                                if (members.size() == 2 && members.contains(user.getEmail()) && members.contains(userEmail)) {
                                    Log.d(LOG_TAG, "Find Message: " + cheatItem.getId());
                                    filteredCheatItems.add(cheatItem);
                                    ((HomeActivity) mContext).startChatFragment(cheatItem, document.getId());
                                    break;
                                }
                            }

                            if (filteredCheatItems.isEmpty()) {
                                ChatItem newCheatItem = new ChatItem(user.getEmail(), userEmail, GenerateId.newId());
                                mItems.add(newCheatItem)
                                        .addOnSuccessListener(documentReference -> {
                                            ((HomeActivity) mContext).startChatFragment(newCheatItem, documentReference.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(LOG_TAG, "Error save chat-->" + e);
                                        });
                            }
                        } else {
                            Log.d(LOG_TAG, "Error getting documents: ", task.getException());
                        }
                    }));

            if(isGroup){
                layoutItem.setOnClickListener(view -> {
                    groupAddIndicator.setChecked(!groupAddIndicator.isChecked());
                    if (groupAddIndicator.isChecked()) {
                        layoutItem.setBackgroundColor(Color.GREEN);
                        selectedEmails.add(userEmail);
                    } else {
                        layoutItem.setBackgroundColor(Color.LTGRAY);
                        selectedEmails.remove(userEmail);
                    }
                    if(listener != null) listener.onEmailSelectionChanged(selectedEmails);
                });


            }
        }


        void bindTo(UserItem currentItem) {
            userName.setText(currentItem.getName());
            userLastActive.setText("Last login: " + new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(currentItem.getLastActive()));
            userEmail = currentItem.getEmail();
            activeIndicator.setChecked(currentItem.isActive());
            if(isGroup){
                userName.setTextColor(Color.BLACK);
                userLastActive.setTextColor(Color.BLACK);
                layoutItem.setBackgroundColor(Color.LTGRAY);
                groupAddIndicator.setChecked(selectedEmails.contains(currentItem.getEmail()));
                RelativeLayout layoutItem = itemView.findViewById(R.id.layout_item);
                if(groupAddIndicator.isChecked()) layoutItem.setBackgroundColor(Color.GREEN);
                else  layoutItem.setBackgroundColor(Color.WHITE);
            }

        }
    }
}

