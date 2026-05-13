package ru.akarakuts.russiancheckers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test that the instrumentation runner targets this application id. */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun packageName() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("ru.akarakuts.russiancheckers", ctx.packageName)
    }
}
