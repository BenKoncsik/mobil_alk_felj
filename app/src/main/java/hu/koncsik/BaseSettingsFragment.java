package hu.koncsik;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;

import hu.koncsik.bindInterface.GroupsChatCallback;


public class BaseSettingsFragment extends Fragment {

    private static final String LOG_TAG = BaseSettingsFragment.class.toString();
    private MutableLiveData<ArrayList<String>> selectedEmailsLiveData;
    private String groupName;

    public BaseSettingsFragment() {
        this.groupName = "";
    }

    public BaseSettingsFragment(String name) {
        this.groupName = name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base_settings, container, false);
        EditText groupNameEditText = view.findViewById(R.id.group_name);
        groupNameEditText.setText(groupName);
        if(groupName.equals("")) {
            view.findViewById(R.id.createGroup).setVisibility(View.VISIBLE);
            view.findViewById(R.id.saveGroup).setVisibility(View.GONE);
        }else {
            view.findViewById(R.id.createGroup).setVisibility(View.GONE);
            view.findViewById(R.id.saveGroup).setVisibility(View.VISIBLE);
        }
        return view;
    }



}