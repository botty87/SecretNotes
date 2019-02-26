package com.botty.secretnotes.note

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.botty.secretnotes.R
import com.botty.secretnotes.databinding.ActivityNoteBinding
import com.botty.secretnotes.note.data.NoteActivityViewModel
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.db.category.Category
import com.botty.secretnotes.storage.db.note.Note
import com.botty.secretnotes.storage.storage_extensions.saveNote
import com.botty.secretnotes.utilities.activites.BottomSheetCategoriesActivity
import com.botty.secretnotes.utilities.getDialog
import com.botty.secretnotes.utilities.loadAd
import com.botty.secretnotes.utilities.security.Security
import com.botty.secretnotes.utilities.toastError
import com.botty.secretnotes.utilities.toastSuccess
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.bottom_sheet_main_content.*
import kotlinx.android.synthetic.main.bottom_sheet_main_title.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.util.*

@ExperimentalCoroutinesApi
class NoteActivity : BottomSheetCategoriesActivity(), NoteCallbacks {

    private lateinit var noteBinding: ActivityNoteBinding
    private val viewModel by lazy {
        noteBinding.viewModel!!
    }

    private lateinit var buttonUnlock: MenuItem
    private lateinit var buttonLock: MenuItem

    private val isButtonSaveEnabled = !AppPreferences.noteAutoSave

    override fun onCreate(savedInstanceState: Bundle?) {

        suspend fun setButtonSave() = withContext(Dispatchers.Default) {
            if(isButtonSaveEnabled) {
                buttonSave.setOnClickListener {
                    if(viewModel.note.value?.isValid() == true) {
                        saveNote()
                    }
                    else {
                        Toasty.error(this@NoteActivity, getString(R.string.fill_the_note)).show()
                    }
                }
            }
            else {
                buttonSave.visibility = View.GONE
            }
        }

        suspend fun setKeyboardListener() = withContext(Dispatchers.Default) {
            KeyboardVisibilityEvent.setEventListener(this@NoteActivity) { isOpen ->
                if(isOpen) {
                    if(!editTextTitle.hasFocus()) {
                        tabLayout.visibility = GONE
                        if(isButtonSaveEnabled) {
                            buttonSave.hide()
                        }
                    }
                } else {
                    tabLayout.visibility = VISIBLE
                    if(isButtonSaveEnabled) {
                        buttonSave.show()
                    }
                    currentFocus?.clearFocus()
                }
            }
        }

        fun setViewPager() {
            viewPager.adapter = PagerAdapter(supportFragmentManager, this@NoteActivity)
            tabLayout.setupWithViewPager(viewPager)
        }

        super.onCreate(savedInstanceState)
        noteBinding = DataBindingUtil.setContentView(this, R.layout.activity_note)
        noteBinding.lifecycleOwner = this
        noteBinding.viewModel = ViewModelProviders.of(this).get(NoteActivityViewModel::class.java)

        launch {
            val fab = if(isButtonSaveEnabled) buttonSave else null
            readNote(savedInstanceState, intent)
            setSelectedCategory(intent)

            setButtonSave()
            setKeyboardListener()

            setBottomSheetCategories(bottomSheetCategories, bottomSheetPeek, recyclerViewCategories,
                    imageViewShowHide, viewCategoriesBackground, fab, false)

            onBackPressedPlus = this@NoteActivity::onBackPressedPlus
        }

        setViewPager()
        loadAd(adView)
    }

    private suspend fun readNote(savedInstanceState: Bundle?, intent: Intent) = withContext(Dispatchers.Default) {
        var note: Note? = savedInstanceState?.getParcelable(Note.NOTE_TAG)
        var password = savedInstanceState?.getString(Note.NOTE_PAS)

        if(note == null) {
            note = intent.getParcelableExtra(Note.NOTE_TAG)
            if(note == null) {
                finish()
                return@withContext
            }
            if(password.isNullOrBlank()) {
                password = intent.getStringExtra(Note.NOTE_PAS)
            }

            note.let {note ->
                password?.let {password ->
                    note.content = Security.decryptNote(password, note)
                }
            }
        }

        viewModel.note.postValue(note)
        viewModel.password.postValue(password)
    }

    private suspend fun setSelectedCategory(intent: Intent) = withContext(Dispatchers.Default) {
        viewModel.note.value?.let {note ->

            val tempSelectedCategory = Category().also {tempSelCat ->
                tempSelCat.firestoreId = note.firestoreCatId
                tempSelCat.id = note.category.targetId
            }

            if(!tempSelectedCategory.hasNoCategoryID()) {
                categoryAdapter.setSelectedCategory(tempSelectedCategory, forceSelected = true)

                intent.extras?.getString(Category.NAME_KEY)?.run {
                    if(isNotBlank()) {
                        textViewCategoryTitle.text = this
                    }
                    return@withContext
                }

                /*
                Rare situation. In case off "show all" settings could happen that we have a note
                with category, but we do not have the category name. In this case we need to retrieve
                the db.
               */
                categoryAdapter.categories.find {category ->
                    category.matchCategory(tempSelectedCategory)
                }?.run {
                    textViewCategoryTitle.text = name
                    return@withContext
                }

                categoryAdapter.onCategoriesChanged = {categories ->
                    categories.find {category ->
                        category.matchCategory(tempSelectedCategory)
                    }?.run {
                        textViewCategoryTitle.text = name
                        categoryAdapter.onCategoriesChanged = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run {
            launch {
                readNote(null, this@run)
                setSelectedCategory(this@run)
            }
        }
    }

    private fun saveNote() {
        if(viewModel.note.value?.reminder?.before(Date()) == true) {
            toastError(getString(R.string.reminder_before_now))
            return
        }
        viewModel.note.value?.let {note ->
            viewModel.password.value.let {password ->
                if(password != null && password.isNotBlank()) {
                    Security.encryptNote(password, note.content).run {
                        note.content = first
                        note.nonceArray = second
                    }
                    note.passwordHash = Security.getPasswordHash(password)
                }
                else {
                    note.passwordHash = null
                    note.nonceArray = null
                }
            }
            saveNote(note)
        }
        toastSuccess(R.string.saved)
        val resultIntent = Intent()
        resultIntent.putExtra(Note.NOTE_TAG, viewModel.note.value)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun onBackPressedPlus() {
        if(isButtonSaveEnabled) {
            Intent().run {
                putExtra(Note.NOTE_DISCARDED, viewModel.note.value)
                putExtra(Category.NAME_KEY, categoryAdapter.selectedCategory?.name)
                setResult(Activity.RESULT_CANCELED, this)
            }
            finish()
        }
        else {
            if(viewModel.note.value?.isValid() == true) {
                saveNote()
            }
            else {
                getDialog()
                        .title(R.string.no_valid_note_title)
                        .message(R.string.no_valid_note_message)
                        .positiveButton(R.string.editing)
                        .negativeButton(R.string.discard) {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                        .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note_menu, menu)
        menu?.run {
            buttonLock = findItem(R.id.action_lock)
            buttonUnlock = findItem(R.id.action_unlock)

            setButtonsLockUnlock()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        fun lockNote() {
            lateinit var newPassword: String

            getDialog()
                    .title(R.string.lock_note)
                    .message(R.string.lock_note_message)
                    .negativeButton(R.string.cancel)
                    .positiveButton(R.string.set) {
                        viewModel.password.value = newPassword
                    }
                    .show {
                        setActionButtonEnabled(WhichButton.POSITIVE, false)

                        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        input(inputType = inputType, waitForPositiveButton = false) { dialog, passwordChars ->
                            val isValid = passwordChars.length >= 6
                            dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                            if (isValid) {
                                newPassword = passwordChars.toString()
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getInputField().importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                        }
                    }
        }

        fun unlockNote() {
            getDialog()
                    .title(R.string.unlock_note)
                    .message(R.string.unlock_note_message)
                    .negativeButton (R.string.no)
                    .positiveButton (R.string.yes) {
                        viewModel.password.value = null
                    }
                    .show()
        }

        fun deleteNote() {
            getDialog()
                    .title(R.string.delete_note)
                    .message(R.string.delete_note_message)
                    .negativeButton(R.string.no)
                    .positiveButton(R.string.yes) {
                        //Verify if there was an original password
                        val password = intent.getStringExtra(Note.NOTE_PAS)
                        if(password != null && password.isNotBlank()) {
                            viewModel.note.value?.run {
                                Security.encryptNote(password, content).run {
                                    content = first
                                    nonceArray = second
                                }
                                passwordHash = Security.getPasswordHash(password)
                            }
                        }

                        val resultIntent = Intent()
                        resultIntent.putExtra(Note.NOTE_TAG, viewModel.note.value)
                        resultIntent.putExtra(Note.NOTE_TO_DELETE, true)
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                    .show()
        }

        when(item?.itemId) {
            R.id.action_lock -> {
                lockNote()
                return true
            }

            R.id.action_unlock -> {
                unlockNote()
                return true
            }

            R.id.action_delete -> {
                deleteNote()
                return true
            }

            android.R.id.home -> {
                onBackPressedPlus()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setButtonsLockUnlock() {
        viewModel.password.observe(this, androidx.lifecycle.Observer { password ->
            val locked = password?.isNotBlank() ?: false
            buttonLock.isVisible = !locked
            buttonUnlock.isVisible = locked
        })
    }

    override fun onCategoryClicked(category: Category?) {
        super.onCategoryClicked(category)
        if(AppPreferences.userHasAccount) {
            viewModel.note.value?.firestoreCatId = category?.firestoreId
        }
        else {
            viewModel.note.value?.category?.targetId = category?.id ?: Category.NO_CATEGORY_ID
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(Note.NOTE_TAG, viewModel.note.value)
        outState.putString(Note.NOTE_PAS, viewModel.password.value)
        super.onSaveInstanceState(outState)
    }

    /*
    TODO test if ok!!!
    override fun onDestroy() {
        super.onDestroy()
        noteBinding.note = null
    }

    override fun getNote(): Note {
        return noteBinding.note!!
    }*/

    override fun isButtonSaveEnabled(): Boolean {
        return isButtonSaveEnabled
    }

    override fun changeFabSaveVisibility(isVisible: Boolean) {
        if(isVisible) {
            buttonSave.show()
        }
        else {
            buttonSave.hide()
        }
    }
}
