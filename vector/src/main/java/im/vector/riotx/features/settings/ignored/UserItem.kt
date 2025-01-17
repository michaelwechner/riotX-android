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
package im.vector.riotx.features.settings.ignored

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.features.home.AvatarRenderer

/**
 * A list item for User.
 */
@EpoxyModelClass(layout = R.layout.item_user)
abstract class UserItem : VectorEpoxyModel<UserItem.Holder>() {

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    lateinit var user: User

    @EpoxyAttribute
    var itemClickAction: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        holder.root.setOnClickListener { itemClickAction?.invoke() }

        avatarRenderer.render(user, holder.avatarImage)
        holder.userIdText.setTextOrHide(user.userId)
        holder.displayNameText.setTextOrHide(user.displayName)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<View>(R.id.itemUserRoot)
        val avatarImage by bind<ImageView>(R.id.itemUserAvatar)
        val userIdText by bind<TextView>(R.id.itemUserId)
        val displayNameText by bind<TextView>(R.id.itemUserName)
    }
}
