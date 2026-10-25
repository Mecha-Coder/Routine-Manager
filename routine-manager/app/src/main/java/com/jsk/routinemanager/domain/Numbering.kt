package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem

/** The outline lists' automatic numbers (README › To-Do, Self, Room: numbered outline lists). */
object Numbering {

    /**
     * One label per item, in order: `1)`, `2)`, and children `2.1)`, `2.2)`. The child counter
     * restarts under each parent. Done items keep their number. A child with no parent above it
     * (never in normalised data) is numbered under 1, as in the prototype, rather than `0.1)`.
     */
    fun labels(items: List<OutlineItem>): List<String> {
        var parent = 0
        var child = 0
        return items.map {
            if (it.depth == 0) {
                parent++
                child = 0
                "$parent)"
            } else {
                child++
                "${parent.coerceAtLeast(1)}.$child)"
            }
        }
    }
}
