package com.freeme.contacts.callback;

public interface ContactChoiceCheckedIdListener {
    void checkedId(long id,boolean isMultiCheck,boolean isGroupChecked);
    void checkedGroupId(long id,boolean isGroupChecked);
    void checkedAll(boolean isChecked);
}
