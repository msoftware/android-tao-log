/**
 * ****************************************************************************
 * Copyright (c) 2010 Alexandr Tsvetkov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p/>
 * Contributors:
 * Alexandr Tsvetkov - initial API and implementation
 * <p/>
 * Project:
 * TAO Core
 * <p/>
 * License agreement:
 * <p/>
 * 1. This code is published AS IS. Author is not responsible for any damage that can be
 * caused by any application that uses this code.
 * 2. Author does not give a garantee, that this code is error free.
 * 3. This code can be used in NON-COMMERCIAL applications AS IS without any special
 * permission from author.
 * 4. This code can be modified without any special permission from author IF AND OFormat.NLY IF
 * this license agreement will remain unchanged.
 * ****************************************************************************
 */
package ua.at.tsvetkov.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended logger. Allows you to automatically adequately logged class, method and line call in the log. Makes it easy to write logs. For
 * example Log.v("Boo") will in the log some the record: 04-04 08:29:40.336: V > SomeClass: someMethod: 286 Boo
 *
 * @author A.Tsvetkov 2010 http://tsvetkov.at.ua mailto:al@ukr.net
 */
public class Log {

    static volatile boolean isDisabled = false;
    static volatile boolean isLogOutlined = true;
    static volatile boolean isAlignNewLines = false;
    private static final String FRAGMENT_STACK = "FRAGMENT STACK [";
    private static volatile Application.ActivityLifecycleCallbacks activityLifecycleCallback = null;
    private static volatile HashMap<String, FragmentManager.FragmentLifecycleCallbacks> fragmentLifecycleCallbacks = new HashMap<>();
    private static volatile HashMap<String, android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks> supportFragmentLifecycleCallbacks = new HashMap<>();

    private Log() {
    }

    /**
     * Is print a log string in new lines with spaces (as in AndroidStudio before 3.1). False by default
     *
     * @return
     */
    public static boolean isAlignNewLines() {
        return isAlignNewLines;
    }

    /**
     * Set to print a log string  in new lines with spaces (as in AndroidStudio before 3.1). False by default
     *
     * @param isArrangeNewLines
     */
    public static void setAlignNewLines(boolean isArrangeNewLines) {
        Log.isAlignNewLines = isArrangeNewLines;
    }

    /**
     * Added auto log messages for activity lifecycle and fragment stack events.
     *
     * @param application the application instance
     */
    public static void enableComponentsChangesLogging(@NonNull Application application) {
        enableActivityLifecycleLogger(application, true);
    }

    /**
     * Added auto log messages for activity lifecycle.
     *
     * @param application the application instance
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void enableActivityLifecycleLogger(@NonNull Application application) {
        enableActivityLifecycleLogger(application, false);
    }

    /**
     * Added auto log messages for activity lifecycle.
     *
     * @param application            the application instance
     * @param isAttachFragmentLogger attach fragment stack changes logger
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void enableActivityLifecycleLogger(@NonNull Application application, final boolean isAttachFragmentLogger) {
        if (application == null) {
            Log.w("Can't enable Activity auto logger, application == null");
        }
        if (isDisabled) {
            return;
        }
        if (activityLifecycleCallback == null) {
            activityLifecycleCallback = new Application.ActivityLifecycleCallbacks() {

                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    printActivityCallMethod(activity);
                    if (isAttachFragmentLogger) {
                        enableFragmentStackChangesLogger(activity);
                    }
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    printActivityCallMethod(activity);
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    printActivityCallMethod(activity);
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    printActivityCallMethod(activity);
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    printActivityCallMethod(activity);
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    printActivityCallMethod(activity);
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    printActivityCallMethod(activity);
                    if (isAttachFragmentLogger) {
                        disableFragmentStackChangesLogger(activity);
                    }
                }

                private void printActivityCallMethod(Activity activity) {
                    android.util.Log.v(Format.getActivityTag(activity), Format.getActivityMethodInfo(activity));
                }

            };
        }

        application.registerActivityLifecycleCallbacks(activityLifecycleCallback);
    }

    /**
     * Disable auto log messages for activity lifecycle and fragment stack events.
     *
     * @param application the application instance
     */
    public static void disableComponentsChangesLogging(@NonNull Application application) {
        disableActivityLifecycleLogger(application);
    }

    /**
     * Disabled auto log messages for activity lifecycle.
     *
     * @param application the application instance
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void disableActivityLifecycleLogger(@NonNull Application application) {
        if (isDisabled) {
            return;
        }
        if (application == null) {
            Log.w("Can't disable Activity auto logger, application=null");
        } else {
            application.unregisterActivityLifecycleCallbacks(activityLifecycleCallback);
        }
    }

    /**
     * Enabled auto log fragment stack changes.
     *
     * @param activity
     */
    public static void enableFragmentStackChangesLogger(@NonNull Activity activity) {
        if (isDisabled) {
            if (activity instanceof AppCompatActivity) {
                android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks callback = createSupportFragmentLifecycleCallbacks();
                supportFragmentLifecycleCallbacks.put(activity.toString(), callback);
                ((AppCompatActivity) activity).getSupportFragmentManager().registerFragmentLifecycleCallbacks(callback, true);
                Log.i("SupportFragment Lifecycle Logger attached");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                FragmentManager.FragmentLifecycleCallbacks callback = createFragmentLifecycleCallbacks();
                fragmentLifecycleCallbacks.put(activity.toString(), callback);
                activity.getFragmentManager().registerFragmentLifecycleCallbacks(callback, true);
                Log.i("Fragment Lifecycle Logger attached");
            } else {
                Log.w("Fragment Lifecycle Logger requires API level 26");
            }
        }
    }

    /**
     * Disabled auto log fragment stack changes.
     *
     * @param activity
     */
    public static void disableFragmentStackChangesLogger(@NonNull Activity activity) {
        if (isDisabled) {
            if (activity instanceof AppCompatActivity) {
                android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks callback = supportFragmentLifecycleCallbacks.get(activity.toString());
                ((AppCompatActivity) activity).getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(callback);
                supportFragmentLifecycleCallbacks.remove(activity.toString());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                FragmentManager.FragmentLifecycleCallbacks callback = fragmentLifecycleCallbacks.get(activity.toString());
                activity.getFragmentManager().unregisterFragmentLifecycleCallbacks(callback);
                fragmentLifecycleCallbacks.remove(activity.toString());
            }
            Log.i("Fragment Lifecycle Logger detached");
        }
    }

    /**
     * Create the line boundaries of the log. True by default
     *
     * @param isLogOutlined
     */
    public static void setLogOutlined(boolean isLogOutlined) {
        Log.isLogOutlined = isLogOutlined;
    }


    /**
     * Is logs disabled
     *
     * @return is disabled
     */
    public static boolean isDisabled() {
        return isDisabled;
    }

    /**
     * Set logs disabled or enabled
     *
     * @param isDisabled is disabled
     */
    public static void setDisabled(boolean isDisabled) {
        Log.isDisabled = isDisabled;
    }

    /**
     * Set stamp for mark log. You can add a stamp which are awesome for binding the commits/build time to your logs among other things.
     *
     * @param stamp
     */
    public static void setStamp(String stamp) {
        Format.stamp = stamp;
    }

    /**
     * Send a VERBOSE log message.
     *
     * @param message The message you would like logged.
     */
    public static void v(String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(message));
    }

    /**
     * Send a DEBUG log message.
     *
     * @param message The message you would like logged.
     */
    public static void d(String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.d(Format.getTag(), Format.getFormattedMessage(message));
    }

    /**
     * Send a INFO log message.
     *
     * @param message The message you would like logged.
     */
    public static void i(String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(message));
    }

    /**
     * Send a WARN log message.
     *
     * @param message The message you would like logged.
     */
    public static void w(String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.w(Format.getTag(), Format.getFormattedMessage(message));
    }

    /**
     * Send a ERROR log message.
     *
     * @param message The message you would like logged.
     */
    public static void e(String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.getTag(), Format.getFormattedMessage(message));
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen. The error will always be logged at level ASSERT with the call
     * stack. Depending on system configuration, a report may be added to the DropBoxManager and/or the process may be terminated immediately
     * with an error dialog.
     *
     * @param message The message you would like logged.
     */
    public static void wtf(String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.wtf(Format.getTag(), Format.getFormattedMessage(message));
    }

    // ==========================================================

    /**
     * Send a VERBOSE log message and log the throwable.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void v(String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.v(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a DEBUG log message and log the throwable.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void d(String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.d(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a INFO log message and log the throwable.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void i(String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a WARN log message and log the throwable.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void w(String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.w(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a ERROR log message and log the throwable.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void e(String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a ERROR log message and log the throwable. RuntimeException is not handled.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void rt(String message, Throwable tr) {
        if (tr instanceof RuntimeException) {
            throw (RuntimeException) tr;
        }
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    /**
     * What a Terrible Failure: Report an throwable that should never happen. Similar to wtf(String, Throwable), with a message as well.
     *
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void wtf(String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.wtf(Format.getTag(), Format.getFormattedThrowable(message, tr));
    }

    // ==========================================================

    /**
     * Send a VERBOSE log the throwable.
     *
     * @param tr An throwable to log
     */
    public static void v(Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.v(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    /**
     * Send a DEBUG log the throwable.
     *
     * @param tr An throwable to log
     */
    public static void d(Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.d(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    /**
     * Send a INFO log the throwable.
     *
     * @param tr An throwable to log
     */
    public static void i(Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    /**
     * Send a WARN log the throwable.
     *
     * @param tr An throwable to log
     */
    public static void w(Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.w(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    /**
     * Send a ERROR log the throwable.
     *
     * @param tr An throwable to log
     */
    public static void e(Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    /**
     * Send a ERROR log the throwable. RuntimeException is not handled.
     *
     * @param tr An throwable to log
     */
    public static void rt(Throwable tr) {
        if (tr instanceof RuntimeException) {
            throw (RuntimeException) tr;
        }
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    /**
     * What a Terrible Failure: Report an throwable that should never happen. Similar to wtf(String, Throwable), with a message as well.
     *
     * @param tr An throwable to log
     */
    public static void wtf(Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.wtf(Format.getTag(), Format.getFormattedThrowable(tr));
    }

    // ==========================================================

    /**
     * Send a <b>VERBOSE</b> log message. Using when you extend any Class and wont to receive full info in LogCat tag. Usually you can use
     * "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumberClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     */
    public static void v(Object obj, String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.v(Format.gatExtendedTag(obj), Format.getFormattedMessage(message));
    }

    /**
     * Send a <b>DEBUG</b> log message. Using when you extend any Class and wont to receive full info in LogCat tag. Usually you can use
     * "this" in "objl" parameter. As result you receive tag string "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     */
    public static void d(Object obj, String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.d(Format.gatExtendedTag(obj), Format.getFormattedMessage(message));
    }

    /**
     * Send a <b>INFO</b> log message. Using when you extend any Class and wont to receive full info in LogCat tag. Usually you can use
     * "this" in "objl" parameter. As result you receive tag string "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     */
    public static void i(Object obj, String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.gatExtendedTag(obj), Format.getFormattedMessage(message));
    }

    /**
     * Send a <b>WARN</b> log message. Using when you extend any Class and wont to receive full info in LogCat tag. Usually you can use
     * "this" in "objl" parameter. As result you receive tag string "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     */
    public static void w(Object obj, String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.w(Format.gatExtendedTag(obj), Format.getFormattedMessage(message));
    }

    /**
     * Send a <b>ERROR</b> log message. Using when you extend any Class and wont to receive full info in LogCat tag. Usually you can use
     * "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     */
    public static void e(Object obj, String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.gatExtendedTag(obj), Format.getFormattedMessage(message));
    }

    /**
     * Send a <b>What a Terrible Failure: Report a condition that should never happen</b> log message. Using when you extend any Class and
     * wont to receive full info in LogCat tag. Usually you can use "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     */
    public static void wtf(Object obj, String message) {
        if (isDisabled) {
            return;
        }
        android.util.Log.wtf(Format.gatExtendedTag(obj), Format.getFormattedMessage(message));
    }

    // ==========================================================

    /**
     * Send a <b>VERBOSE</b> log message and log the throwable. Using when you extend any Class and wont to receive full info in LogCat tag.
     * Usually you can use "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void v(Object obj, String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.v(Format.gatExtendedTag(obj), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a <b>DEBUG</b> log message and log the throwable. Using when you extend any Class and wont to receive full info in LogCat tag.
     * Usually you can use "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void d(Object obj, String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.d(Format.gatExtendedTag(obj), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a <b>INFO</b> log message and log the throwable. Using when you extend any Class and wont to receive full info in LogCat tag.
     * Usually you can use "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void i(Object obj, String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.gatExtendedTag(obj), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a <b>WARN</b> log message and log the throwable. Using when you extend any Class and wont to receive full info in LogCat tag.
     * Usually you can use "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param message The message you would like logged.
     * @param tr      An throwable to log
     */
    public static void w(Object obj, String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.w(Format.gatExtendedTag(obj), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a <b>ERROR</b> log message and log the throwable. Using when you extend any Class and wont to receive full info in LogCat tag.
     * Usually you can use "this" in "objl" parameter. As result you receive tag string
     * "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param tr      An throwable to log
     * @param message The message you would like logged.
     */
    public static void e(Object obj, String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.e(Format.gatExtendedTag(obj), Format.getFormattedThrowable(message, tr));
    }

    /**
     * Send a <b>What a Terrible Failure: Report a condition that should never happen</b> log message and log the throwable. Using when you
     * extend any Class and wont to receive full info in LogCat tag. Usually you can use "this" in "objl" parameter. As result you receive tag
     * string "<b>(Called Main Class) LoggedClass:MethodInLoggedClass:lineNumber</b>"
     *
     * @param obj     main class
     * @param tr      An throwable to log
     * @param message The message you would like logged.
     */
    public static void wtf(Object obj, String message, Throwable tr) {
        if (isDisabled) {
            return;
        }
        android.util.Log.wtf(Format.gatExtendedTag(obj), Format.getFormattedThrowable(message, tr));
    }

    // =========================== Collections, arrays and objects ===============================

    /**
     * Logged String representation of map. Each item in new line.
     *
     * @param map a Map
     */
    public static void map(Map<?, ?> map) {
        map(map, "Map");
    }

    /**
     * Logged String representation of map. Each item in new line.
     *
     * @param map a Map
     */
    public static void map(Map<?, ?> map, String title) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.map(map), title));
    }

    /**
     * Logged String representation of list. Each item in new line.
     *
     * @param list a List
     */
    public static void list(List<?> list) {
        list(list, "List");
    }

    /**
     * Logged String representation of list. Each item in new line.
     *
     * @param list a List
     */
    public static void list(List<?> list, String title) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.list(list), title));
    }

    /**
     * Logged String representation of Objects array. Each item in new line.
     *
     * @param array an array
     */
    public static <T> void array(T[] array) {
        array(array, Format.ARRAY);
    }

    /**
     * Logged String representation of Objects array. Each item in new line.
     *
     * @param array an array
     */
    public static <T> void array(T[] array, String title) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), title));
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(int[] array) {
        array(array, Format.ARRAY);
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(int[] array, String title) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), title));
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(float[] array) {
        array(array, Format.ARRAY);
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(float[] array, String title) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), title));
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(boolean[] array) {
        array(array, Format.ARRAY);
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(boolean[] array, String title) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), title));
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(char[] array) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), Format.ARRAY));
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(double[] array) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), Format.ARRAY));
    }

    /**
     * Logged String representation of array.
     *
     * @param array an array
     */
    public static void array(long[] array) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.array(array), Format.ARRAY));
    }

    /**
     * Logged String representation of class.
     *
     * @param obj a class for representation
     */
    public static void objl(Object obj) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.objl(obj), obj.getClass().getSimpleName()));
    }

    /**
     * Logged String representation of Object. Each field in new line.
     *
     * @param obj a class for representation
     */
    public static void objn(Object obj) {
        if (isDisabled) {
            return;
        }
        android.util.Log.i(Format.getTag(), Format.getFormattedMessage(Format.objl(obj), obj.getClass().getSimpleName()));
    }

    /**
     * Logged readable representation of bytes array data like 0F CD AD.... Each countPerLine bytes will print in new line
     *
     * @param data         your bytes array data
     * @param countPerLine count byte per line
     */
    public static void hex(byte[] data, int countPerLine) {
        Log.i(Format.hex(data, countPerLine));
    }

    /**
     * Logged readable representation of bytes array data like 0F CD AD....
     *
     * @param data your bytes array data
     */
    public static void hex(byte[] data) {
        Log.i(Format.hex(data));
    }

    /**
     * Logged readable representation of xml with indentation 2
     *
     * @param xmlStr your xml data
     */
    public static void xml(String xmlStr) {
        Log.i(Format.xml(xmlStr));
    }

    /**
     * Logged readable representation of xml
     *
     * @param xmlStr      your xml data
     * @param indentation xml identetion
     */
    public static void xml(String xmlStr, int indentation) {
        Log.i(Format.xml(xmlStr, indentation));
    }


    // =========================== Thread and stack trace ===============================

    /**
     * Logged the current Thread info
     */
    public static void threadInfo() {
        if (isDisabled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Format.addThreadInfo(sb, Thread.currentThread());
        sb.append(Format.NL);
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(sb.toString()));
    }

    /**
     * Logged the current Thread info and an throwable
     *
     * @param throwable An throwable to log
     */
    public static void threadInfo(Throwable throwable) {
        if (isDisabled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Format.addThreadInfo(sb, Thread.currentThread());
        sb.append(Format.NL);
        Format.addStackTrace(sb, throwable);
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(sb.toString()));
    }

    /**
     * Logged the current Thread info and a message
     */
    public static void threadInfo(@Nullable String message) {
        if (isDisabled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Format.addThreadInfo(sb, Thread.currentThread());
        sb.append(Format.NL);
        Format.addMessage(sb, message);
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(sb.toString()));
    }

    /**
     * Logged the current Thread info and a message and an throwable
     *
     * @param message   The message you would like logged.
     * @param throwable An throwable to log
     */
    public static void threadInfo(String message, Throwable throwable) {
        if (isDisabled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Format.addThreadInfo(sb, Thread.currentThread());
        sb.append(Format.NL);
        Format.addMessage(sb, message);
        Format.addStackTrace(sb, throwable);
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(sb.toString()));
    }

    /**
     * Logged the current Thread info and a message and an throwable
     *
     * @param thread    for Logged info.
     * @param throwable An throwable to log
     */
    public static void threadInfo(Thread thread, Throwable throwable) {
        if (isDisabled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Format.addThreadInfo(sb, thread);
        sb.append(Format.NL);
        Format.addStackTrace(sb, throwable);
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(sb.toString()));
    }

    /**
     * Logged current stack trace.
     */
    public static void stackTrace() {
        stackTrace("Current stack trace:");
    }

    /**
     * Logged current stack trace with a message.
     *
     * @param message a custom message
     */
    public static void stackTrace(String message) {
        if (isDisabled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Format.addMessage(sb, message);
        Format.addStackTrace(sb, Thread.currentThread());
        android.util.Log.v(Format.getTag(), Format.getFormattedMessage(sb.toString()));
    }

    private static FragmentManager.FragmentLifecycleCallbacks createFragmentLifecycleCallbacks() {
        return new FragmentManager.FragmentLifecycleCallbacks() {

            @Override
            public void onFragmentAttached(FragmentManager fm, Fragment fr, Context context) {
                super.onFragmentAttached(fm, fr, context);
                int backStackCount = fm.getBackStackEntryCount();
                Format.printFragmentsStack(fr.getActivity().getLocalClassName(), fm, FRAGMENT_STACK + backStackCount + "]", "attached " + fr.getClass().getSimpleName(), backStackCount);
            }

            @Override
            public void onFragmentDetached(FragmentManager fm, Fragment fr) {
                super.onFragmentDetached(fm, fr);
                int backStackCount = fm.getBackStackEntryCount();
                Format.printFragmentsStack(fr.getActivity().getLocalClassName(), fm, FRAGMENT_STACK + backStackCount + "]", "detached " + fr.getClass().getSimpleName(), backStackCount);
            }

        };
    }

    private static android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks createSupportFragmentLifecycleCallbacks() {
        return new android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(android.support.v4.app.FragmentManager fm, android.support.v4.app.Fragment fr, Context context) {
                super.onFragmentAttached(fm, fr, context);
                int backStackCount = fm.getBackStackEntryCount();
                Format.printFragmentsStack(fr.getActivity().getLocalClassName(), fm, FRAGMENT_STACK + backStackCount + "]", "attached " + fr.getClass().getSimpleName(), backStackCount);
            }

            @Override
            public void onFragmentDetached(android.support.v4.app.FragmentManager fm, android.support.v4.app.Fragment fr) {
                super.onFragmentDetached(fm, fr);
                int backStackCount = fm.getBackStackEntryCount();
                Format.printFragmentsStack(fr.getActivity().getLocalClassName(), fm, FRAGMENT_STACK + backStackCount + "]", "detached " + fr.getClass().getSimpleName(), backStackCount);
            }
        };
    }


}
