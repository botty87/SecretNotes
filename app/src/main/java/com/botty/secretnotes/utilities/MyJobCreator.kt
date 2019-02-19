package com.botty.secretnotes.utilities

import com.botty.secretnotes.storage.jobs.NoteReminderJob
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class MyJobCreator: JobCreator {

    override fun create(tag: String): Job? {
        return when(tag) {
            NoteReminderJob.TAG -> NoteReminderJob()
            else -> null
        }
    }
}