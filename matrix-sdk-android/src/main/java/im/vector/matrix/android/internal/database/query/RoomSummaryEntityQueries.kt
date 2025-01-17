/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.kotlin.where

internal fun RoomSummaryEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<RoomSummaryEntity> {
    val query = realm.where<RoomSummaryEntity>()
    if (roomId != null) {
        query.equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
    }
    return query
}

internal fun RoomSummaryEntity.Companion.getOrCreate(realm: Realm, roomId: String): RoomSummaryEntity {
    return where(realm, roomId).findFirst()
           ?: realm.createObject(RoomSummaryEntity::class.java, roomId)
}

internal fun RoomSummaryEntity.Companion.getDirectRooms(realm: Realm): RealmResults<RoomSummaryEntity> {
    return RoomSummaryEntity.where(realm)
            .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
            .findAll()
}

internal fun RoomSummaryEntity.Companion.isDirect(realm: Realm, roomId: String): Boolean {
    return RoomSummaryEntity.where(realm)
            .equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
            .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
            .findAll()
            .isNotEmpty()
}
