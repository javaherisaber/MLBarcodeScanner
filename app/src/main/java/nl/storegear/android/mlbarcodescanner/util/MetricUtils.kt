package nl.storegear.android.mlbarcodescanner.util

import android.content.res.Resources

object MetricUtils {
    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}