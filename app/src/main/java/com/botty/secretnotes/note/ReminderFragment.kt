package com.botty.secretnotes.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.botty.secretnotes.R
import com.botty.secretnotes.utilities.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.android.synthetic.main.fragment_reminder.*
import org.joda.time.LocalDateTime
import java.util.*

class ReminderFragment : NoteFragmentCallbacks() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fun setReminderFromNote() {
            calendarViewReminder.state().edit()
                    .setMinimumDate(CalendarDay.today())
                    .commit()
            calendarViewReminder.selectedDate = noteCallbacks?.getNote()?.reminder?.getCalendarDay()

            timePickerReminder.setIs24HourView(true)
            noteCallbacks?.getNote()?.reminder?.run {
                timePickerReminder.setFromDate(this)
            }

            checkboxReminderEnabled.isChecked = noteCallbacks?.getNote()?.reminder != null
        }

        setReminderFromNote()
        setListenersForReminder()
    }

    private fun setListenersForReminder() {
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
                    noteCallbacks?.getNote()?.reminder = reminderDate
                    buttonView.text = getString(R.string.reminder_enabled)

                }
                else {
                    checkboxReminderEnabled.isChecked = false
                    toastError(R.string.select_valid_date_reminder)
                }
            }
            else {
                noteCallbacks?.getNote()?.reminder = null
                buttonView.text = getString(R.string.reminder_not_enabled)
            }
        }

        timePickerReminder.setOnTimeChangedListener { view, hourOfDay, minute ->
            noteCallbacks?.getNote()?.reminder?.getLocalDateTime()?.run {
                noteCallbacks?.getNote()?.reminder = this
                        .withHourOfDay(hourOfDay)
                        .withMinuteOfHour(minute)
                        .toDate()
            }
        }

        calendarViewReminder.setOnDateChangedListener { widget, date, selected ->
            noteCallbacks?.getNote()?.reminder?.getLocalDateTime()?.run {
                val hourMinutes = timePickerReminder.getHourAndMinute()
                noteCallbacks?.getNote()?.reminder = this
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
