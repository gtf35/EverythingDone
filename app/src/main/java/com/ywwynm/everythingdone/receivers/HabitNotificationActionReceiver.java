package com.ywwynm.everythingdone.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.Pair;

import com.ywwynm.everythingdone.App;
import com.ywwynm.everythingdone.Def;
import com.ywwynm.everythingdone.R;
import com.ywwynm.everythingdone.activities.AuthenticationActivity;
import com.ywwynm.everythingdone.activities.NoticeableNotificationActivity;
import com.ywwynm.everythingdone.activities.StartDoingActivity;
import com.ywwynm.everythingdone.database.HabitDAO;
import com.ywwynm.everythingdone.helpers.RemoteActionHelper;
import com.ywwynm.everythingdone.model.HabitReminder;
import com.ywwynm.everythingdone.model.Thing;
import com.ywwynm.everythingdone.services.DoingService;

public class HabitNotificationActionReceiver extends BroadcastReceiver {

    public static final String TAG = "HabitNotificationActionReceiver";

    public HabitNotificationActionReceiver() { }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long hrId = intent.getLongExtra(Def.Communication.KEY_ID, -1);

        long ongoingThingId = context.getSharedPreferences(
                Def.Meta.PREFERENCES_NAME, Context.MODE_PRIVATE).getLong(
                Def.Meta.KEY_ONGOING_THING_ID, -1);
        if (hrId == -1) hrId = ongoingThingId;

        if (ongoingThingId != hrId) {
            NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
            nmc.cancel((int) hrId);
        }

        int position = intent.getIntExtra(Def.Communication.KEY_POSITION, -1);
        HabitDAO habitDAO = HabitDAO.getInstance(context);
        HabitReminder habitReminder = habitDAO.getHabitReminderById(hrId);
        long id = habitReminder.getHabitId();
        for (Long dId : App.getRunningDetailActivities()) if (dId == id) {
            return;
        }

        Pair<Thing, Integer> pair = App.getThingAndPosition(context, id, position);
        Thing thing = pair.first;
        if (thing == null) {
            return;
        }
        position = pair.second;

        context.sendBroadcast(
                new Intent(NoticeableNotificationActivity.BROADCAST_ACTION_JUST_FINISH)
                        .putExtra(Def.Communication.KEY_ID, thing.getId()));

        long hrTime = intent.getLongExtra(Def.Communication.KEY_TIME, -1);
        if (action.equals(Def.Communication.NOTIFICATION_ACTION_FINISH)) {
            if (thing.isPrivate()) {
                Intent actionIntent = AuthenticationActivity.getOpenIntent(
                        context, TAG, id, position,
                        Def.Communication.AUTHENTICATE_ACTION_FINISH,
                        context.getString(R.string.act_finish));
                actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                actionIntent.putExtra(Def.Communication.KEY_TIME, hrTime);
                context.startActivity(actionIntent);
            } else {
                RemoteActionHelper.finishHabitOnce(context, thing, position, hrTime);
            }
        } else if (action.equals(Def.Communication.NOTIFICATION_ACTION_START_DOING)) {
            Intent actionIntent;
            if (thing.isPrivate()) {
                actionIntent = AuthenticationActivity.getOpenIntent(
                        context, TAG, id, position,
                        Def.Communication.AUTHENTICATE_ACTION_START_DOING,
                        context.getString(R.string.start_doing_full_title));
                actionIntent.putExtra(Def.Communication.KEY_TIME, hrTime);
            } else {
                actionIntent = StartDoingActivity.getOpenIntent(
                        context, thing.getId(), position, thing.getColor(),
                        DoingService.START_TYPE_ALARM, hrTime);
            }
            actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(actionIntent);
        }
    }
}
