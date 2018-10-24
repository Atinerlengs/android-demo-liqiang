package application.android.com.zhaozehong.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import application.android.com.zhaozehong.demoapplication.R;

public class ShortcutActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shortcut);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create:
                createShortcut();
                break;
            case R.id.delete:
                deleteShortcut();
                break;
            default:
                break;
        }
    }

    private void createShortcut() {
        Intent intent = new Intent();
        intent.setClass(this, this.getClass());
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");

        Intent shortcut = new Intent("com.android.launcher.permission.INSTALL_SHORTCUT");
        shortcut.putExtra("duplicate", false);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, "AAAAA");
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher));
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);

        sendBroadcast(shortcut);

    }

    private void deleteShortcut() {
        Intent shortcut = new Intent("com.android.launcher.permission.UNINSTALL_SHORTCUT");
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, "AAAAA");

        sendBroadcast(shortcut);
    }
}
