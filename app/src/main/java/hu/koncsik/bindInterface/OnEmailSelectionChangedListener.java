package hu.koncsik.bindInterface;

import java.util.HashSet;
import java.util.List;

public interface OnEmailSelectionChangedListener {
    void onEmailSelectionChanged(HashSet<String> selectedEmails);
}
