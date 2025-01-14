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

package im.vector.riotx.features.home.group

import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

class GroupSummaryController @Inject constructor(private val avatarRenderer: AvatarRenderer) : EpoxyController() {

    var callback: Callback? = null
    private var viewState: GroupListViewState? = null

    init {
        requestModelBuild()
    }

    fun update(viewState: GroupListViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val nonNullViewState = viewState ?: return
        buildGroupModels(nonNullViewState.asyncGroups(), nonNullViewState.selectedGroup)
    }

    private fun buildGroupModels(summaries: List<GroupSummary>?, selected: GroupSummary?) {
        if (summaries.isNullOrEmpty()) {
            return
        }
        summaries.forEach { groupSummary ->
            val isSelected = groupSummary.groupId == selected?.groupId
            groupSummaryItem {
                avatarRenderer(avatarRenderer)
                id(groupSummary.groupId)
                groupId(groupSummary.groupId)
                groupName(groupSummary.displayName)
                selected(isSelected)
                avatarUrl(groupSummary.avatarUrl)
                listener { callback?.onGroupSelected(groupSummary) }
            }
        }
    }

    interface Callback {
        fun onGroupSelected(groupSummary: GroupSummary)
    }
}
