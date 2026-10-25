package com.jsk.routinemanager.data

/** The prototype's `sample()`: loaded on first run and by the debug reset (SPEC D8). */
object SampleData {

    private class Row(val start: String, val end: String, val title: String, val subs: List<String>)

    private val weekday = listOf(
        Row("06:00", "07:00", "Morning", listOf("Wake up", "Stretch", "Shower", "Breakfast")),
        Row("07:00", "08:00", "Commute", listOf("Podcast", "Plan the day")),
        Row("08:00", "12:00", "Work Session1", listOf("To-do list", "Session + Prompt", "Read / Learn", "Drink")),
        Row("12:00", "13:00", "Lunch", listOf("Eat", "Short walk")),
        Row("13:00", "17:30", "Work Session2", listOf("Session + Prompt", "Review", "Drink")),
        Row("17:30", "18:30", "Commute", listOf("Music", "Pick up groceries")),
        Row("18:30", "20:00", "Dinner & Chores", listOf("Cook", "Dishes", "Tidy room")),
        Row("20:00", "22:30", "Personal", listOf("Read / Learn", "Journal")),
        Row("22:30", "23:00", "Wind down", listOf("Floss teeth", "Lights off")),
    )

    private val weekend = listOf(
        Row("08:00", "09:00", "Morning", listOf("Wake up", "Stretch", "Breakfast")),
        Row("09:00", "12:00", "Work Session1", listOf("To-do list", "Session + Prompt", "Read / Learn", "Drink")),
        Row("14:00", "17:00", "Errands", listOf("Grocery", "Clean room")),
        Row("19:00", "21:30", "Personal", listOf("Read / Learn", "Call family")),
        Row("22:30", "23:00", "Wind down", listOf("Floss teeth", "Lights off")),
    )

    private fun minutes(hhmm: String): Int {
        val (h, m) = hhmm.split(':').map(String::toInt)
        return h * 60 + m
    }

    fun create(): AppData {
        // One group per row, so each row's copies for Mon–Fri (or Sat–Sun) form one multi-day block.
        val weekdayGroups = weekday.map { newId() }
        val weekendGroups = weekend.map { newId() }
        val blocks = (0..6).flatMap { day ->
            val (rows, groups) = if (day < 5) weekday to weekdayGroups else weekend to weekendGroups
            rows.mapIndexed { k, r ->
                Block(newId(), groups[k], day, minutes(r.start), minutes(r.end), r.title, r.subs)
            }
        }
        fun item(text: String, depth: Int = 0, done: Boolean = false) = OutlineItem(newId(), text, depth, done)
        fun grocery(text: String, on: Boolean = false) = GroceryItem(newId(), text, on)
        return AppData(
            todo = listOf(
                item("Clean room"),
                item("Sabah"),
                item("Complete places", depth = 1),
                item("Itinerary", depth = 1),
                item("Find future room + strategy", done = true),
            ),
            grocery = listOf(
                GroceryCategory(newId(), "Toiletries", items = listOf(grocery("Others"), grocery("Chemical", on = true))),
                GroceryCategory(newId(), "Food", items = listOf(grocery("Egg"), grocery("Sausage"))),
            ),
            self = listOf("Cut Hair", "Cut Nail", "Floss teeth").map { item(it) },
            room = listOf("Change bedsheet", "Vacuum floor", "Water plants").map { item(it) },
            blocks = blocks,
        )
    }
}
