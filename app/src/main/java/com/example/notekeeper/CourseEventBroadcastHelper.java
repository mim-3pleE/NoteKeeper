package com.example.notekeeper;

import android.content.Context;
import android.content.Intent;

class CourseEventBroadcastHelper {

    static final String ACTION_COURSE_EVENT = "com.example.notekeeper.COURSE_EVENT";

     static final String EXTRA_COURSE_ID = "com.example.notekeeper.COURSE_ID";
     static final String EXTRA_COURSE_MESSAGE = "com.example.notekeeper.COURSE_MESSAGE";

    static void sendEventBroadcast(Context context, String courseId, String message){

        Intent intent = new Intent(ACTION_COURSE_EVENT);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        intent.putExtra(EXTRA_COURSE_MESSAGE, message);

        context.sendBroadcast(intent);

    }



}
