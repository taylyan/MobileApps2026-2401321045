
package com.huntersstory.app.domain.validation

object HuntingDogValidator {

    fun validate(
        name: String,
        breed: String,
        age: Int?
    ): String? {

        if (name.isBlank()) {
            return "Dog name is required."
        }

        if (breed.isBlank()) {
            return "Breed is required."
        }

        if (age == null || age !in 0..30) {
            return "Age must be between 0 and 30."
        }

        return null
    }
}
