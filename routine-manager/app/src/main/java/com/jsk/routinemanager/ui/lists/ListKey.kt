package com.jsk.routinemanager.ui.lists

import androidx.annotation.DrawableRes
import com.jsk.routinemanager.R

/** The four lists, in the list menu's order (README › List menu). */
enum class ListKey(val label: String, @param:DrawableRes val icon: Int) {
    TODO("To-Do", R.drawable.ic_format_list_bulleted),
    GROCERY("Grocery", R.drawable.ic_shopping_cart),
    SELF("Self", R.drawable.ic_person),
    ROOM("Room", R.drawable.ic_home),
}
