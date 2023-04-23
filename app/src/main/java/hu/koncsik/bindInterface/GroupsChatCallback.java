package hu.koncsik.bindInterface;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

public interface GroupsChatCallback {
    MutableLiveData<ArrayList<String>> getSelectedEmailsLiveData();
}

