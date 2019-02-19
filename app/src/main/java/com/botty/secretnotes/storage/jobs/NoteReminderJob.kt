package com.botty.secretnotes.storage.jobs

import android.app.PendingIntent
import android.content.Intent
import com.botty.secretnotes.MainActivity
import com.botty.secretnotes.R
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.db.note.Note
import com.botty.secretnotes.storage.db.note.Note_
import com.botty.secretnotes.storage.db.noteReminder.NoteReminder
import com.botty.secretnotes.storage.db.noteReminder.NoteReminder_
import com.botty.secretnotes.storage.db.noteReminder.getNoteReminderFromDB
import com.botty.secretnotes.storage.storage_extensions.getNoteReminderBox
import com.botty.secretnotes.storage.storage_extensions.getNotesBox
import com.botty.secretnotes.storage.storage_extensions.getNotesCollection
import com.botty.secretnotes.utilities.await
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import io.karn.notify.Notify
import io.objectbox.kotlin.equal
import io.objectbox.kotlin.query
import kotlinx.coroutines.runBlocking

class NoteReminderJob : Job() {

    override fun onRunJob(params: Params): Result {
        getNoteReminderBox().query {
            equal(NoteReminder_.jobId, params.id)
        }.findUnique()?.let {reminder ->
            runBlocking {
                notifyReminder(reminder)
            }
            cleanDB(reminder)
        }

        return Result.SUCCESS
    }

    private suspend fun notifyReminder(reminder: NoteReminder) {
        suspend fun getNote(): Note? {
            return if(AppPreferences.userHasAccount) {
                try {
                    getNotesCollection()
                            .document(reminder.noteFirestoreId!!).get().await()
                            .run {
                                Note.getNoteFromFirestoreDoc(this)
                            }
                } catch (e: Exception) {
                    null
                }
            }
            else {
                getNotesBox().query {
                    equal(Note_.id, reminder.noteObjBoxId!!)
                }.findUnique()
            }
        }

        getNote()?.let { note ->
            Notify.with(context)
                    .meta {
                        clickIntent = PendingIntent.getActivity(context, 0,
                                Intent(context, MainActivity::class.java), 0)
                    }
                    .run {
                        if(note.hasPassword()) {
                                    content {
                                        title = note.title
                                        text = context.getString(R.string.note_reminder)
                                    }
                        }
                        else {
                            asBigText {
                                title = note.title
                                text = context.getString(R.string.note_reminder)
                                expandedText = context.getString(R.string.note_reminder)
                                bigText = note.content
                            }
                        }
                    }.show()
        }
    }

    private fun cleanDB(reminder: NoteReminder) {
        getNoteReminderBox().remove(reminder)
    }

    companion object {
        const val TAG = "note_rem_job"

        fun setNoteReminder(note: Note) {
            fun initNoteReminder(): NoteReminder {
                getNoteReminderFromDB(note)?.run {
                    return this
                }

                return if(AppPreferences.userHasAccount) {
                    NoteReminder(noteFirestoreId = note.firestoreId)
                } else {
                    NoteReminder(noteObjBoxId = note.id)
                }
            }

            val noteReminder = initNoteReminder()

            //If already exist a work cancel it
            noteReminder.jobId?.let { jobId ->
                JobManager.instance().cancel(jobId)
            }

            if(note.reminder != null) {
                val exactLongTime = note.reminder!!.time - System.currentTimeMillis()
                JobRequest.Builder(TAG)
                        .setExact(exactLongTime)
                        .build()
                        .schedule().let { jobId ->
                            noteReminder.reminder = note.reminder
                            noteReminder.jobId = jobId
                            getNoteReminderBox().put(noteReminder)
                        }
            }
            //It means that we have to remove the reminder, cleaning DB
            else if(noteReminder.id != 0L){
                getNoteReminderBox().remove(noteReminder)
            }
        }
    }

}