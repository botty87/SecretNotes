package com.botty.secretnotes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.botty.secretnotes.note.NoteActivity
import com.botty.secretnotes.settings.SettingsActivity
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.adapters.NoteAdapter
import com.botty.secretnotes.storage.db.category.Category
import com.botty.secretnotes.storage.db.note.Note
import com.botty.secretnotes.storage.db.note.NoteViewModel
import com.botty.secretnotes.storage.storage_extensions.*
import com.botty.secretnotes.user_account.LoginActivity
import com.botty.secretnotes.utilities.*
import com.botty.secretnotes.utilities.activites.BottomSheetCategoriesActivity
import com.botty.secretnotes.utilities.security.Security
import com.botty.secretnotes.utilities.security.askMasterPassword
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarCallback
import com.firebase.ui.auth.AuthUI
import com.github.florent37.kotlin.pleaseanimate.please
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_main_content.*
import kotlinx.android.synthetic.main.bottom_sheet_main_title.*
import kotlinx.coroutines.*
import org.jetbrains.anko.startActivityForResult


@ExperimentalCoroutinesApi
class MainActivity : BottomSheetCategoriesActivity() {

    private val noteAdapter by lazy {
        NoteAdapter(this).apply {
            onItemClick = this@MainActivity::onNoteClicked
        }
    }

    private val notesViewModel by lazy {
        ViewModelProviders.of(this).get(NoteViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        fun checkIfMigratePreferences() {
            if(AppPreferences.migrationNeeded) {
                val preferences = getSharedPreferences("note_preferences", Context.MODE_PRIVATE)

                AppPreferences.userHasAccount =
                        preferences.getBoolean(Constants.USER_HAS_ACCOUNT_KEY, AppPreferences.userHasAccount)

                AppPreferences.userAccountToSet =
                        preferences.getBoolean(Constants.USER_ACCOUNT_TO_SET_KEY, AppPreferences.userAccountToSet)

                AppPreferences.masterPasToSet =
                        preferences.getBoolean(Security.MASTER_PAS_TO_SET_KEY, AppPreferences.masterPasToSet)

                AppPreferences.masterPas =
                        preferences.getString(Security.MASTER_PAS_KEY, AppPreferences.masterPas)

                AppPreferences.migrationNeeded = false
            }
        }

        //Set the layout and the toolbar
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        //Set adMob
        loadAd(adView)

        launch {
            val firebaseUser = withContext(Dispatchers.IO) {
                checkIfMigratePreferences()
                isUserAccountOk()
            }

            if(firebaseUser != null) {
                runStandardStartup(firebaseUser.uid)
            }
            else {
                getDialog()
                        .title(R.string.welcome_title)
                        .message(R.string.welcome_message)
                        .positiveButton(R.string.lets_go)
                        .show{
                            onDismiss {
                                startActivityForResult<LoginActivity>(Constants.LOGIN_ACTIVITY_REQ_CODE)
                            }
                        }
            }
        }
    }

    //Verify if it's the first run or if the user has a valid local profile, or a valide Firebase
    private suspend fun isUserAccountOk(): FirebaseUser? {
        if(AppPreferences.userAccountToSet) {
            return null
        }
        else {
            val firebaseAuth = FirebaseAuth.getInstance()
            if(AppPreferences.userHasAccount) {
                return if (firebaseAuth.currentUser?.isAnonymous != false) {
                    try {
                        AuthUI.getInstance().silentSignIn(this, Constants.LOGIN_PROVIDERS)
                                .await().user
                    }
                    catch (e: Exception) {
                        logException(e)
                        null
                    }
                } else {
                    firebaseAuth.currentUser
                }
            }
            else {
                //Anyway we need an anonymous firebase user
                return if(firebaseAuth.currentUser?.isAnonymous == true) {
                    firebaseAuth.currentUser
                } else {
                    try {
                        AuthUI.getInstance().signOut(this).await()
                        firebaseAuth.signInAnonymously().await().user
                    } catch (e: Exception) {
                        logException(e)
                        null
                    }
                }
            }
        }
    }

    private suspend fun runStandardStartup(uid: String) {

        suspend fun getNotesAndCategories() {
            loadNotes()
            setBottomSheetCategories(bottomSheetCategories, bottomSheetPeek,
                    recyclerViewCategories, imageViewShowHide, viewCategoriesBackground,
                    fabNewNote)
        }

        //Return true if we can continue, false otherwise TODO test!
        fun verifyMasterPas(): Boolean {
            if(AppPreferences.masterPasToSet) {
                var password: String? = null
                getDialog()
                        .title(R.string.master_password_title)
                        .message(R.string.master_password_message)
                        .negativeButton(R.string.proceed_whitout_master_password) {
                            launch {
                                AppPreferences.masterPasToSet = false
                                getNotesAndCategories()
                            }
                        }
                        .positiveButton(R.string.set) {
                            launch {
                                withContext(Dispatchers.IO) {
                                    password?.let { password ->
                                        AppPreferences.masterPas = Security.getPasswordHash(password)
                                    }
                                    AppPreferences.masterPasToSet = false
                                }
                                getNotesAndCategories()
                            }
                        }
                        .show {
                            setActionButtonEnabled(WhichButton.POSITIVE, false)

                            val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            input(inputType = inputType, waitForPositiveButton = false) { dialog, text ->
                                val isValid = text.length >= 6
                                dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                                password = if(isValid) text.toString() else null
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                getInputField().importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                            }
                        }
                return false
            }
            else {
                return if(AppPreferences.masterPas.isNullOrEmpty()) {
                    true
                } else {
                    askMasterPassword({
                        launch { getNotesAndCategories() }
                    }, { finish() })
                    false
                }
            }
        }

        withContext(Dispatchers.Default) {
            setCrashlyticsUserId(uid)
            setFabNewNote()
        }

        setBackground(imageViewBackground)

        if(verifyMasterPas()) {
            getNotesAndCategories()
        }
    }

    private fun loadNotes() {
        recyclerViewNotes.run {
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            setNoteAdapterLayout()
            adapter = noteAdapter
        }

        getNotes(categoryAdapter.selectedCategory, notesViewModel)
    }

    //Called inside getNotes function every time we have an update
    internal fun onNotesChanged(notes: List<Note>) {
        if (loaderView.visibility != View.GONE) {
            loaderView.visibility = GONE
        }
        noteAdapter.submitNotes(notes)
    }

    private fun setFabNewNote() {
        fabNewNote.setOnClickListener {
            startNoteActivity(Note().also { note ->
                categoryAdapter.selectedCategory?.let { category ->
                    if(AppPreferences.userHasAccount) {
                        note.firestoreCatId = category.firestoreId
                    }
                    else {
                        note.category.targetId = category.id
                    }
                }
            })
        }

        recyclerViewNotes.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if(dy > 0){
                    fabNewNote.hide()
                } else{
                    fabNewNote.show()
                }

                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }

    private fun setNoteAdapterLayout() {
        recyclerViewNotes.run {
            for(i in 1..itemDecorationCount) {
                removeItemDecorationAt(0)
            }

            if(AppPreferences.oneColumn) {
                layoutManager = LinearLayoutManager(this@MainActivity)
                addItemsMargins(10, false)
            }
            else {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                addItemsMargins(5, true)
            }
        }
    }

    private fun onNoteClicked(note: Note) {
        if(note.hasPassword()) {
            getDialog()
                    .title(R.string.insert_the_password)
                    .positiveButton (R.string.open)
                    .negativeButton (R.string.undo)
                    .show {
                        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        input(inputType = inputType) { _, passwordChars ->
                            val password = passwordChars.toString()

                            if(Security.passwordMatch(note, password)) {
                                startNoteActivity(note, password)
                            }
                            else {
                                Toasty.error(this@MainActivity, R.string.wrong_password_retry).show()
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getInputField().importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                        }
                    }
        }
        else {
            startNoteActivity(note)
        }
    }

    private fun startNoteActivity(note: Note, password: String? = null, categoryName: String? = null) {
        val catName = categoryName ?: categoryAdapter.selectedCategory?.name

        startActivityForResult<NoteActivity>(Constants.NOTE_ACTIVITY_REQ_CODE,
                Note.NOTE_TAG to note,
                Note.NOTE_PAS to password,
                Category.NAME_KEY to catName)
    }

    override fun onCategoryClicked(category: Category?) {
        super.onCategoryClicked(category)
        getNotes(category, notesViewModel)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        fun onLoginRequest() {
            if(resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser?.run {
                    launch {
                        AppPreferences.userHasAccount = data?.getBooleanExtra(Constants.USER_HAS_ACCOUNT_KEY, false) ?: false
                        AppPreferences.userAccountToSet = false
                        runStandardStartup(uid)
                    }
                } ?: finish()
            }
            else {
                finish()
            }
        }

        fun onNoteRequest() {
            if(resultCode == Activity.RESULT_OK) {
                data?.run {
                    val note = getParcelableExtra<Note>(Note.NOTE_TAG)
                    val noteToDelete = getBooleanExtra(Note.NOTE_TO_DELETE, false)
                    if(noteToDelete) {
                        if(!note.isNewNote()) {
                            deleteNote(note)
                            showCafeBar(R.string.note_deleted, mainCoordLayout, CafeBar.Duration.LONG,
                                    R.string.undo to CafeBarCallback {
                                        saveNote(note)
                                        it.dismiss()
                                    })
                        }
                    }
                    else {
                        val tempSelectedCategory = Category().also {tempSelCat ->
                            tempSelCat.firestoreId = note.firestoreCatId
                            tempSelCat.id = note.category.targetId
                        }

                        if(!tempSelectedCategory.matchCategory(categoryAdapter.selectedCategory)) {
                            if(tempSelectedCategory.hasNoCategoryID()) {
                                categoryAdapter.setSelectedCategory(null)
                                onCategoryClicked(null)
                            }
                            else {
                                categoryAdapter.setSelectedCategory(tempSelectedCategory)
                                onCategoryClicked(categoryAdapter.selectedCategory)
                            }
                        }
                    }
                }
            }
            else {
                data?.run {
                    getParcelableExtra<Note>(Note.NOTE_DISCARDED)?.let {note ->
                        if(note.hasPassword()) {
                            return
                        }
                        val catName = getStringExtra(Category.NAME_KEY)
                        showCafeBar(R.string.forget_to_save_note, mainCoordLayout, duration = CafeBar.Duration.LONG,
                                action = R.string.recover to CafeBarCallback {
                                    startNoteActivity(note, categoryName = catName)
                                    it.dismiss()
                                })
                    }
                }
            }
        }

        fun onSettingsRequest() {
            val wasUserDeleted = data?.getBooleanExtra(Constants.USER_DELETED_KEY, false) ?: false
            if(resultCode == Activity.RESULT_OK) {
                setNoteAdapterLayout()
                if(wasUserDeleted) {
                    getNotes(null, notesViewModel)
                    getCategories(categoriesViewModel)
                }
                else {
                    getNotes(categoryAdapter.selectedCategory, notesViewModel)
                }
            }
        }

        fun onChangeUserAccount() {
            if(resultCode == Activity.RESULT_OK && !AppPreferences.userHasAccount
                    && FirebaseAuth.getInstance().currentUser?.isAnonymous == false) {
                migrateToFirebase()
            }
        }

        when(requestCode) {
            Constants.LOGIN_ACTIVITY_REQ_CODE -> onLoginRequest()
            Constants.NOTE_ACTIVITY_REQ_CODE -> onNoteRequest()
            Constants.SETTINGS_ACTIVITY_REQ_CODE -> onSettingsRequest()
            Constants.CHANGE_USER_ACCOUNT_ACTIVITY_REQ_CODE -> onChangeUserAccount()
        }
    }

    private fun migrateToFirebase() {
        notesViewModel.noteLiveData?.clearAll(this@MainActivity)
        categoriesViewModel.categoryLiveData?.clearAll(this@MainActivity)

        var uploadJob: Job? = null
        val snackProgressBarManager = SnackProgressBarManager(mainCoordLayout)
        snackProgressBarManager.setOnDisplayListener(object: SnackProgressBarManager.OnDisplayListener {
            override fun onDismissed(snackProgressBar: SnackProgressBar, onDisplayId: Int) {}

            override fun onShown(snackProgressBar: SnackProgressBar, onDisplayId: Int) {
                uploadJob = launch {
                    val isOk = moveDBToFirebase()
                    snackProgressBarManager.dismiss()
                    if(isOk) {
                        AppPreferences.userHasAccount = true
                        toastSuccess(R.string.migration_completed)
                    }
                    else {
                        AuthUI.getInstance().signOut(this@MainActivity).await()
                        FirebaseAuth.getInstance().signInAnonymously().await()
                    }
                    withContext(NonCancellable) {
                        getNotes(null, notesViewModel)
                        getCategories(categoriesViewModel)
                    }
                }
            }
        })

        SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.waiting_notes_migrate))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), object: SnackProgressBar.OnActionClickListener {
                    override fun onActionClick() {
                        uploadJob?.cancel()
                        snackProgressBarManager.dismiss()
                    }
                })
                .run {
                    snackProgressBarManager.show(this, SnackProgressBarManager.LENGTH_INDEFINITE, 100)
                }
    }

    private fun migrateLocally(removeNotes: Boolean) {
        notesViewModel.noteLiveData?.clearAll(this)
        categoriesViewModel.categoryLiveData?.clearAll(this)

        var downloadJob: Job? = null
        val snackProgressBarManager = SnackProgressBarManager(mainCoordLayout)
        snackProgressBarManager.setOnDisplayListener(object: SnackProgressBarManager.OnDisplayListener {
            override fun onDismissed(snackProgressBar: SnackProgressBar, onDisplayId: Int) {}

            override fun onShown(snackProgressBar: SnackProgressBar, onDisplayId: Int) {
                downloadJob = launch {
                    val isOk = moveDBLocally(removeNotes)
                    snackProgressBarManager.dismiss()
                    if(isOk) {
                        AppPreferences.userHasAccount = false
                        toastSuccess(R.string.migration_completed)
                    }
                    withContext(NonCancellable) {
                        getNotes(null, notesViewModel)
                        getCategories(categoriesViewModel)
                    }
                }
            }
        })

        SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.waiting_notes_migrate))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), object: SnackProgressBar.OnActionClickListener {
                    override fun onActionClick() {
                        downloadJob?.cancel()
                        snackProgressBarManager.dismiss()
                    }
                })
                .run {
                    snackProgressBarManager.show(this, SnackProgressBarManager.LENGTH_INDEFINITE, 100)
                }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu?.findItem(R.id.action_search)
        searchView.run {
            setMenuItem(item)

            setOnQueryTextListener(object: MaterialSearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    getNotes(categoryAdapter.selectedCategory, notesViewModel, newText)
                    return true
                }

            })

            setOnSearchViewListener(object: MaterialSearchView.SearchViewListener {
                override fun onSearchViewClosed() {
                    please(400) {
                        animate(bottomSheetCategories) toBe {
                            visible()
                            originalPosition()
                        }
                        animate(fabNewNote) toBe {
                            visible()
                            originalPosition()
                        }
                    }.start()
                }

                override fun onSearchViewShown() {
                    val bottomSheetExpanded = bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED

                    please(400) {
                        animate(bottomSheetCategories) toBe {
                            invisible()
                            outOfScreen(Gravity.LEFT)
                        }
                        animate(fabNewNote) toBe {
                            if(!bottomSheetExpanded) {
                                outOfScreen(Gravity.RIGHT)
                                invisible()
                            }
                        }
                    }.withStartAction {
                        if(bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }.thenCouldYou(100) {
                        if(bottomSheetExpanded) {
                            animate(fabNewNote) toBe {
                                outOfScreen(Gravity.RIGHT)
                                invisible()
                            }
                        }
                    }.start()
                }
            })
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        fun onAccountAction() {
            if(AppPreferences.userHasAccount) {
                val accountMessage = FirebaseAuth.getInstance().currentUser?.run {
                    displayName?.run {
                        getString(R.string.logged_as) + "\n" + this + "\n" + email
                    } ?: getString(R.string.logged_as) + "\n" + email
                }

                getDialog()
                        .title(R.string.account_info)
                        .message(text = accountMessage)
                        .positiveButton(R.string.logout) {
                            getDialog()
                                    .title(R.string.logout)
                                    .message(R.string.logout_message)
                                    .negativeButton(R.string.no)
                                    .positiveButton(R.string.yes) {
                                        getDialog()
                                                .title(R.string.notes_download)
                                                .message(R.string.notes_download_message)
                                                .negativeButton(R.string.keep) {
                                                    migrateLocally(false)
                                                }
                                                .positiveButton(R.string.erase) {
                                                    migrateLocally(true)
                                                }
                                                .show()
                                    }
                                    .show()
                        }
                        .negativeButton(R.string.close)
                        .show()
            }
            else {
                getDialog()
                        .title(R.string.no_user_account_title)
                        .message(R.string.no_user_account_message)
                        .negativeButton(R.string.no)
                        .positiveButton(R.string.yes) {
                            startActivityForResult<LoginActivity>(Constants.CHANGE_USER_ACCOUNT_ACTIVITY_REQ_CODE)
                        }
                        .show()
            }
        }

        return when(item?.itemId) {
            R.id.action_settings -> {
                startActivityForResult<SettingsActivity>(Constants.SETTINGS_ACTIVITY_REQ_CODE)
                true
            }

            R.id.action_account -> {
                onAccountAction()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if(searchView.isSearchOpen) {
            searchView.closeSearch()
        }
        else {
            super.onBackPressed()
        }
    }
}