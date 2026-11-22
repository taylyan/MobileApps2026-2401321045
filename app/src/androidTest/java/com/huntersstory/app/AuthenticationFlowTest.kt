
package com.huntersstory.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.ui.login.LoginActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationFlowTest {

    @Before
    fun prepare() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun loginToRegisterNavigationWorks() {

        ActivityScenario.launch(
            LoginActivity::class.java
        ).use {

            onView(withId(R.id.etEmail))
                .check(matches(isDisplayed()))

            onView(withId(R.id.etPassword))
                .check(matches(isDisplayed()))

            onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()))

            onView(withId(R.id.tvRegister))
                .perform(click())

            onView(withId(R.id.etRegisterEmail))
                .check(matches(isDisplayed()))

            onView(withId(R.id.etRegisterPassword))
                .check(matches(isDisplayed()))

            onView(withId(R.id.etConfirmPassword))
                .check(matches(isDisplayed()))

            onView(withId(R.id.btnRegister))
                .check(matches(isDisplayed()))
        }
    }
}
