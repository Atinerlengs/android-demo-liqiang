package com.freeme.filemanager.controller;

import android.view.ActionMode;

public interface IActionModeCtr {
    ActionMode getActionMode();

    void setActionMode(ActionMode actionMode);

    ActionMode startActionMode(ActionMode.Callback callback);
}
