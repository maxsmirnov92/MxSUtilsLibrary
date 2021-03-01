package net.maxsmr.commonutils.notification;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.DrawableRes;

import org.jetbrains.annotations.NotNull;

/**
 * Created by maxsmirnov on 10.09.15.
 */
@Deprecated
public class NotificationActionInfo {

    public enum NotificationAction {
        NONE, ACTIVITY, SERVICE, BROADCAST;
    }

    @NotNull
    public final NotificationAction action;

    public final int requestCode;

    @DrawableRes
    public final int iconResId;

    public final String title;

    @NotNull
    public final Intent actionIntent;

    public final int pIntentFlag;

    public NotificationActionInfo(NotificationAction action, int requestCode, @DrawableRes int iconResId, String title, @NotNull Intent actionIntent, int pIntentFlag) {
        this.action = action == null ? NotificationAction.NONE : action;
        this.requestCode = requestCode;
        this.iconResId = iconResId;
        this.title = title;
        this.actionIntent = actionIntent;
        this.pIntentFlag = (pIntentFlag == PendingIntent.FLAG_CANCEL_CURRENT || pIntentFlag == PendingIntent.FLAG_NO_CREATE || pIntentFlag == PendingIntent.FLAG_ONE_SHOT || pIntentFlag == PendingIntent.FLAG_UPDATE_CURRENT || pIntentFlag == 0) ? pIntentFlag : 0;
    }

    @Override
    @NotNull
    public String toString() {
        return "NotificationActionInfo{" +
                "action=" + action +
                ", requestCode=" + requestCode +
                ", iconResId=" + iconResId +
                ", title='" + title + '\'' +
                ", actionIntent=" + actionIntent +
                ", pIntentFlag=" + pIntentFlag +
                '}';
    }
}
