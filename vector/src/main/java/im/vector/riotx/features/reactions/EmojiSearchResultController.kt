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
package im.vector.riotx.features.reactions

import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.genericFooterItem
import javax.inject.Inject

class EmojiSearchResultController @Inject constructor(val stringProvider: StringProvider,
                                                      private val fontProvider: EmojiCompatFontProvider)
    : TypedEpoxyController<EmojiSearchResultViewState>() {

    var emojiTypeface: Typeface? = fontProvider.typeface

    private val fontProviderListener = object : EmojiCompatFontProvider.FontProviderListener {
        override fun compatibilityFontUpdate(typeface: Typeface?) {
            emojiTypeface = typeface
        }
    }

    init {
        fontProvider.addListener(fontProviderListener)
    }

    var listener: ReactionClickListener? = null

    override fun buildModels(data: EmojiSearchResultViewState?) {
        val results = data?.results ?: return

        if (results.isEmpty()) {
            if (data.query.isEmpty()) {
                // display 'Type something to find'
                genericFooterItem {
                    id("type.query.item")
                    text(stringProvider.getString(R.string.reaction_search_type_hint))
                }
            } else {
                // Display no search Results
                genericFooterItem {
                    id("no.results.item")
                    text(stringProvider.getString(R.string.no_result_placeholder))
                }
            }
        } else {
            // Build the search results
            results.forEach {
                emojiSearchResultItem {
                    id(it.name)
                    emojiItem(it)
                    emojiTypeFace(emojiTypeface)
                    currentQuery(data.query)
                    onClickListener(listener)
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        fontProvider.removeListener(fontProviderListener)
    }
}
