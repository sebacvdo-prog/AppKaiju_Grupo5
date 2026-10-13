package com.example.appkaiju_grupo5

import com.example.appkaiju_grupo5.ui.utils.WindowSize
import com.example.appkaiju_grupo5.ui.utils.calculateWindowSize
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowSizeUtilsTest {
    @Test
    fun compactEndsBefore600Dp() {
        assertEquals(WindowSize.Compact, calculateWindowSize(360))
        assertEquals(WindowSize.Compact, calculateWindowSize(599))
    }

    @Test
    fun mediumIncludes600DpAndEndsBefore840Dp() {
        assertEquals(WindowSize.Medium, calculateWindowSize(600))
        assertEquals(WindowSize.Medium, calculateWindowSize(839))
    }

    @Test
    fun expandedStartsAt840Dp() {
        assertEquals(WindowSize.Expanded, calculateWindowSize(840))
        assertEquals(WindowSize.Expanded, calculateWindowSize(1200))
    }
}
