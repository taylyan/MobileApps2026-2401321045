package com.huntersstory.app

import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.ui.journal.JournalActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HuntingJournalFlowTest {
    private val testLocation =
        "Espresso-${UUID.randomUUID().toString().take(8)}"

    private var testUserId: String? = null

    @Before
    fun signInTestUser() {
        val arguments = InstrumentationRegistry.getArguments()

        val email =
            arguments
                .getString("testEmail")
                ?.takeIf { it.isNotBlank() }
                ?: error("Missing testEmail instrumentation argument")

        val password =
            arguments
                .getString("testPassword")
                ?.takeIf { it.isNotBlank() }
                ?: error("Missing testPassword instrumentation argument")

        val result =
            Tasks.await(
                FirebaseAuth
                    .getInstance()
                    .signInWithEmailAndPassword(email, password),
                30,
                TimeUnit.SECONDS,
            )

        testUserId = requireNotNull(result.user).uid
    }

    @Test
    fun createHuntingTripAndVerifyItInJournal() {
        val today =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US,
            ).format(Date())

        ActivityScenario
            .launch(
                JournalActivity::class.java,
            ).use {
                onView(withId(R.id.btnAddHunt))
                    .check(matches(isDisplayed()))
                    .perform(click())

                onView(withId(R.id.etHuntDate))
                    .perform(
                        replaceText(today),
                        closeSoftKeyboard(),
                    )

                onView(withId(R.id.etHuntLocation))
                    .perform(
                        replaceText(testLocation),
                        closeSoftKeyboard(),
                    )

                onView(withId(R.id.etGameSpecies))
                    .perform(
                        replaceText("Quail"),
                        closeSoftKeyboard(),
                    )

                onView(withId(R.id.etHuntNotes))
                    .perform(
                        replaceText("Espresso UI test"),
                        closeSoftKeyboard(),
                    )

                onView(withId(R.id.btnSaveHunt))
                    .perform(click())

                // Wait for AddHuntActivity to finish.
                waitUntilDisplayed(
                    withId(R.id.btnAddHunt),
                )

                // Verify that the Room record
                // appears in the Journal UI.
                waitUntilDisplayed(
                    allOf(
                        withId(R.id.tvTripLocation),
                        withText(testLocation),
                    ),
                )
            }
    }

    @After
    fun cleanUp() {
        try {
            val userId = testUserId ?: return

            val context =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext

            val dao =
                HunterDatabase
                    .getDatabase(context)
                    .huntingTripDao()

            runBlocking {
                dao
                    .getTripsByUser(userId)
                    .first()
                    .filter { trip ->
                        trip.location == testLocation
                    }.forEach { trip ->
                        dao.delete(trip)
                    }
            }
        } finally {
            if (testUserId != null) {
                FirebaseAuth.getInstance().signOut()
            }
        }
    }

    private fun waitUntilDisplayed(matcher: Matcher<View>) {
        val deadline =
            SystemClock.elapsedRealtime() + 10_000L

        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                onView(matcher)
                    .check(matches(isDisplayed()))
                return
            } catch (_: NoMatchingViewException) {
                // The screen has not updated yet.
            } catch (_: AssertionError) {
                // The matching view is not visible yet.
            }

            SystemClock.sleep(250L)
        }

        onView(matcher)
            .check(matches(isDisplayed()))
    }
}
