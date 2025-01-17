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

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.*
import com.google.android.material.snackbar.Snackbar
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.platform.OnBackPressed
import im.vector.riotx.core.platform.StateView
import im.vector.riotx.core.platform.VectorBaseFragment

import im.vector.riotx.features.home.RoomListDisplayMode
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.riotx.features.home.room.list.widget.FabMenuView
import im.vector.riotx.features.notifications.NotificationDrawerManager
import im.vector.riotx.features.share.SharedData
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_list.*
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode,
        val sharedData: SharedData? = null
) : Parcelable

class RoomListFragment @Inject constructor(
        private val roomController: RoomSummaryController,
        val roomListViewModelFactory: RoomListViewModel.Factory,
        private val errorFormatter: ErrorFormatter,
        private val notificationDrawerManager: NotificationDrawerManager

) : VectorBaseFragment(), RoomSummaryController.Listener, OnBackPressed, FabMenuView.Listener {

    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private val roomListParams: RoomListParams by args()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_list

    private var hasUnreadRooms = false

    override fun getMenuRes() = R.menu.room_list

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_home_mark_all_as_read -> {
                roomListViewModel.handle(RoomListAction.MarkAllRoomsRead)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_home_mark_all_as_read).isVisible = hasUnreadRooms
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCreateRoomButton()
        setupRecyclerView()
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)

        roomListViewModel.subscribe { renderState(it) }
        roomListViewModel.viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is RoomListViewEvents.SelectRoom -> openSelectedRoom(it)
                        is RoomListViewEvents.Failure    -> showError(it)
                    }
                }
                .disposeOnDestroyView()

        createChatFabMenu.listener = this

        sharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        roomListView.adapter = null
    }

    private fun openSelectedRoom(event: RoomListViewEvents.SelectRoom) {
        if (roomListParams.displayMode == RoomListDisplayMode.SHARE) {
            val sharedData = roomListParams.sharedData ?: return
            navigator.openRoomForSharing(requireActivity(), event.roomId, sharedData)
        } else {
            navigator.openRoom(requireActivity(), event.roomId)
        }
    }

    private fun showError(event: RoomListViewEvents.Failure) {
        vectorBaseActivity.coordinatorLayout?.let {
            Snackbar.make(it, errorFormatter.toHumanReadable(event.throwable), Snackbar.LENGTH_SHORT)
                    .show()
        }
    }

    private fun setupCreateRoomButton() {
        when (roomListParams.displayMode) {
            RoomListDisplayMode.HOME   -> createChatFabMenu.isVisible = true
            RoomListDisplayMode.PEOPLE -> createChatRoomButton.isVisible = true
            RoomListDisplayMode.ROOMS  -> createGroupRoomButton.isVisible = true
            else                       -> Unit // No button in this mode
        }

        createChatRoomButton.setOnClickListener {
            createDirectChat()
        }
        createGroupRoomButton.setOnClickListener {
            openRoomDirectory()
        }

        // Hide FAB when list is scrolling
        roomListView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        createChatFabMenu.removeCallbacks(showFabRunnable)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE     -> {
                                createChatFabMenu.postDelayed(showFabRunnable, 250)
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                when (roomListParams.displayMode) {
                                    RoomListDisplayMode.HOME   -> createChatFabMenu.hide()
                                    RoomListDisplayMode.PEOPLE -> createChatRoomButton.hide()
                                    RoomListDisplayMode.ROOMS  -> createGroupRoomButton.hide()
                                    else                       -> Unit
                                }
                            }
                        }
                    }
                })
    }

    fun filterRoomsWith(filter: String) {
        // Scroll the list to top
        roomListView.scrollToPosition(0)

        roomListViewModel.handle(RoomListAction.FilterWith(filter))
    }

    override fun openRoomDirectory(initialFilter: String) {
        navigator.openRoomDirectory(requireActivity(), initialFilter)
    }

    override fun createDirectChat() {
        navigator.openCreateDirectRoom(requireActivity())
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        roomListView.layoutManager = layoutManager
        roomListView.itemAnimator = RoomListAnimator()
        roomController.listener = this
        roomController.addModelBuildListener { it.dispatchTo(stateRestorer) }
        roomListView.adapter = roomController.adapter
        stateView.contentView = roomListView
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            when (roomListParams.displayMode) {
                RoomListDisplayMode.HOME   -> createChatFabMenu.show()
                RoomListDisplayMode.PEOPLE -> createChatRoomButton.show()
                RoomListDisplayMode.ROOMS  -> createGroupRoomButton.show()
                else                       -> Unit
            }
        }
    }

    private fun handleQuickActions(quickAction: RoomListQuickActionsSharedAction) {
        when (quickAction) {
            is RoomListQuickActionsSharedAction.NotificationsAllNoisy     -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES_NOISY))
            }
            is RoomListQuickActionsSharedAction.NotificationsAll          -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES))
            }
            is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MENTIONS_ONLY))
            }
            is RoomListQuickActionsSharedAction.NotificationsMute         -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MUTE))
            }
            is RoomListQuickActionsSharedAction.Settings                  -> {
                vectorBaseActivity.notImplemented("Opening room settings")
            }
            is RoomListQuickActionsSharedAction.Leave                     -> {
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.room_participants_leave_prompt_title)
                        .setMessage(R.string.room_participants_leave_prompt_msg)
                        .setPositiveButton(R.string.leave) { _, _ ->
                            roomListViewModel.handle(RoomListAction.LeaveRoom(quickAction.roomId))
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }
        }
    }

    private fun renderState(state: RoomListViewState) {
        when (state.asyncFilteredRooms) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
            is Fail       -> renderFailure(state.asyncFilteredRooms.error)
        }
        roomController.update(state)

        // Mark all as read menu
        when (roomListParams.displayMode) {
            RoomListDisplayMode.HOME,
            RoomListDisplayMode.PEOPLE,
            RoomListDisplayMode.ROOMS -> {
                val newValue = state.hasUnread
                if (hasUnreadRooms != newValue) {
                    hasUnreadRooms = newValue
                    requireActivity().invalidateOptionsMenu()
                }
            }
            else                      -> Unit
        }
    }

    private fun renderSuccess(state: RoomListViewState) {
        val allRooms = state.asyncRooms()
        val filteredRooms = state.asyncFilteredRooms()
        if (filteredRooms.isNullOrEmpty()) {
            renderEmptyState(allRooms)
        } else {
            stateView.state = StateView.State.Content
        }
    }

    private fun renderEmptyState(allRooms: List<RoomSummary>?) {
        val hasNoRoom = allRooms
                ?.filter {
                    it.membership == Membership.JOIN || it.membership == Membership.INVITE
                }
                .isNullOrEmpty()
        val emptyState = when (roomListParams.displayMode) {
            RoomListDisplayMode.HOME   -> {
                if (hasNoRoom) {
                    StateView.State.Empty(
                            getString(R.string.room_list_catchup_welcome_title),
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_catchup),
                            getString(R.string.room_list_catchup_welcome_body)
                    )
                } else {
                    StateView.State.Empty(
                            getString(R.string.room_list_catchup_empty_title),
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_noun_party_popper),
                            getString(R.string.room_list_catchup_empty_body))
                }
            }
            RoomListDisplayMode.PEOPLE ->
                StateView.State.Empty(
                        getString(R.string.room_list_people_empty_title),
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_chat),
                        getString(R.string.room_list_people_empty_body)
                )
            RoomListDisplayMode.ROOMS  ->
                StateView.State.Empty(
                        getString(R.string.room_list_rooms_empty_title),
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_group),
                        getString(R.string.room_list_rooms_empty_body)
                )
            else                       ->
                // Always display the content in this mode, because if the footer
                StateView.State.Content
        }
        stateView.state = emptyState
    }

    private fun renderLoading() {
        stateView.state = StateView.State.Loading
    }

    private fun renderFailure(error: Throwable) {
        val message = when (error) {
            is Failure.NetworkConnection -> getString(R.string.network_error_please_check_and_retry)
            else                         -> getString(R.string.unknown_error)
        }
        stateView.state = StateView.State.Error(message)
    }

    override fun onBackPressed(): Boolean {
        if (createChatFabMenu.onBackPressed()) {
            return true
        }

        return false
    }

    // RoomSummaryController.Callback **************************************************************

    override fun onRoomClicked(room: RoomSummary) {
        roomListViewModel.handle(RoomListAction.SelectRoom(room))
    }

    override fun onRoomLongClicked(room: RoomSummary): Boolean {
        roomController.onRoomLongClicked()

        RoomListQuickActionsBottomSheet
                .newInstance(room.roomId)
                .show(childFragmentManager, "ROOM_LIST_QUICK_ACTIONS")
        return true
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(room.roomId)
        roomListViewModel.handle(RoomListAction.AcceptInvitation(room))
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(room.roomId)
        roomListViewModel.handle(RoomListAction.RejectInvitation(room))
    }

    override fun onToggleRoomCategory(roomCategory: RoomCategory) {
        roomListViewModel.handle(RoomListAction.ToggleCategory(roomCategory))
    }

    override fun createRoom(initialName: String) {
        navigator.openCreateRoom(requireActivity(), initialName)
    }
}
