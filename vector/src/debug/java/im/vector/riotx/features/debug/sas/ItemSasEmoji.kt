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

package im.vector.riotx.features.debug.sas

import android.annotation.SuppressLint
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.api.session.crypto.sas.EmojiRepresentation
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel

@EpoxyModelClass(layout = im.vector.riotx.R.layout.item_sas_emoji)
abstract class ItemSasEmoji : VectorEpoxyModel<ItemSasEmoji.Holder>() {

    @EpoxyAttribute
    var index: Int = 0
    @EpoxyAttribute
    lateinit var emojiRepresentation: EmojiRepresentation

    @SuppressLint("SetTextI18n")
    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.indexView.text = "$index:"
        holder.emojiView.text = emojiRepresentation.emoji
        holder.textView.setText(emojiRepresentation.nameResId)
        holder.idView.text = holder.idView.resources.getResourceEntryName(emojiRepresentation.nameResId)
    }

    class Holder : VectorEpoxyHolder() {
        val indexView by bind<TextView>(im.vector.riotx.R.id.sas_emoji_index)
        val emojiView by bind<TextView>(im.vector.riotx.R.id.sas_emoji)
        val textView by bind<TextView>(im.vector.riotx.R.id.sas_emoji_text)
        val idView by bind<TextView>(im.vector.riotx.R.id.sas_emoji_text_id)
    }
}
