
package com.huntersstory.app.domain.validation

object HuntingDogValidator {
    private const val MIN_DOG_AGE = 0
    private const val MAX_DOG_AGE = 30

    fun validate(
        name: String,
        breed: String,
        age: Int?,
    ): String? =
        when {
            name.isBlank() -> "Dog name is required."

            breed.isBlank() -> "Breed is required."

            age == null ||
                age !in MIN_DOG_AGE..MAX_DOG_AGE ->
                "Age must be between 0 and 30."

            else -> null
        }
}
