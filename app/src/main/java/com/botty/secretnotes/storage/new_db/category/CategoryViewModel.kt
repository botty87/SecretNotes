package com.botty.secretnotes.storage.new_db.category

import androidx.lifecycle.ViewModel

abstract class CategoryViewModel: ViewModel() {
    var categoryLiveData: CategoryLiveData? = null

    abstract fun onCategoriesChanged(categories: List<Category>)
}