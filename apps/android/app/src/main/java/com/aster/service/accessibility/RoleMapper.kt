package com.aster.service.accessibility

/**
 * Pure className + a11y-flag → SPEC `role` token derivation.
 *
 * Role vocabulary (cross-phase contract; P5/P6 teach these strings):
 *   button | edittext | checkbox | switch | radio | image | link |
 *   text | listitem | tab | menuitem | scrollview | other
 *
 * Device-free → fully unit-testable.
 */
object RoleMapper {

    fun roleOf(
        className: String,
        isEditable: Boolean,
        isCheckable: Boolean,
        isClickable: Boolean,
        isLongClickable: Boolean,
        isScrollable: Boolean,
        hasText: Boolean,
    ): String {
        val cn = className.lowercase()

        // 1. Editable is the strongest signal (an input field, regardless of className).
        if (isEditable || cn.endsWith("edittext") || cn.contains("autocompletetextview")) {
            return "edittext"
        }

        // 2. Toggle widgets — switch before checkbox/radio (a switch is also checkable).
        if (isCheckable || cn.endsWith("switch") || cn.endsWith("checkbox") ||
            cn.endsWith("radiobutton") || cn.contains("togglebutton")
        ) {
            return when {
                cn.contains("switch") || cn.contains("togglebutton") -> "switch"
                cn.contains("radio") -> "radio"
                else -> "checkbox"
            }
        }

        // 3. Tabs / menu items (navigation affordances).
        if (cn.contains("tab")) return "tab"
        if (cn.contains("menuitem")) return "menuitem"

        // 4. Scroll containers.
        if (isScrollable || cn.contains("recyclerview") || cn.contains("scrollview") ||
            cn.endsWith("listview") || cn.contains("viewpager")
        ) {
            return "scrollview"
        }

        // 5. Buttons by className.
        if (cn.endsWith("button") || cn.contains("imagebutton")) return "button"

        // 6. Links / images.
        if (cn.contains("textview") && cn.contains("link")) return "link"
        if (cn.endsWith("imageview") || (cn.contains("image") && !hasText)) {
            return "image"
        }

        // 7. Clickable rows with text (cards, list rows) → listitem.
        if ((isClickable || isLongClickable) && hasText) return "listitem"

        // 8. Plain clickable with no text → button-ish actionable.
        if (isClickable || isLongClickable) return "button"

        // 9. Anything with text and no interaction → static text.
        if (hasText) return "text"

        return "other"
    }
}
