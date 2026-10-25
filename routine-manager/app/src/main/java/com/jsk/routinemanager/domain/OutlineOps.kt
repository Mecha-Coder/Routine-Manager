package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem

/** Changes to the To-Do, Self and Room outline lists (stored flat, a child after its parent). */
object OutlineOps {

    /** The To-Do badge: items not done, parents and children alike (README › List menu). */
    fun openCount(items: List<OutlineItem>): Int = items.count { !it.done }

    /** Done items stay in place; only the tapped item changes, not its children (README › To-Do). */
    fun toggleDone(items: List<OutlineItem>, id: String): List<OutlineItem> =
        items.map { if (it.id == id) it.copy(done = !it.done) else it }
}
