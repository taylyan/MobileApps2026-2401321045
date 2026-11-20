
package com.huntersstory.app.data.repository

import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.entity.Equipment
import com.huntersstory.app.data.local.entity.HuntingDog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class CollectionType {
    EQUIPMENT,
    DOGS
}

data class CollectionItem(
    val id: Long = 0,
    val name: String,
    val secondary: String,
    val notes: String = "",
    val ageYears: Int? = null
)

class CollectionRepository(
    private val db: HunterDatabase
) {

    fun observe(
        type: CollectionType,
        uid: String
    ): Flow<List<CollectionItem>> {

        return when (type) {

            CollectionType.EQUIPMENT -> {
                db.equipmentDao().getByUser(uid).map { items ->
                    items.map { item ->
                        CollectionItem(
                            id = item.id,
                            name = item.name,
                            secondary = item.category,
                            notes = item.notes
                        )
                    }
                }
            }

            CollectionType.DOGS -> {
                db.huntingDogDao().getByUser(uid).map { dogs ->
                    dogs.map { dog ->
                        CollectionItem(
                            id = dog.id,
                            name = dog.name,
                            secondary = dog.breed,
                            notes = dog.notes,
                            ageYears = dog.ageYears
                        )
                    }
                }
            }
        }
    }

    suspend fun save(
        type: CollectionType,
        uid: String,
        item: CollectionItem
    ) {
        when (type) {

            CollectionType.EQUIPMENT -> {
                if (item.id == 0L) {
                    db.equipmentDao().insert(
                        Equipment(
                            userId = uid,
                            name = item.name,
                            category = item.secondary,
                            notes = item.notes
                        )
                    )
                } else {
                    val changed = db.equipmentDao().update(
                        item.id,
                        uid,
                        item.name,
                        item.secondary,
                        item.notes
                    )
                    check(changed > 0) { "Equipment not found." }
                }
            }

            CollectionType.DOGS -> {
                val age = requireNotNull(item.ageYears)

                if (item.id == 0L) {
                    db.huntingDogDao().insert(
                        HuntingDog(
                            userId = uid,
                            name = item.name,
                            breed = item.secondary,
                            ageYears = age,
                            notes = item.notes
                        )
                    )
                } else {
                    val changed = db.huntingDogDao().update(
                        item.id,
                        uid,
                        item.name,
                        item.secondary,
                        age,
                        item.notes
                    )
                    check(changed > 0) { "Dog not found." }
                }
            }
        }
    }

    suspend fun delete(
        type: CollectionType,
        uid: String,
        id: Long
    ) {
        val deleted = when (type) {
            CollectionType.EQUIPMENT ->
                db.equipmentDao().delete(id, uid)
            CollectionType.DOGS ->
                db.huntingDogDao().delete(id, uid)
        }

        check(deleted > 0) { "Record not found." }
    }
}
