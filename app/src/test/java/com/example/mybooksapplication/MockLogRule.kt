package com.example.mybooksapplication

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MockLogRule: TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                mockkStatic(Log::class)

                // common 2-arg overloads
                every { Log.v(any<String>(), any<String>()) } returns 0
                every { Log.d(any<String>(), any<String>()) } returns 0
                every { Log.i(any<String>(), any<String>()) } returns 0
                every { Log.w(any<String>(), any<String>()) } returns 0
                every { Log.e(any<String>(), any<String>()) } returns 0
                every { Log.wtf(any<String>(), any<String>()) } returns 0

                // common 3-arg overloads (message + throwable)
                every { Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
                every { Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
                every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
                every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
                every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
                every { Log.wtf(any<String>(), any<String>(), any<Throwable>()) } returns 0

                try {
                    base.evaluate()
                } catch (e: Throwable) {
                    throw e
                }
            }
        }
    }
}
