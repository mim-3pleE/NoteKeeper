package com.example.notekeeper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.example.notekeeper.NoteKeeperProviderContract.Courses;
import com.example.notekeeper.NoteKeeperProviderContract.Notes;
import com.example.notekeeper.dataBasesActivity.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.example.notekeeper.dataBasesActivity.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.example.notekeeper.dataBasesActivity.NoteKeeperOpenHelper;

import java.util.List;

public class NoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int LOADER_NOTE_ID = 0;
    private final String TAG = getClass().getSimpleName();
    public static final String NOTE_POSITION = "com.example.notekeeper.NOTE_POSITION";
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.example.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE="com.example.notekeeper.NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT="com.example.notekeeper.NOTE_TEXT";
    public static final int LOADER_COURSES = 1;
    public static final int ID_NOT_SET = -1;
    private NoteInfo mNote = new NoteInfo(
            DataManager.getInstance().getCourses().get(0),"","");
    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
    private String mOriginalNoteCourseId;
    private String mOriginalNoteTitle;
    private String mOriginalNoteText;
    private NoteKeeperOpenHelper mDbOpenHelper;
    private  int mNoteTitlePos;
    private Cursor mNoteCursor;
    private int mNoteTextPos;
    private int mNoteCourseIdPos;
    private SimpleCursorAdapter mAdapterCourses;
    private boolean mCoursesQueryFinished;
    private boolean mNotesQueryFinished;
    private Uri mNoteUri;
    private ModuleStatusView mViewModuleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDbOpenHelper = new NoteKeeperOpenHelper(this);

        mSpinnerCourses = findViewById(R.id.spinner_courses);

        mAdapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
                null, new String[] {CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[] {android.R.id.text1}, 0);
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);

        getLoaderManager().initLoader(LOADER_COURSES, null, this);

        readDisplayStateValues();
        if(savedInstanceState == null){
            saveOriginalNoteValues();
        }else{
            restoreOriginalNoteValues(savedInstanceState);
        }

        mTextNoteTitle= findViewById(R.id.text_note_title);
        mTextNoteText= findViewById(R.id.text_note_text);

        if(!mIsNewNote){
         getLoaderManager().initLoader(LOADER_NOTE_ID, null, this);
        }

        mViewModuleStatus = findViewById(R.id.custom_view);
        loadModuleStatusValues();

       // Log.d(TAG, "onCreate");
    }

    private void loadModuleStatusValues() {
        int totalNumberOfModule = 11;
        int totalCompletedModule = 7;
        boolean[] moduleStatus = new boolean[totalNumberOfModule];
        for(int moduleIndex = 0; moduleIndex < totalCompletedModule; moduleIndex++){
            moduleStatus[moduleIndex] = true;

        }
        mViewModuleStatus.setModuleStatus(moduleStatus);

    }

    private void loadCourseData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID
        };

        Cursor courseCursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns, null,
                null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);
        mAdapterCourses.changeCursor(courseCursor);
    }

    public void loadNoteData(){
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        String selection = NoteInfoEntry._ID +" = ?";
        String[] selectionArg = {Integer.toString(mNoteId)};

        String[] noteColumns = {
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_NOTE_TEXT,
                NoteInfoEntry.COLUMN_COURSE_ID
        };

        mNoteCursor = db.query(NoteInfoEntry.TABLE_NAME, noteColumns, selection, selectionArg, null,
                null, null);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteCursor.moveToNext();

        displayNote();
    }

    private void displayNote() {
        String courseId = mNoteCursor.getString(mNoteCourseIdPos);
        String noteTitle = mNoteCursor.getString(mNoteTitlePos);
        String noteText = mNoteCursor.getString(mNoteTextPos);

        int courseIndex = getIndexOfCourseId(courseId);
        mSpinnerCourses.setSelection(courseIndex);
        mTextNoteTitle.setText(noteTitle);
        mTextNoteText.setText(noteText);

        CourseEventBroadcastHelper.sendEventBroadcast(this, courseId, "Editing Note");
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mAdapterCourses.getCursor();
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;

        boolean more = cursor.moveToFirst();
        while (more){
            String cursorCourseId = cursor.getString(courseIdPos);
            if(courseId.equals(cursorCourseId)){
                break;
            }
            courseRowIndex++;
        }
        return courseRowIndex;
    }

    @Override
    protected void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    private void restoreOriginalNoteValues(Bundle saveStateInstance){
        mOriginalNoteCourseId = saveStateInstance.getString(ORIGINAL_NOTE_COURSE_ID);
        mOriginalNoteTitle = saveStateInstance.getString(ORIGINAL_NOTE_TITLE);
        mOriginalNoteText = saveStateInstance.getString(ORIGINAL_NOTE_TEXT);
    }

    private void saveOriginalNoteValues(){
        if(mIsNewNote)
            return;

        mOriginalNoteCourseId = mNote.getCourse().getCourseId();
        mOriginalNoteTitle = mNote.getTitle();
        mOriginalNoteText = mNote.getText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mIsCancelling){
            Log.i(TAG, "Cancelling... note at position; " + mNoteId);
            if(mIsNewNote){
               // DataManager.getInstance().removeNote(mNoteId);
                deleteNoteFromDatabase();
            }else{
                storePreviousNoteValues();
            }
        }else{
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    private void deleteNoteFromDatabase() {
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
               getContentResolver().delete(mNoteUri, null, null);
                return null;
            }
        };
        task.execute();
    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mOriginalNoteTitle);
        mNote.setText(mOriginalNoteText);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);
    }

    private void saveNote() {
        String courseID = selectCourseId();
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();

        saveNoteToDatabase(courseID, noteTitle, noteText);
    }

    private String selectCourseId() {
        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
        Cursor cursor = mAdapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);

        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        String courseId = cursor.getString(courseIdPos);

        return courseId;
    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText){
        final ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, courseId);
        values.put(Notes.COLUMN_NOTE_TITLE, noteTitle);
        values.put(Notes.COLUMN_NOTE_TEXT, noteText);

       final AsyncTask task= new AsyncTask() {
           @Override
           protected Object doInBackground(Object[] objects) {
               getContentResolver().update(mNoteUri, values, null, null);
               return true;
           }
       }.execute();
    }

    private void displayNote(Spinner spinnerCourses, EditText textNoteTitle, EditText textNoteText) {
        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        int courseIndex = courses.indexOf(mNote.getCourse());
        spinnerCourses.setSelection(courseIndex);
        textNoteTitle.setText(mNote.getTitle());
        textNoteText.setText(mNote.getText());
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_POSITION, ID_NOT_SET);
        mIsNewNote = mNoteId == ID_NOT_SET;
        if(mIsNewNote) {
            createNewNote();
        }

        Log.i(TAG, "mNoteId: " + mNoteId);
        //mNote = DataManager.getInstance().getNotes().get(mNoteId);

    }

    private void createNewNote() {
        AsyncTask<ContentValues, Integer, Uri> task = new AsyncTask<ContentValues, Integer, Uri>() {
            private ProgressBar mProgressBar;

            @Override
            protected void onPreExecute() {
                mProgressBar = findViewById(R.id.progress_bar);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(1);
            }

            @Override
            protected Uri doInBackground(ContentValues... contentValues) {
                ContentValues insetValues= contentValues[0];
                Uri rowUri = getContentResolver().insert(Notes.CONTENT_URI, insetValues);
                simulateLongRunningWork();
                publishProgress(2);

                simulateLongRunningWork();
                publishProgress(3);

                return rowUri;
            }

            protected void onProgressUpdate(Integer... values) {
                int progressValue = values[0];
                mProgressBar.setProgress(progressValue);
            }

            @Override
            protected void onPostExecute(Uri uri) {
                   mNoteUri = uri;
                   displaySnackbar(mNoteUri.toString());
                   mProgressBar.setVisibility(View.GONE);
            }

        };
        ContentValues values= new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");
        Log.d(TAG, "call to execute -thread: "+Thread.currentThread().getId());
        task.execute(values);

    }

    private void displaySnackbar(String message) {
        View view = findViewById(R.id.spinner_courses);
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    private void simulateLongRunningWork() {
        try{
            Thread.sleep(2000);
        }catch (Exception ex){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        }else if(id==R.id.action_cancel){
            mIsCancelling=true;
            finish();
        }else if(id==R.id.action_next){
            moveNext();
        }else if(id==R.id.action_reminder_menu){
            showReminderNotification();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showReminderNotification() {
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();
        int noteId = (int)ContentUris.parseId(mNoteUri);

        Intent intent = new Intent(this, NoteReminderReceiver.class);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT, noteText);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long currentTimeInMillisecond = SystemClock.elapsedRealtime();
        long ONE_HOUR = 60 * 60 * 1000;
        long TEN_SECOND = 10 * 1000;

        long alarmTime = currentTimeInMillisecond + ONE_HOUR;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, TEN_SECOND, pendingIntent);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(mNoteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();

        ++mNoteId;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);

        saveOriginalNoteValues();
        displayNote(mSpinnerCourses, mTextNoteTitle, mTextNoteText);
        invalidateOptionsMenu();
    }

    private void sendEmail(){
        CourseInfo courseInfo = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Checkout what I learned in the Pluralsight course \"" +
                courseInfo.getTitle() +"\"\n" + mTextNoteText.getText().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    private CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;
        String[] noteColumns = {
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT,
                Notes.COLUMN_COURSE_ID
        };
        mNoteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        return new CursorLoader(this, mNoteUri, noteColumns, null,
                null,null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = null;
        if(i == LOADER_NOTE_ID){
            loader = createLoaderNotes();
        }else if(i == LOADER_COURSES){
            loader= createLoaderCourses();
        }
        return loader;
    }

    private CursorLoader createLoaderCourses() {
        mCoursesQueryFinished = false;
        //Uri uri = Uri.parse("content://com.example.notekeeper.provider");
        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COLUMN_COURSE_TITLE,
                Courses.COLUMN_COURSE_ID,
                Courses._ID
        };
        return new CursorLoader(this, uri,
                courseColumns, null, null, Courses.COLUMN_COURSE_TITLE);

        /*
        return new CursorLoader(this){@Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
                String[] courseColumns = {CourseInfoEntry.COLUMN_COURSE_TITLE,
               CourseInfoEntry.COLUMN_COURSE_ID,CourseInfoEntry._ID};
                return db.query(CourseInfoEntry.TABLE_NAME, courseColumns, null,
                        null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE); }};*/
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if(loader.getId() == LOADER_NOTE_ID){
            loadFinishedNote(cursor);
        }else if(loader.getId() == LOADER_COURSES){
            mAdapterCourses.changeCursor(cursor);
            mCoursesQueryFinished = true;
            displayNoteWhenQueriesFinished();
        }
    }

    private void loadFinishedNote(Cursor cursor) {
        mNoteCursor = cursor;
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteCursor.moveToNext();
        mNotesQueryFinished = true;
        displayNoteWhenQueriesFinished();
    }

    private void displayNoteWhenQueriesFinished() {
        if(mCoursesQueryFinished && mNotesQueryFinished){
            displayNote();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(loader.getId()==LOADER_NOTE_ID){
            if(mNoteCursor!=null){
                mNoteCursor.close();
            }else if(loader.getId()==LOADER_COURSES){
                mAdapterCourses.changeCursor(null);
            }
        }
    }
}
