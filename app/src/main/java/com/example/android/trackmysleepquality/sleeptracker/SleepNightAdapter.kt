package com.example.android.trackmysleepquality.sleeptracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.databinding.ListItemSleepNightBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1
/**
 * Created by Nicholas Fragiskatos on 4/20/2020.
 */

/*
Changed this from extending RecyclerView.Adapter to ListAdapter. ListAdapter is a subclass and
specializes in handling lists of things.

Added a SleepNightListener in the constructor so we can pass a clickListener down to the ViewHolder so
it can set the clickListener property in our binding.
 */
class SleepNightAdapter(val clickListener: SleepNightListener): ListAdapter<DataItem, RecyclerView.ViewHolder>(SleepNightDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> TextViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> ViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val nightItem = getItem(position) as DataItem.SleepNightItem
                holder.bind(nightItem.sleepNight, clickListener)
            }
        }
    }

    /*
    Helper function for the fragment when it is observing the backing data list from the ViewModel

    The backing list is still a list of SleepNight objects, so when we set the source list for the adapter
    to the updated data from the ViewModel, we need to map List<SleepNight> -> List<DataItem> where
    the first value in the list is a DataItem.Header, and the rest are just DataItem.SleepNightItem
     */
    fun addHeaderAndSubmitList(list: List<SleepNight>) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(DataItem.Header)
                else -> listOf(DataItem.Header) + list.map { DataItem.SleepNightItem(it) }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    /*
    Need to override this function from Adapter class.

    We determined that our header will be displayed at the beginning of the list, so if the position
    is 0 then the View is a header, else it's a SleepNight.
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DataItem.SleepNightItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    class ViewHolder private constructor(val binding: ListItemSleepNightBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SleepNight, clickListener: SleepNightListener) {
            binding.sleepNight = item
            binding.executePendingBindings()
            binding.clickListener = clickListener
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                /*
                Essentially boilerplate code.
                The important part is to supply the correct layout resource
                 */
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSleepNightBinding.inflate(layoutInflater, parent, false)
                val view = layoutInflater.inflate(R.layout.list_item_sleep_night, parent, false)
                return ViewHolder(binding)
            }
        }
    }

    /*
    Similar to our other view holder, except simpler. We just have a predefined layout that's fully
    defined, so all we need to do is inflate the view and return it.

    Using the similar companion object from function pattern.
     */
    class TextViewHolder(view: View): RecyclerView.ViewHolder(view) {
        companion object {
            fun from(parent: ViewGroup): TextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.header, parent, false)
                return TextViewHolder(view)
            }
        }
    }
}

/*
Our DiffUtil must be updated because we are no longer working on just SleepNight items, but the more
generalized DataItem.

Since DataItem handles how it defines its IDs, and it implements its own hash code function we don't
really have to do anything except change the argument types from SleepNight to DataItem.
 */
class SleepNightDiffCallback: DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        /*
        Can do this cause we defined SleepNight as a data class, which automagically provides the
        equal() functionality. It will implicitly check all field values.
         */
        return oldItem == newItem
    }
}

/*
Just a fancy wrapper for a lambda expression. Will be convenient to use this cause we need to pass it
around as a variable.
 */
class SleepNightListener(val clickListener: (sleepId: Long) -> Unit) {
    fun onClick(night: SleepNight) = clickListener(night.nightId)
}

/*
We want to generalise our RecyclerView/ListAdapter a bit more to make room for a header in our list.

The RecyclerView will now just take a general ViewHolder
The ListAdapter will take this new DataItem instead of a specific SleepNight

We will introduce logic to handle each case differently.

Use a sealed class to enforce a strict class hierarchy. Guarantee to the ListAdapter that its
data will be of one of these types.
 */
sealed class DataItem {
    abstract val id: Long

    data class SleepNightItem(val sleepNight: SleepNight) : DataItem() {
        override val id = sleepNight.nightId
    }

    object Header : DataItem() {
        override val id = Long.MIN_VALUE
    }
}