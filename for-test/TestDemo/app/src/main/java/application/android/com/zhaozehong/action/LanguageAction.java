package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.util.Log;

import java.util.Locale;

public class LanguageAction extends Action {

    public LanguageAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Language";
    }

    @Override
    public void doAction() {
        Locale locale = mActivity.getResources().getConfiguration().locale;
        String country = locale.getLanguage();
        Log.e("zhaozehong", "[LanguageAction][doAction] " + country);
    }
}
