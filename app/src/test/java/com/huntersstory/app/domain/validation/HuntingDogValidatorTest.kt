
package com.huntersstory.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HuntingDogValidatorTest {
    @Test
    fun validDogReturnsNull() {
        assertNull(
            HuntingDogValidator.validate(
                "Rex",
                "German Shorthaired Pointer",
                3,
            ),
        )
    }

    @Test
    fun emptyNameReturnsError() {
        assertEquals(
            "Dog name is required.",
            HuntingDogValidator.validate(
                "",
                "Pointer",
                3,
            ),
        )
    }

    @Test
    fun blankNameReturnsError() {
        assertEquals(
            "Dog name is required.",
            HuntingDogValidator.validate(
                "   ",
                "Pointer",
                3,
            ),
        )
    }

    @Test
    fun emptyBreedReturnsError() {
        assertEquals(
            "Breed is required.",
            HuntingDogValidator.validate(
                "Rex",
                "",
                3,
            ),
        )
    }

    @Test
    fun nullAgeReturnsError() {
        assertEquals(
            "Age must be between 0 and 30.",
            HuntingDogValidator.validate(
                "Rex",
                "Pointer",
                null,
            ),
        )
    }

    @Test
    fun negativeAgeReturnsError() {
        assertEquals(
            "Age must be between 0 and 30.",
            HuntingDogValidator.validate(
                "Rex",
                "Pointer",
                -1,
            ),
        )
    }

    @Test
    fun ageOverThirtyReturnsError() {
        assertEquals(
            "Age must be between 0 and 30.",
            HuntingDogValidator.validate(
                "Rex",
                "Pointer",
                31,
            ),
        )
    }

    @Test
    fun puppyAgeZeroIsValid() {
        assertNull(
            HuntingDogValidator.validate(
                "Max",
                "Weimaraner",
                0,
            ),
        )
    }

    @Test
    fun ageThirtyIsValid() {
        assertNull(
            HuntingDogValidator.validate(
                "Rex",
                "Pointer",
                30,
            ),
        )
    }

    @Test
    fun validDogWithNotesNotRequired() {
        assertNull(
            HuntingDogValidator.validate(
                "Lucky",
                "English Setter",
                5,
            ),
        )
    }
}
