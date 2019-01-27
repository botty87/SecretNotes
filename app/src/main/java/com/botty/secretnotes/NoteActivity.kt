package com.botty.secretnotes

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.botty.secretnotes.databinding.ActivityNoteBinding
import com.botty.secretnotes.settings.SettingsContainer
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.storage.new_db.note.Note
import com.botty.secretnotes.storage.storage_extensions.saveNote
import com.botty.secretnotes.utilities.activites.BottomSheetCategoriesActivity
import com.botty.secretnotes.utilities.getDialog
import com.botty.secretnotes.utilities.loadAd
import com.botty.secretnotes.utilities.security.Security
import com.botty.secretnotes.utilities.toastSuccess
import com.botty.secretnotes.utilities.userHasAccount
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.bottom_sheet_main_content.*
import kotlinx.android.synthetic.main.bottom_sheet_main_title.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class NoteActivity : BottomSheetCategoriesActivity() {
    private lateinit var noteBinding: ActivityNoteBinding

    private var password: String? = null

    private lateinit var buttonUnlock: MenuItem
    private lateinit var buttonLock: MenuItem

    private val isButtonSaveEnabled by lazy {
        !SettingsContainer.getSettingsContainer(this).noteAutoSave
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        noteBinding = DataBindingUtil.setContentView(this, R.layout.activity_note)

        readNote(savedInstanceState, intent)
        setSelectedCategory(intent)

        loadAd(adView)

        setBottomSheetCategories(bottomSheetCategories, bottomSheetPeek, recyclerViewCategories,
                imageViewShowHide, viewCategoriesBackground, isEditMode = false)

        if(isButtonSaveEnabled) {
            buttonSave.setOnClickListener {
                if(noteBinding.note?.isValid() == true) {
                    saveNote()
                }
                else {
                    Toasty.error(this, getString(R.string.fill_the_note)).show()
                }
            }
        }
        else {
            buttonSave.visibility = View.GONE
        }

        onBackPressedPlus = this::onBackPressedPlus
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run {
            readNote(null, this)
            setSelectedCategory(this)
        }
    }

    private fun setSelectedCategory(intent: Intent) {
        noteBinding.note?.let {note ->

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
                    return
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
                    return
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

    private fun readNote(savedInstanceState: Bundle?, intent: Intent) {
        noteBinding.note = savedInstanceState?.getParcelable(Note.NOTE_TAG)
        password = savedInstanceState?.getString(Note.NOTE_PAS)

        if(noteBinding.note == null) {
            noteBinding.note = intent.getParcelableExtra(Note.NOTE_TAG)
            if(noteBinding.note == null) {
                finish()
            }
            if(password.isNullOrBlank()) {
                password = intent.getStringExtra(Note.NOTE_PAS)
            }

            noteBinding.note!!.let {note ->
                password?.let {password ->
                    note.content = Security.decryptNote(password, note)
                }
            }
        }
    }

    private fun saveNote() {
        noteBinding.note?.let {note ->
            if(password != null && password?.isNotBlank() == true) {
                Security.encryptNote(password!!, note.content).run {
                    note.content = first
                    note.nonceArray = second
                }
                note.passwordHash = Security.getPasswordHash(password!!)
            }
            else {
                note.passwordHash = null
                note.nonceArray = null
            }
            saveNote(note)
        }
        toastSuccess(R.string.saved)
        val resultIntent = Intent()
        resultIntent.putExtra(Note.NOTE_TAG, noteBinding.note)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun onBackPressedPlus() {
        if(isButtonSaveEnabled) {
            Intent().run {
                putExtra(Note.NOTE_DISCARDED, noteBinding.note)
                putExtra(Category.NAME_KEY, categoryAdapter.selectedCategory?.name)
                setResult(Activity.RESULT_CANCELED, this)
            }
            finish()
        }
        else {
            if(noteBinding.note?.isValid() == true) {
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
            getDialog()
                    .title(R.string.lock_note)
                    .message(R.string.lock_note_message)
                    .negativeButton(R.string.cancel)
                    .positiveButton(R.string.set)
                    .show {
                        setActionButtonEnabled(WhichButton.POSITIVE, false)

                        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        input(inputType = inputType, waitForPositiveButton = false) { dialog, passwordChars ->
                            val isValid = passwordChars.length >= 6
                            dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                            password = if (isValid) passwordChars.toString() else null
                            setButtonsLockUnlock()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getInputField()?.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                        }
                    }
        }

        fun unlockNote() {
            getDialog()
                    .title(R.string.unlock_note)
                    .message(R.string.unlock_note_message)
                    .negativeButton (R.string.no)
                    .positiveButton (R.string.yes) {
                        password = null
                        setButtonsLockUnlock()
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
                        password = intent.getStringExtra(Note.NOTE_PAS)
                        if(password != null && password?.isNotBlank() == true) {
                            noteBinding.note?.run {
                                Security.encryptNote(password!!, content).run {
                                    content = first
                                    nonceArray = second
                                }
                                passwordHash = Security.getPasswordHash(password!!)
                            }
                        }

                        val resultIntent = Intent()
                        resultIntent.putExtra(Note.NOTE_TAG, noteBinding.note)
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
        val locked = password?.isNotBlank() ?: false
        buttonLock.isVisible = !locked
        buttonUnlock.isVisible = locked
    }

    override fun onCategoryClicked(category: Category?) {
        super.onCategoryClicked(category)
        if(userHasAccount()) {
            noteBinding.note?.firestoreCatId = category?.firestoreId
        }
        else {
            noteBinding.note?.category?.targetId = category?.id ?: Category.NO_CATEGORY_ID
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(Note.NOTE_TAG, noteBinding.note)
        outState.putString(Note.NOTE_PAS, password)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        noteBinding.note = null
    }
}
