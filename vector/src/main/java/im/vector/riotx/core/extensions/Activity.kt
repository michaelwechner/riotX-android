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

package im.vector.riotx.core.extensions

import android.os.Parcelable
import androidx.fragment.app.Fragment
import im.vector.riotx.core.platform.VectorBaseActivity

fun VectorBaseActivity.addFragment(frameId: Int, fragment: Fragment) {
    supportFragmentManager.commitTransactionNow { add(frameId, fragment) }
}

fun <T : Fragment> VectorBaseActivity.addFragment(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    supportFragmentManager.commitTransactionNow {
        add(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun VectorBaseActivity.replaceFragment(frameId: Int, fragment: Fragment, tag: String? = null) {
    supportFragmentManager.commitTransactionNow { replace(frameId, fragment, tag) }
}

fun <T : Fragment> VectorBaseActivity.replaceFragment(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    supportFragmentManager.commitTransactionNow {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun VectorBaseActivity.addFragmentToBackstack(frameId: Int, fragment: Fragment, tag: String? = null) {
    supportFragmentManager.commitTransaction { replace(frameId, fragment).addToBackStack(tag) }
}

fun <T : Fragment> VectorBaseActivity.addFragmentToBackstack(frameId: Int, fragmentClass: Class<T>, params: Parcelable? = null, tag: String? = null) {
    supportFragmentManager.commitTransaction {
        replace(frameId, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

fun VectorBaseActivity.hideKeyboard() {
    currentFocus?.hideKeyboard()
}
