package net.maxsmr.commonutils.notification;

import android.content.Intent;

import androidx.annotation.DrawableRes;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Created by maxsmirnov on 10.09.15.
 */
@Deprecated
public class NotificationInfo {

    public final static int PROGRESS_NOT_SET = -1;

    public int id;

    public boolean autoCancel;

    public boolean onlyUpdate;

    public boolean ongoing;

    @Nullable
    public Intent contentIntent;

    @Nullable
    public NotificationActionInfo[] actionInfos;

    @DrawableRes
    public int iconResId;

    @Nullable
    public String tickerText;

    @Nullable
    public String contentTitle;

    @Nullable
    public String text;

    @Nullable
    public String subText;

    public int progress = PROGRESS_NOT_SET;

    @Override
    public String toString() {
        return "NotificationInfo{" +
                "id=" + id +
                ", autoCancel=" + autoCancel +
                ", onlyUpdate=" + onlyUpdate +
                ", ongoing=" + ongoing +
                ", contentIntent=" + contentIntent +
                ", iconResId=" + iconResId +
                ", tickerText='" + tickerText + '\'' +
                ", contentTitle='" + contentTitle + '\'' +
                ", text='" + text + '\'' +
                ", subText='" + subText + '\'' +
                ", progress=" + progress +
                ", actionInfos=" + Arrays.toString(actionInfos) +
                '}';
    }
}
