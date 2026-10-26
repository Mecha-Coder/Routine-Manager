package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem

/** Changes to the To-Do, Self and Room outline lists (stored flat, a child after its parent). */
object OutlineOps {

    /** README › numbered outline lists: one level of children. */
    const val MAX_DEPTH = 1

    /** The To-Do badge: items not done, parents and children alike (README › List menu). */
    fun openCount(items: List<OutlineItem>): Int = items.count { !it.done }

    /** Done items stay in place; only the tapped item changes, not its children (README › To-Do). */
    fun toggleDone(items: List<OutlineItem>, id: String): List<OutlineItem> =
        items.map { if (it.id == id) it.copy(done = !it.done) else it }

    /** The index just past the item at [index] and its children (the prototype's `subEnd`). */
    fun subtreeEnd(items: List<OutlineItem>, index: Int): Int {
        var end = index + 1
        while (end < items.size && items[end].depth > items[index].depth) end++
        return end
    }

    /**
     * Each depth is at most one more than the item above and at most [MAX_DEPTH], so no child is
     * left without a parent (the prototype's `normalize`).
     */
    fun normalize(items: List<OutlineItem>): List<OutlineItem> {
        var previous = -1
        return items.map {
            val depth = it.depth.coerceAtMost(previous + 1).coerceIn(0, MAX_DEPTH)
            previous = depth
            if (depth == it.depth) it else it.copy(depth = depth)
        }
    }

    fun editText(items: List<OutlineItem>, id: String, text: String): List<OutlineItem> =
        items.map { if (it.id == id) it.copy(text = text) else it }

    /**
     * Enter: a new empty item [newId] after the item [id] and its children, at the item's depth
     * (README › Edit mode).
     */
    fun insertAfter(items: List<OutlineItem>, id: String, newId: String): List<OutlineItem> {
        val i = items.indexOfFirst { it.id == id }
        if (i < 0) return items
        return items.toMutableList().apply { add(subtreeEnd(items, i), OutlineItem(newId, "", items[i].depth)) }
    }

    /** "+ Add task" / "+ Add item": a new empty top-level item at the end. */
    fun append(items: List<OutlineItem>, newId: String): List<OutlineItem> = items + OutlineItem(newId, "")

    /** Deleting a parent promotes its children up one level (README › Edit mode). */
    fun delete(items: List<OutlineItem>, id: String): List<OutlineItem> {
        val i = items.indexOfFirst { it.id == id }
        if (i < 0) return items
        val end = subtreeEnd(items, i)
        val promoted = items.mapIndexed { k, it -> if (k in i + 1 until end) it.copy(depth = it.depth - 1) else it }
        return normalize(promoted.filterIndexed { k, _ -> k != i })
    }

    /**
     * An indent drag released at [depth] (README › Drag handle gestures): only the item changes. An
     * indented parent becomes a child of the item above and its children stay at depth 1; an
     * outdented child becomes a parent of the children after it.
     */
    fun setDepth(items: List<OutlineItem>, id: String, depth: Int): List<OutlineItem> =
        normalize(items.map { if (it.id == id) it.copy(depth = depth) else it })

    /**
     * A reorder drop: the item and its children move together to [index] in the list without them.
     * The item takes [depth], and its children shift by as much (README › Drag handle gestures).
     */
    fun moveSubtree(items: List<OutlineItem>, id: String, index: Int, depth: Int): List<OutlineItem> {
        val from = items.indexOfFirst { it.id == id }
        if (from < 0) return items
        val end = subtreeEnd(items, from)
        val delta = depth - items[from].depth
        val moved = items.subList(from, end).map { it.copy(depth = (it.depth + delta).coerceIn(0, MAX_DEPTH)) }
        val rest = items.filterIndexed { k, _ -> k !in from until end }
        val at = index.coerceIn(0, rest.size)
        return normalize(rest.subList(0, at) + moved + rest.subList(at, rest.size))
    }

    /**
     * When leaving edit mode or the list (README › Saving edits): items with no text (or only
     * spaces) are dropped and depths normalised. Kept text is not trimmed, as in the prototype.
     */
    fun cleanup(items: List<OutlineItem>): List<OutlineItem> = normalize(items.filter { it.text.isNotBlank() })
}
