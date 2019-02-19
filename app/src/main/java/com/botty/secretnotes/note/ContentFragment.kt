package com.botty.secretnotes.note

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.botty.secretnotes.R
import com.botty.secretnotes.databinding.FragmentContentBinding
import com.botty.secretnotes.utilities.*
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarCallback
import kotlinx.android.synthetic.main.activity_note.*

import kotlinx.android.synthetic.main.fragment_content.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.email

class ContentFragment : NoteFragmentCallbacks() {

    private lateinit var contentBinding: FragmentContentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        contentBinding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_content, container, false)
        contentBinding.note = noteCallbacks?.getNote()

        return contentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Set the link management for the content
        editTextContent.afterTextChanged {
            Linkify.addLinks(it as Spannable, Linkify.ALL)
        }

        editTextContent.onLinkClicked {url, urlType ->
            val textRes = when(urlType) {
                UrlType.WEB -> R.string.snackbar_link_click
                UrlType.EMAIL -> R.string.snackbar_email_click
                else -> R.string.snackbar_phone_click
            }

            activity?.run {
                showCafeBar(textRes, noteCoordLayout, duration = CafeBar.Duration.MEDIUM,
                        action = R.string.open to CafeBarCallback { cafeBar ->
                            when (urlType) {
                                UrlType.WEB -> browse(url)
                                UrlType.EMAIL ->
                                    url.removePrefix("mailto:").run { email(this) }
                                else -> openDialer(url)
                            }
                            cafeBar.dismiss()
                        })
            }
        }

        if (noteCallbacks?.getIsButtonSaveEnabled() == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollViewTextContent.setOnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                if (scrollY > oldScrollY) {
                    noteCallbacks?.changeFabSaveVisibility(false)
                } else {
                    noteCallbacks?.changeFabSaveVisibility(true)
                }
            }
        }
    }


    companion object {
        fun newInstance(): ContentFragment {
            return ContentFragment()
        }
    }

}
