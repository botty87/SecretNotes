package com.botty.secretnotes.note

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.botty.secretnotes.R

class PagerAdapter(fm: FragmentManager, private val context: Context): FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> ContentFragment.newInstance()
            1 -> PositionFragment.newInstance()
            else -> ReminderFragment.newInstance()
        }
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when(position) {
            0 -> getString(R.string.text)
            1 -> context.getString(R.string.position)
            2 -> context.getString(R.string.reminder)
            else -> super.getPageTitle(position)
        }
    }

    private fun getString(stringRes: Int): String {
        return context.getString(stringRes)
    }
}