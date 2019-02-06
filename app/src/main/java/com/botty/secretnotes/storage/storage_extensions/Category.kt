package com.botty.secretnotes.storage.storage_extensions

import androidx.lifecycle.Observer
import com.botty.secretnotes.R
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.storage.new_db.category.CategoryLiveData
import com.botty.secretnotes.storage.new_db.category.CategoryViewModel
import com.botty.secretnotes.storage.new_db.category.Category_
import com.botty.secretnotes.storage.new_db.note.Note
import com.botty.secretnotes.utilities.activites.BottomSheetCategoriesActivity
import com.botty.secretnotes.utilities.await
import com.botty.secretnotes.utilities.logException
import com.botty.secretnotes.utilities.toastError
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import es.dmoral.toasty.Toasty
import io.objectbox.exception.UniqueViolationException
import io.objectbox.kotlin.query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
fun BottomSheetCategoriesActivity.saveCategory(category: Category): Category? {
    category.name = category.name.toUpperCase()
    return if(AppPreferences.userHasAccount) {
        val categoryDoc = if(category.firestoreId.isNullOrBlank()) {
            getCategoriesCollection().document().apply {
                category.firestoreId = id
            }
        }
        else {
            getCategoriesCollection().document(category.firestoreId!!)
        }
        categoryDoc.set(Category.getFirestoreMap(category))
        category
    } else {
        try {
            category.id = getCategoriesBox().put(category)
            category
        } catch (e: UniqueViolationException) {
            Toasty.error(this, getString(R.string.category_name_exist)).show()
            null
        }
    }
}

@ExperimentalCoroutinesApi
fun BottomSheetCategoriesActivity.getCategories(categoryViewModel: CategoryViewModel) {
    categoryViewModel.categoryLiveData?.clearAll(this)

    if(AppPreferences.userHasAccount) {
        val query = getCategoriesCollection().orderBy(Category.NAME_KEY)
        categoryViewModel.categoryLiveData = CategoryLiveData(query).apply {
            observe(this@getCategories, Observer(categoryViewModel::onCategoriesChanged))
        }
    }
    else {
        val query = getCategoriesBox().query {
            order(Category_.name)
        }

        categoryViewModel.categoryLiveData = CategoryLiveData(query).apply {
            observe(this@getCategories, Observer(categoryViewModel::onCategoriesChanged))
        }
    }
}

@ExperimentalCoroutinesApi
fun BottomSheetCategoriesActivity.deleteCategory(category: Category, keepNotes: Boolean,
                                                 onCategoryDeleted: (() -> Unit)) {
    if(AppPreferences.userHasAccount) {
        category.firestoreId?.let {categoryId ->

            suspend fun deleteCategory(snackProgressBarManager: SnackProgressBarManager) {
                val noteDocuments =
                try {
                    getNotesCollection()
                            .whereEqualTo(Note.FIRESTORE_CAT_ID_KEY, categoryId)
                            .get().await().documents
                }catch (e: Exception) {
                    if(e !is CancellationException) {
                        toastError(R.string.error_while_deleting_category)
                        logException(e)
                    }
                    return
                }finally {
                    snackProgressBarManager.dismiss()
                }

                withContext(NonCancellable) {
                    val firestoreBatch = getFirestoreWriteBatch()
                    if(keepNotes) {
                        noteDocuments.forEach {noteDocument ->
                            firestoreBatch.update(noteDocument.reference, Note.FIRESTORE_CAT_ID_KEY, null)
                        }
                    }
                    else {
                        noteDocuments.forEach {noteDocument ->
                            firestoreBatch.delete(noteDocument.reference)
                        }
                    }
                    getCategoriesCollection().document(categoryId).run {
                        firestoreBatch.delete(this)
                    }

                    firestoreBatch.commit()
                    onCategoryDeleted.invoke()
                }
            }

            var deleteJob: Job? = null
            val snackProgressBarManager = SnackProgressBarManager(mainCoordLayout)
            snackProgressBarManager.setOnDisplayListener(object: SnackProgressBarManager.OnDisplayListener {
                override fun onDismissed(snackProgressBar: SnackProgressBar, onDisplayId: Int) {}

                override fun onShown(snackProgressBar: SnackProgressBar, onDisplayId: Int) {
                    deleteJob = launch {
                        deleteCategory(snackProgressBarManager)
                    }
                }

            })

            SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.deleting_category))
                    .setIsIndeterminate(true)
                    .setAction(getString(R.string.cancel), object: SnackProgressBar.OnActionClickListener{
                        override fun onActionClick() {
                            deleteJob?.cancel()
                            snackProgressBarManager.dismiss()
                        }

                    })
                    .run {
                        snackProgressBarManager.show(this, SnackProgressBarManager.LENGTH_INDEFINITE, 100)
                    }
        }
    }
    else {
        if(keepNotes) {
            getCategoriesBox().remove(category)
        }
        else {
            val notes = category.notes.toList()
            getCategoriesBox().remove(category)
            getNotesBox().remove(notes)
        }
        onCategoryDeleted.invoke()
    }
}