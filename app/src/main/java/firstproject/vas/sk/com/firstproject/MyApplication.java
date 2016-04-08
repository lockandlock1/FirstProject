package firstproject.vas.sk.com.firstproject;

import android.app.Application;
import android.content.Context;

/**
 * Created by ListenAndRepeat on 2016. 2. 22..
 */
public class MyApplication extends Application{
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }
}
