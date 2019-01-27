package com.botty.secretnotes.storage.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.botty.secretnotes.R
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.utilities.getColorStateListCompat
import kotlinx.android.synthetic.main.category_item.view.*

class CategoryAdapter(val context: Context) : RecyclerView.Adapter<CategoryAdapter.CategoryHolder>() {

    private var selectedPosition: Int? = null

    var selectedCategory: Category? = null
        private set(value) {
            field = value
        }

    var onItemClick: ((Category?) -> Unit)? = null

    var onCategoriesChanged: ((List<Category>) -> Unit)? = null

    var categories: List<Category> = emptyList()
        set(categories) {
            onCategoriesChanged?.invoke(categories)

            field = categories
            selectedCategory?.run {
                selectedCategory = categories.find {category ->
                    this.matchCategory(category)
                }?.apply {
                    isSelected = true
                }
            }
            notifyDataSetChanged()
        }

    private val selectedColor by lazy {
        ContextCompat.getColor(context, R.color.accent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
        return CategoryHolder(LayoutInflater.from(context)
                .inflate(R.layout.category_item, parent, false))
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        fun setColors(isSelected: Boolean, textView: TextView) {
            if(isSelected) {
                textView.setTextColor(Color.WHITE)
                textView.backgroundTintList = context.getColorStateListCompat(R.color.accent)
                selectedPosition = holder.adapterPosition
            }
            else {
                textView.setTextColor(selectedColor)
                textView.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }

        categories[position].run {
            holder.view.textViewCategoryName.let {textView ->
                textView.text = name
                setColors(isSelected, textView)

                textView.setOnClickListener {
                    holder.adapterPosition.run {
                        setSelectedCategory(categories[this], this)
                    }
                    onItemClick?.invoke(selectedCategory)
                }
            }
        }
    }

    fun setSelectedCategory(newSelectedCategory: Category?, pos: Int? = null, forceSelected: Boolean = false) {
        if(forceSelected) {
            selectedCategory = newSelectedCategory
            return
        }

        if(selectedCategory == newSelectedCategory) {
            if(selectedCategory == null && newSelectedCategory == null) {
                return
            }
            selectedCategory?.isSelected = false
            selectedPosition = null
            selectedCategory = null
            pos?.run { notifyItemChanged(this) } ?: notifyDataSetChanged()
        }
        else {
            selectedCategory?.isSelected = false
            selectedPosition?.run { notifyItemChanged(this) }

            if(pos != null && categories[pos].matchCategory(newSelectedCategory)) {
                categories[pos].isSelected = true
                notifyItemChanged(pos)
                selectedCategory = categories[pos]
                selectedPosition = pos
            }
            else if(newSelectedCategory != null) {
                categories.indexOfFirst { category ->
                    category.matchCategory(newSelectedCategory)
                }.let { catPos ->
                    categories[catPos].isSelected = true
                    notifyItemChanged(catPos)
                    selectedCategory = categories[catPos]
                    selectedPosition = catPos
                }
            }
            else {
                selectedCategory = null
                selectedPosition = null
            }
        }
    }

    inner class CategoryHolder(val view: View) : RecyclerView.ViewHolder(view)
}