package com.botty.secretnotes.utilities.activites

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.input.input
import com.botty.secretnotes.R
import com.botty.secretnotes.storage.adapters.CategoryAdapter
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.storage.new_db.category.CategoryViewModel
import com.botty.secretnotes.storage.storage_extensions.deleteCategory
import com.botty.secretnotes.storage.storage_extensions.getCategories
import com.botty.secretnotes.storage.storage_extensions.saveCategory
import com.botty.secretnotes.utilities.getDialog
import com.botty.secretnotes.utilities.toastError
import com.github.florent37.kotlin.pleaseanimate.please
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.bottom_sheet_main_content.*
import kotlinx.android.synthetic.main.bottom_sheet_main_title.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import org.jetbrains.anko.dip

//Used in main and in note activities. Is useful for the category shared functions
@ExperimentalCoroutinesApi
abstract class BottomSheetCategoriesActivity: OnPauseTrackActivity(), CoroutineScope by MainScope() {

    var onBackPressedPlus: (() -> Unit)? = null

    protected lateinit var bottomSheet: BottomSheetBehavior<View>

    protected val categoryAdapter by lazy {
        CategoryAdapter(this).apply {
            onItemClick = this@BottomSheetCategoriesActivity::onCategoryClicked
        }
    }

    protected val categoriesViewModel: CategoryViewModel by lazy {
        object : CategoryViewModel() {
            override fun onCategoriesChanged(categories: List<Category>) {
                categoryAdapter.categories = categories
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        categoryAdapter.setSelectedCategory(savedInstanceState?.getParcelable(Category.SELECTED_CATEGORY_KEY))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(Category.SELECTED_CATEGORY_KEY, categoryAdapter.selectedCategory)
        super.onSaveInstanceState(outState)
    }

    protected fun setBottomSheetCategories(bottomSheetCategories: View, bottomSheetPeek: View,
                                           recyclerViewCategories: RecyclerView, imageViewShowHide: ImageView,
                                           viewCategoriesBackground: View,
                                           fabAction: FloatingActionButton? = null,
                                           isEditMode: Boolean = true) {

        fun setAnimation() {
            bottomSheet = BottomSheetBehavior.from(bottomSheetCategories)

            bottomSheetPeek.setOnClickListener {
                when(bottomSheet.state) {
                    BottomSheetBehavior.STATE_EXPANDED -> bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    BottomSheetBehavior.STATE_COLLAPSED -> bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            please {
                animate(recyclerViewCategories) toBe {
                    scale(0F, 0F, Gravity.CENTER, Gravity.TOP)
                }
            }.now()

            val upDownArrowAnimation = please {
                animate(imageViewShowHide) toBe {
                    toBeRotated(180F)
                }
                animate(viewCategoriesBackground) toBe {
                    alpha(0.95F)
                }
                animate(recyclerViewCategories) toBe {
                    originalScale()
                    alpha(1F)
                }
                fabAction?.run {
                    animate(this) toBe {
                        outOfScreen(Gravity.LEFT)
                        toBeRotated(360F)
                        invisible()
                    }
                }
            }

            bottomSheet.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(p0: View, p1: Int) {}

                override fun onSlide(p0: View, slidePercent: Float) {
                    upDownArrowAnimation.setPercent(slidePercent)
                }

            })
        }

        fun loadCategories() {
            recyclerViewCategories.run {
                //setHasFixedSize(false)
                layoutManager = FlexboxLayoutManager(this@BottomSheetCategoriesActivity)
                        .apply {
                            flexDirection = FlexDirection.ROW
                            justifyContent = JustifyContent.SPACE_AROUND
                        }
                adapter = categoryAdapter
            }

            getCategories(categoriesViewModel)
        }

        fun setIsCategoriesEditEnabled() {
            if(isEditMode) {
                layoutCategoriesButtons.visibility = View.VISIBLE
                setCategoriesButtons()
            }
            else {
                layoutCategoriesButtons.visibility = View.GONE
                recyclerViewCategories.updatePadding(bottom = dip(12))
            }
        }

        setAnimation()
        loadCategories()
        setIsCategoriesEditEnabled()
    }

    private fun setCategoriesButtons() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS

        buttonRemoveCategory.setOnClickListener {
            categoryAdapter.selectedCategory?.let {category ->
                val message = getString(R.string.remove_category_message) + " \"" +
                        category.name + "\" ?"

                getDialog()
                        .title(R.string.remove_category)
                        .message(text = message)
                        .negativeButton(R.string.no)
                        .positiveButton(R.string.yes) {
                            getDialog()
                                    .title(R.string.remove_category)
                                    .message(R.string.remove_category_notes_message)
                                    .negativeButton(R.string.keep) {
                                        deleteCategory(category, true) {
                                            onCategoryClicked(null)
                                        }
                                    }
                                    .positiveButton(R.string.remove) {
                                        deleteCategory(category, false) {
                                            onCategoryClicked(null)
                                        }
                                    }.show()
                        }.show()

            } ?: Toasty.error(this, R.string.select_a_category).show()
        }

        fun addRenameCategory(category: Category, setSelected: Boolean = false) {
            saveCategory(category)?.run {
                if(setSelected) {
                    isSelected = true
                    categoryAdapter.setSelectedCategory(this, forceSelected = true)
                }
                onCategoryClicked(this)
            }
        }

        buttonRenameCategory.setOnClickListener {
            categoryAdapter.selectedCategory?.let { category ->
                val message = getString(R.string.rename_category_message) +
                        " \"" + category.name + "\":"
                getDialog()
                        .title(R.string.rename_category)
                        .message(text = message)
                        .negativeButton(R.string.cancel)
                        .show {
                            input(inputType = inputType) { _, charSequence ->
                                val newName = charSequence.toString().trim()
                                if(existCategoryWithSameName(newName)) {
                                    toastError(R.string.category_name_exist)
                                }
                                else {
                                    addRenameCategory(category.getCopy().apply { name = newName })
                                }
                            }
                        }
            } ?: Toasty.error(this, R.string.select_a_category).show()
        }

        buttonAddCategory.setOnClickListener {
            getDialog()
                    .title(R.string.add_category)
                    .message(R.string.insert_category_name)
                    .negativeButton(R.string.cancel)
                    .show {
                        input(inputType = inputType) { _, charSequence ->
                            val newName = charSequence.toString().trim()
                            if(existCategoryWithSameName(newName)) {
                                toastError(R.string.category_name_exist)
                            }
                            else {
                                addRenameCategory(Category(newName), true)
                            }
                        }
                    }
        }
    }

    open fun onCategoryClicked(category: Category?) {
        textViewCategoryTitle.text = category?.name ?: getString(R.string.categories)
    }

    override fun onBackPressed() {
        if(bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        else {
            onBackPressedPlus?.invoke() ?: super.onBackPressed()
        }
    }

    private fun existCategoryWithSameName(name: String): Boolean {
        return categoryAdapter.categories.find {category ->
            category.name.equals(name, true)
        }?.run {true} ?: false
    }
}