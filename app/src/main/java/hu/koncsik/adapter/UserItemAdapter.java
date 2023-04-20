package hu.koncsik.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hu.koncsik.Chat;
import hu.koncsik.R;

public class UserItemAdapter extends RecyclerView.Adapter<UserItemAdapter.ViewHolder> implements Filterable {

    private static final String LOG_TAG = UserItemAdapter.class.toString();
    private ArrayList<UserItem> mUserData;
    private ArrayList<UserItem> mUserDataAll;
    private Context mContext;
    private int lastPosition = -1;

    public UserItemAdapter(Context context, ArrayList<UserItem> itemsData) {
        this.mUserData = itemsData;
        this.mUserDataAll = itemsData;
        this.mContext = context;
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
            ArrayList<UserItem> filteredList = new ArrayList<>();
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
                }

                results.count = filteredList.size();
                results.values = filteredList;
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
        public ViewHolder(View itemView) {
            super(itemView);
            // Initialize the views.
            userName = itemView.findViewById(R.id.name);
            userLastActive = itemView.findViewById(R.id.last_active);
            userImage = itemView.findViewById(R.id.itemImage);

            mFirestore = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            user = FirebaseAuth.getInstance().getCurrentUser();
            mItems = mFirestore.collection("messages");


            itemView.findViewById(R.id.start_message).setOnClickListener(view -> mItems
                    .whereEqualTo("group", false)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<ChatItem> filteredCheatItems = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ChatItem cheatItem = document.toObject(ChatItem.class);
                                List<String> members = cheatItem.getMembers();
                                if (members.size() == 2 && members.contains(user.getEmail()) && members.contains(userEmail)){
                                    Log.d(LOG_TAG, "Find Message: " + cheatItem.getId());
                                    filteredCheatItems.add(cheatItem);
                                    Intent intent = new Intent(mContext, Chat.class);
                                    intent.putExtra("cheatItem", cheatItem);
                                    intent.putExtra("id", document.getId());
                                    mContext.startActivity(intent);
                                }
                            }
                            if(filteredCheatItems.isEmpty()){
                                ChatItem newCheatItem = new ChatItem(user.getEmail(), userEmail);
                                mItems.add(newCheatItem).addOnSuccessListener(documentReference -> {
                                    newCheatItem.setId(documentReference.getId());
                                    Intent intent = new Intent(mContext, Chat.class);
                                    intent.putExtra("cheatItem", newCheatItem);
                                    intent.putExtra("id", documentReference.getId());
                                    mContext.startActivity(intent);
                                });
                            }
                        } else {
                            Log.d(LOG_TAG, "Error getting documents: ", task.getException());
                        }
                    }));
        }


        void bindTo(UserItem currentItem) {
            userName.setText(currentItem.getName());
            userLastActive.setText(currentItem.getLastActive().toString());
            userEmail = currentItem.getEmail();

            // Load the images into the ImageView using the Glide library.
//            Glide.with(mContext).load(currentItem.getImageResource()).into(mItemImage);
        }
    }
}

