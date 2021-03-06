package com.aiora.trendy;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OSNotificationAction;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OSNotificationPayload;
import com.onesignal.OneSignal;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class App extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //OneSignal Push
        if (!TextUtils.isEmpty(getString(R.string.onesignal_app_id)))
            OneSignal.init(this, getString(R.string.onesignal_google_project_number), getString(R.string.onesignal_app_id), new NotificationHandler());

    }

    // This fires when a notification is opened by tapping on it or one is received while the app is running.
    private class NotificationHandler implements OneSignal.NotificationOpenedHandler {
        // This fires when a notification is opened by tapping on it.
        @Override
        public void notificationOpened(OSNotificationOpenResult result) {
            try {
                //String webViewUrl = (data != null) ? data.optString("url", null) : null;

                if (!result.notification.isAppInFocus) {
                    Intent mainIntent;
                    mainIntent = new Intent(App.this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                }

                List<OSNotificationPayload.ActionButton> ans = result.notification.payload.actionButtons;

                String correctAnswer = "";
                for (int i = 0; i < ans.size(); i++) {
                    if (ans.get(i).id.equals("true")) {
                        correctAnswer = ans.get(i).text;
                    }
                }

                JSONObject data = result.notification.payload.additionalData;
                String explain = data.getString("exp");

                OSNotificationAction.ActionType actionType = result.action.type;

                if (actionType == OSNotificationAction.ActionType.ActionTaken) {

                    String body = result.notification.payload.body;
                    String[] truePhrase = {"Your Answer Is Correct"
                            ,"Correct"};
                    String[] falsePhrase = {"Better Luck Next Time"
                            ,"False"};
                    String[] closePhrase = {"No, But Very Close"
                            ,"Close"};

                    switch (result.action.actionID) {
                        case "true":
                            generateNotification(truePhrase[new Random().nextInt(truePhrase.length)]
                                    , body, correctAnswer);
                            updateDatabase(correctAnswer, ans.get(0).text,explain,
                                    ans.get(1).text, ans.get(2).text, body, result.action.actionID);
                            break;
                        case "false":
                            generateNotification(falsePhrase[new Random().nextInt(falsePhrase.length)]
                                    , body, correctAnswer);
                            updateDatabase(correctAnswer, ans.get(0).text,explain,
                                    ans.get(1).text, ans.get(2).text, body, result.action.actionID);
                            break;
                        case "close":
                            generateNotification(closePhrase[new Random().nextInt(closePhrase.length)]
                                    , body, correctAnswer);
                            updateDatabase(correctAnswer, ans.get(0).text,explain,
                                    ans.get(1).text, ans.get(2).text, body, result.action.actionID);
                            break;
                    }
                }

                /*String browserUrl = result.notification.payload.launchURL;
                if (webViewUrl != null || browserUrl != null) {
                    if (webViewUrl != null){
                        HolderActivity.startWebViewActivity(App.this, webViewUrl, false, false, null, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                    } else {
                        HolderActivity.startWebViewActivity(App.this, browserUrl, true, false, null, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                } else if (!result.notification.isAppInFocus) {
                    Intent mainIntent;
                    mainIntent = new Intent(App.this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                }*/

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }

    public void generateNotification(String message, String body, String answer) {

        String title = "Quiz of the Day Answer";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationHelper notificationHelper = new NotificationHelper(this);
            Notification.Builder builder = notificationHelper.getNotification1(title, body, message, answer);

            Intent notificationIntent = new Intent(App.this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                    notificationIntent, 0);
            builder.setContentIntent(intent);

            notificationHelper.notify(1001, builder);

        } else {

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                            .setContentTitle(title)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setContentText(message);

            if (Build.VERSION.SDK_INT >= 21) mBuilder.setVibrate(new long[100]);

            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.bigText(message + "\n\n" + body + "\n" + answer);

            mBuilder.setStyle(bigText);

            Intent notificationIntent = new Intent(App.this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 110,
                    notificationIntent, 0);
            mBuilder.setContentIntent(intent);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            assert mNotificationManager != null;
            mNotificationManager.notify(5678, mBuilder.build());
        }
    }

    public void updateDatabase(String ans, String op1, String explain, String op2, String op3, final String ques, String identity) {

        final String answer = ans;
        final String optionOne = op1;
        final String optionTwo = op2;
        final String optionThree = op3;
        final String question = ques;

        final String id = identity;
        final String exp = explain;

        final Date date = Calendar.getInstance().getTime();

        SimpleDateFormat df3 = new SimpleDateFormat("dd-MM-yyyy");
        final String formattedDate = df3.format(date);

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("quiz");

        reference.getParent().child("version").setValue("1.2");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String finalNode = formattedDate + " " + question;
                //String finalQuestionPlus = question + (dataSnapshot.getChildrenCount()+1);
                //String finalQuestion = question + (dataSnapshot.getChildrenCount());

                if(reference.getParent().child("version").getKey() != null) {

                    if (dataSnapshot.child(finalNode).exists())
                        /*dataSnapshot.child(formattedDate).child(question).exists()) {
                    if (dataSnapshot.child(formattedDate).child(question).exists())*/ {
                        Integer t = dataSnapshot.child(finalNode).child("resTrue")
                                .getValue(Integer.class);
                        Integer f = dataSnapshot.child(finalNode).child("resFalse")
                                .getValue(Integer.class);
                        Integer c = dataSnapshot.child(finalNode).child("resClose")
                                .getValue(Integer.class);

                        if (t != null || f != null || c != null)

                            switch (id) {
                                case "true":
                                    reference.child(finalNode).child("resTrue").setValue(t + 1);
                                    break;
                                case "false":
                                    reference.child(finalNode).child("resFalse").setValue(f + 1);
                                    break;
                                case "close":
                                    reference.child(finalNode).child("resClose").setValue(c + 1);
                                    break;
                            }

                    } else {
                        reference.child(finalNode).child("answer").setValue(answer);
                        reference.child(finalNode).child("option1").setValue(optionOne);
                        reference.child(finalNode).child("option2").setValue(optionTwo);
                        reference.child(finalNode).child("option3").setValue(optionThree);
                        reference.child(finalNode).child("question").setValue(question);
                        reference.child(finalNode).child("date").setValue(formattedDate);
                        reference.child(finalNode).child("explain").setValue(exp);
                        switch (id) {
                            case "true":
                                reference.child(finalNode).child("resTrue").setValue(1);
                                reference.child(finalNode).child("resFalse").setValue(0);
                                reference.child(finalNode).child("resClose").setValue(0);
                                break;
                            case "false":
                                reference.child(finalNode).child("resTrue").setValue(0);
                                reference.child(finalNode).child("resFalse").setValue(1);
                                reference.child(finalNode).child("resClose").setValue(0);
                                break;
                            case "close":
                                reference.child(finalNode).child("resTrue").setValue(0);
                                reference.child(finalNode).child("resFalse").setValue(0);
                                reference.child(finalNode).child("resClose").setValue(1);
                                break;
                        }
                    }

                    reference.getParent().child("version").removeValue();
                }

                /*if (dataSnapshot.child(formattedDate).exists()
                        /*dataSnapshot.child(formattedDate).child(question).exists()//) {
                    if (dataSnapshot.child(formattedDate).child(question).exists()) {
                        Integer t = dataSnapshot.child(formattedDate).child(question)
                                .child("resTrue").getValue(Integer.class);
                        Integer f = dataSnapshot.child(formattedDate).child(question)
                                .child("resFalse").getValue(Integer.class);
                        Integer c = dataSnapshot.child(formattedDate).child(question)
                                .child("resClose").getValue(Integer.class);

                        if (t != null || f != null || c != null)

                            switch (id) {
                                case "true":
                                    reference.child(formattedDate).child(question)
                                            .child("resTrue").setValue(t + 1);
                                    break;
                                case "false":
                                    reference.child(formattedDate).child(question)
                                            .child("resFalse").setValue(f + 1);
                                    break;
                                case "close":
                                    reference.child(formattedDate).child(question)
                                            .child("resClose").setValue(c + 1);
                                    break;
                            }
                    }
                } else {
                    reference.child(formattedDate).child(question).child("answer").setValue(answer);
                    reference.child(formattedDate).child(question).child("option1").setValue(optionOne);
                    reference.child(formattedDate).child(question).child("option2").setValue(optionTwo);
                    reference.child(formattedDate).child(question).child("option3").setValue(optionThree);
                    reference.child(formattedDate).child(question).child("question").setValue(question);
                    switch (id) {
                        case "true":
                            reference.child(formattedDate).child(question).child("resTrue").setValue(1);
                            reference.child(formattedDate).child(question).child("resFalse").setValue(0);
                            reference.child(formattedDate).child(question).child("resClose").setValue(0);
                            break;
                        case "false":
                            reference.child(formattedDate).child(question).child("resTrue").setValue(0);
                            reference.child(formattedDate).child(question).child("resFalse").setValue(1);
                            reference.child(formattedDate).child(question).child("resClose").setValue(0);
                            break;
                        case "close":
                            reference.child(formattedDate).child(question).child("resTrue").setValue(0);
                            reference.child(formattedDate).child(question).child("resFalse").setValue(0);
                            reference.child(formattedDate).child(question).child("resClose").setValue(1);
                            break;
                    }
                }*/
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

}