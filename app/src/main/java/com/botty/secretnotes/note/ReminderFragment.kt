package com.botty.secretnotes.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.botty.secretnotes.R
import com.botty.secretnotes.note.data.NoteActivityViewModel
import com.botty.secretnotes.storage.db.note.Note
import com.botty.secretnotes.utilities.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.android.synthetic.main.fragment_reminder.*
import kotlinx.coroutines.*
import org.joda.time.LocalDateTime
import java.util.*

class ReminderFragment : NoteFragmentCallbacks(), CoroutineScope by MainScope() {

    private lateinit var viewModel: NoteActivityViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        activity?.run {
            viewModel = ViewModelProviders.of(this).get(NoteActivityViewModel::class.java)
        }
        return inflater.inflate(R.layout.fragment_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launch {
            setReminderFromNote()
            setListenersForReminder()
        }
    }

    private fun setReminderFromNote() {

        lateinit var noteObserver: Observer<Note>
        noteObserver = Observer { note ->
            calendarViewReminder.state().edit()
                    .setMinimumDate(CalendarDay.today())
                    .commit()
            calendarViewReminder.selectedDate = note.reminder?.getCalendarDay()

            timePickerReminder.setIs24HourView(true)
            note.reminder?.run {
                timePickerReminder.setFromDate(this)
            }
            checkboxReminderEnabled.isChecked = note.reminder != null

            viewModel.note.removeObserver(noteObserver)
        }
        viewModel.note.observe(this, noteObserver)
    }

    private suspend fun setListenersForReminder() = withContext(Dispatchers.Default) {
        fun getReminderDate(): Date? {
            return calendarViewReminder.selectedDate?.run {
                val hourAndMinute = timePickerReminder.getHourAndMinute()

                LocalDateTime.now()
                        .withDate(year, month, day)
                        .withHourOfDay(hourAndMinute.first)
                        .withMinuteOfHour(hourAndMinute.second)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0)
                        .toDate()
            }
        }

        checkboxReminderEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                val reminderDate = getReminderDate()
                if(reminderDate != null) {
                    viewModel.note.value?.reminder = reminderDate
                    buttonView.text = getString(R.string.reminder_enabled)

                }
                else {
                    checkboxReminderEnabled.isChecked = false
                    toastError(R.string.select_valid_date_reminder)
                }
            }
            else {
                viewModel.note.value?.reminder = null
                buttonView.text = getString(R.string.reminder_not_enabled)
            }
        }

        timePickerReminder.setOnTimeChangedListener { view, hourOfDay, minute ->
            viewModel.note.value?.reminder?.getLocalDateTime()?.run {
                viewModel.note.value?.reminder = this
                        .withHourOfDay(hourOfDay)
                        .withMinuteOfHour(minute)
                        .toDate()
            }
        }

        calendarViewReminder.setOnDateChangedListener { widget, date, selected ->
            viewModel.note.value?.reminder?.getLocalDateTime()?.run {
                val hourMinutes = timePickerReminder.getHourAndMinute()
                viewModel.note.value?.reminder = this
                        .withYear(date.year)
                        .withMonthOfYear(date.month)
                        .withDayOfMonth(date.day)
                        .withHourOfDay(hourMinutes.first)
                        .withMinuteOfHour(hourMinutes.second)
                        .toDate()
            }
        }
    }

    companion object {
        fun newInstance(): ReminderFragment {
            return ReminderFragment()
        }
    }
}
