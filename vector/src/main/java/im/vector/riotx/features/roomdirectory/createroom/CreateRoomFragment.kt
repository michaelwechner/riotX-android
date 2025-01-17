/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.roomdirectory.createroom

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.roomdirectory.RoomDirectorySharedAction
import im.vector.riotx.features.roomdirectory.RoomDirectorySharedActionViewModel
import kotlinx.android.synthetic.main.fragment_create_room.*
import timber.log.Timber
import javax.inject.Inject

class CreateRoomFragment @Inject constructor(private val createRoomController: CreateRoomController) : VectorBaseFragment(), CreateRoomController.Listener {

    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel
    private val viewModel: CreateRoomViewModel by activityViewModel()

    override fun getLayoutResId() = R.layout.fragment_create_room

    override fun getMenuRes() = R.menu.vector_room_creation

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vectorBaseActivity.setSupportActionBar(createRoomToolbar)
        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()
        createRoomClose.setOnClickListener {
            sharedActionViewModel.post(RoomDirectorySharedAction.Back)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_room -> {
                viewModel.handle(CreateRoomAction.Create)
                true
            }
            else                    ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)

        createRoomForm.layoutManager = layoutManager
        createRoomController.listener = this

        createRoomForm.setController(createRoomController)
    }

    override fun onNameChange(newName: String) {
        viewModel.handle(CreateRoomAction.SetName(newName))
    }

    override fun setIsPublic(isPublic: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsPublic(isPublic))
    }

    override fun setIsInRoomDirectory(isInRoomDirectory: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsInRoomDirectory(isInRoomDirectory))
    }

    override fun retry() {
        Timber.v("Retry")
        viewModel.handle(CreateRoomAction.Create)
    }

    override fun invalidate() = withState(viewModel) { state ->
        val async = state.asyncCreateRoomRequest
        if (async is Success) {
            // Navigate to freshly created room
            navigator.openRoom(requireActivity(), async())

            sharedActionViewModel.post(RoomDirectorySharedAction.Close)
        } else {
            // Populate list with Epoxy
            createRoomController.setData(state)
        }
    }
}
