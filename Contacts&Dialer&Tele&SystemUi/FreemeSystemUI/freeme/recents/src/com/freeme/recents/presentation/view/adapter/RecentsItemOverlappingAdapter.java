package com.freeme.recents.presentation.view.adapter;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.presentation.view.component.overlappingstackview.ChildView;
import com.freeme.recents.presentation.view.component.overlappingstackview.OverlappingStackView;
import com.freeme.recents.presentation.event.ui.LoadTaskDataEvent;
import com.freeme.recents.recentslib.data.model.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class RecentsItemOverlappingAdapter extends BaseAdapter {

    private static final String TAG = "RecentsItemOverlappingAdapter";
    private final OverlappingStackView<Task> mOverlappingStackView;
    protected ArrayList<Task> mTaskList = new ArrayList<>();

    public int[] getRemoveTaskIds() {
        int size = mTaskList.size();
        int[] taskIds = new int[size];
        int numToRemove = 0;
        for (int i = 0; i < size; i++) {
            if (!mTaskList.get(i).isLocked()) {
                taskIds[numToRemove] = mTaskList.get(i).taskId;
                numToRemove++;
            }
        }
        return Arrays.copyOfRange(taskIds, 0, numToRemove);
    }

    public Task[] getRemoveTasks() {
        final int size = mTaskList.size();
        Task[] tasks = new Task[size];
        int numToRemove = 0;
        for (int i = 0; i < size; i++) {
            if (!mTaskList.get(i).isLocked()) {
                tasks[numToRemove] = mTaskList.get(i);
                numToRemove++;
            }
        }
        return Arrays.copyOfRange(tasks, 0, numToRemove);
    }

    public void removeAllTasks() {
        Iterator<Task> iterator = mTaskList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.isLocked()) continue;
            iterator.remove();
        }
    }

    public RecentsItemOverlappingAdapter(OverlappingStackView<Task> overlappingStackView) {
        mOverlappingStackView = overlappingStackView;
    }

    @Override
    public int getCount() {
        return this.mTaskList.size();
    }

    @Override
    public Object getItem(int position) {
        return mTaskList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    public void addTask(Task task) {
        this.mTaskList.add(0, task);
    }

    public void updateThumbnail(final Task task, boolean ismultiwindowmode) {
        final WeakReference<ChildView<Task>> weakView = (WeakReference<ChildView<Task>>) task.getHolder();
        if (weakView != null && weakView.get() != null) {
            if (task.isLoaded()) {
                weakView.get().onDataLoaded(task, ((BitmapDrawable) task.getThumbnail()).getBitmap(),
                        task.getIcon(), task.getLabel(), task.isLocked(), ismultiwindowmode);

            }
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mOverlappingStackView.notifyDataSetChanged();
    }

    public ArrayList<Task> getData() {
        return mTaskList;
    }

    public void loadViewData(WeakReference<ChildView<Task>> cv, Task task , boolean ismultiwindowmode) {
        if (cv == null) {
            if (!task.isLoaded()) {
                FreemeEventBus.getDefault().send(new LoadTaskDataEvent(task));
            }
        } else {
            task.setHolder(cv);
            if (task.isLoaded()) {
                updateThumbnail(task, ismultiwindowmode);
            } else {
                FreemeEventBus.getDefault().send(new LoadTaskDataEvent(task));
            }
        }
    }

    public void unloadViewData(Task task) {
        task.setThumbnail(null);
        task.setIcon(null);
        task.setLoaded(false);
        task.setHolder(null);
    }

    public void onViewDismissed(Task task) {
        mTaskList.remove(task);
        notifyDataSetChanged();
    }

    //*/ freeme.gouzhouping, 20180327. Recents, blank view.
    public void updateTaskFromTaskList(Task task) {
        if (mTaskList == null) {
            return;
        }
        int count = mTaskList.size();
        for (int i = 0; i < count; i++) {
            if (mTaskList.get(i).taskId == task.taskId){
                mTaskList.set(i, task);
            }
        }
        notifyDataSetChanged();
    }
    //*/
}
