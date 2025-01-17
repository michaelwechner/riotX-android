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

package im.vector.riotx.features.home.room.list

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.DataSource
import im.vector.riotx.core.utils.PublishDataSource
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class RoomListViewModel @Inject constructor(initialState: RoomListViewState,
                                            private val session: Session,
                                            private val roomSummariesSource: DataSource<List<RoomSummary>>)
    : VectorViewModel<RoomListViewState, RoomListAction>(initialState) {

    interface Factory {
        fun create(initialState: RoomListViewState): RoomListViewModel
    }

    companion object : MvRxViewModelFactory<RoomListViewModel, RoomListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListViewState): RoomListViewModel? {
            val fragment: RoomListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomListViewModelFactory.create(state)
        }
    }

    private val displayMode = initialState.displayMode
    private val roomListDisplayModeFilter = RoomListDisplayModeFilter(displayMode)

    private val _viewEvents = PublishDataSource<RoomListViewEvents>()
    val viewEvents: DataSource<RoomListViewEvents> = _viewEvents

    init {
        observeRoomSummaries()
    }

    override fun handle(action: RoomListAction) {
        when (action) {
            is RoomListAction.SelectRoom                  -> handleSelectRoom(action)
            is RoomListAction.ToggleCategory              -> handleToggleCategory(action)
            is RoomListAction.AcceptInvitation            -> handleAcceptInvitation(action)
            is RoomListAction.RejectInvitation            -> handleRejectInvitation(action)
            is RoomListAction.FilterWith                  -> handleFilter(action)
            is RoomListAction.MarkAllRoomsRead            -> handleMarkAllRoomsRead()
            is RoomListAction.LeaveRoom                   -> handleLeaveRoom(action)
            is RoomListAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListAction.SelectRoom) {
        _viewEvents.post(RoomListViewEvents.SelectRoom(action.roomSummary.roomId))
    }

    private fun handleToggleCategory(action: RoomListAction.ToggleCategory) = setState {
        this.toggle(action.category)
    }

    private fun handleFilter(action: RoomListAction.FilterWith) {
        setState {
            copy(
                    roomFilter = action.filter
            )
        }
    }

    private fun observeRoomSummaries() {
        roomSummariesSource
                .observe()
                .observeOn(Schedulers.computation())
                .execute { asyncRooms ->
                    copy(asyncRooms = asyncRooms)
                }

        roomSummariesSource
                .observe()
                .observeOn(Schedulers.computation())
                .map { buildRoomSummaries(it) }
                .execute { async ->
                    copy(asyncFilteredRooms = async)
                }
    }

    private fun handleAcceptInvitation(action: RoomListAction.AcceptInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId

        if (state.joiningRoomsIds.contains(roomId) || state.rejectingRoomsIds.contains(roomId)) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }

        setState {
            copy(
                    joiningRoomsIds = joiningRoomsIds + roomId,
                    rejectingErrorRoomsIds = rejectingErrorRoomsIds - roomId
            )
        }

        session.getRoom(roomId)?.join(emptyList(), object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            }

            override fun onFailure(failure: Throwable) {
                // Notify the user
                _viewEvents.post(RoomListViewEvents.Failure(failure))
                setState {
                    copy(
                            joiningRoomsIds = joiningRoomsIds - roomId,
                            joiningErrorRoomsIds = joiningErrorRoomsIds + roomId
                    )
                }
            }
        })
    }

    private fun handleRejectInvitation(action: RoomListAction.RejectInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId

        if (state.joiningRoomsIds.contains(roomId) || state.rejectingRoomsIds.contains(roomId)) {
            // Request already sent, should not happen
            Timber.w("Try to reject an already rejecting room. Should not happen")
            return@withState
        }

        setState {
            copy(
                    rejectingRoomsIds = rejectingRoomsIds + roomId,
                    joiningErrorRoomsIds = joiningErrorRoomsIds - roomId
            )
        }

        session.getRoom(roomId)?.leave(object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the rejectingRoomsIds here, because, the room is not rejected yet regarding the sync data.
                // Instead, we wait for the room to be rejected
                // Known bug: if the user is invited again (after rejecting the first invitation), the loading will be displayed instead of the buttons.
                // If we update the state, the button will be displayed again, so it's not ideal...
            }

            override fun onFailure(failure: Throwable) {
                // Notify the user
                _viewEvents.post(RoomListViewEvents.Failure(failure))
                setState {
                    copy(
                            rejectingRoomsIds = rejectingRoomsIds - roomId,
                            rejectingErrorRoomsIds = rejectingErrorRoomsIds + roomId
                    )
                }
            }
        })
    }

    private fun handleMarkAllRoomsRead() = withState { state ->
        state.asyncFilteredRooms.invoke()
                ?.flatMap { it.value }
                ?.filter { it.membership == Membership.JOIN }
                ?.map { it.roomId }
                ?.toList()
                ?.let { session.markAllAsRead(it, object : MatrixCallback<Unit> {}) }
    }

    private fun handleChangeNotificationMode(action: RoomListAction.ChangeRoomNotificationState) {
        session.getRoom(action.roomId)?.setRoomNotificationState(action.notificationState, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                _viewEvents.post(RoomListViewEvents.Failure(failure))
            }
        })
    }

    private fun handleLeaveRoom(action: RoomListAction.LeaveRoom) {
        session.getRoom(action.roomId)?.leave(object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                _viewEvents.post(RoomListViewEvents.Failure(failure))
            }
        })
    }

    private fun buildRoomSummaries(rooms: List<RoomSummary>): RoomSummaries {
        // Set up init size on directChats and groupRooms as they are the biggest ones
        val invites = ArrayList<RoomSummary>()
        val favourites = ArrayList<RoomSummary>()
        val directChats = ArrayList<RoomSummary>(rooms.size)
        val groupRooms = ArrayList<RoomSummary>(rooms.size)
        val lowPriorities = ArrayList<RoomSummary>()
        val serverNotices = ArrayList<RoomSummary>()

        rooms
                .filter { roomListDisplayModeFilter.test(it) }
                .forEach { room ->
                    val tags = room.tags.map { it.name }
                    when {
                        room.membership == Membership.INVITE          -> invites.add(room)
                        tags.contains(RoomTag.ROOM_TAG_SERVER_NOTICE) -> serverNotices.add(room)
                        tags.contains(RoomTag.ROOM_TAG_FAVOURITE)     -> favourites.add(room)
                        tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY)  -> lowPriorities.add(room)
                        room.isDirect                                 -> directChats.add(room)
                        else                                          -> groupRooms.add(room)
                    }
                }

        return RoomSummaries().apply {
            put(RoomCategory.INVITE, invites)
            put(RoomCategory.FAVOURITE, favourites)
            put(RoomCategory.DIRECT, directChats)
            put(RoomCategory.GROUP, groupRooms)
            put(RoomCategory.LOW_PRIORITY, lowPriorities)
            put(RoomCategory.SERVER_NOTICE, serverNotices)
        }
    }
}
