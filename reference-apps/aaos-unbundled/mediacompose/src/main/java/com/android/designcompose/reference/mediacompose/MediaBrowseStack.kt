/*
 * Copyright 2023 Google LLC
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

package com.android.designcompose.reference.mediacompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.car.apps.common.util.FutureData
import com.android.car.media.common.MediaItemMetadata
import com.android.car.media.common.browse.MediaItemsRepository
import java.util.Stack

/// MediaBrowseStack implements a stack of browse pages. It's used for the main browse interface
/// and for the Search Result browse interface.
internal data class MediaBrowseStack(
    private val mediaRepo: MediaItemsRepository,
    private val activity: ComponentActivity,
) : ViewModel() {
    inner class Page(val parent: MediaItemMetadata?) {
        fun getItemList(): MediaItemsRepository.MediaItemsLiveData {
            return mediaRepo.getMediaChildren(parent?.id, Bundle.EMPTY)
        }

        fun getId(): String? {
            return parent?.id
        }
    }

    private fun browsePage(parent: MediaItemMetadata): Page {
        return Page(parent)
    }

    private fun searchRootPage(): Page {
        return Page(null)
    }

    private val pageStack: Stack<Page> = Stack<Page>()
    private val topPage: MutableLiveData<Page?> = MutableLiveData(null)
    private val topItemList: MutableLiveData<FutureData<List<MediaItemMetadata>>?> =
        MutableLiveData(null)

    internal fun resetTo(item: MediaItemMetadata?) {
        if (pageStack.isEmpty() && item == null) return

        if (pageStack.size == 1 && pageStack.firstElement().equals(item)) return

        pageStack.clear()
        browse(item)
    }

    internal fun resetToSearch() {
        pageStack.clear()
        topPage.value = null
        topItemList.value = null
        pageStack.push(searchRootPage())
    }

    internal fun browse(item: MediaItemMetadata?) {
        if (item != null) {
            pageStack.push(browsePage(item))
            updateView()
        }
    }

    internal fun navigateBack() {
        if (pageStack.size <= 1) return
        pageStack.pop()
        updateView()
    }

    internal fun clear() {
        pageStack.clear()
        updateView()
    }

    private fun updateView() {
        if (pageStack.isNotEmpty()) {
            topPage.value = pageStack.peek()
            watchItemList()
        } else {
            topPage.value = null
            topItemList.value = null
        }
    }

    private fun watchItemList() {
        // Watch the list of items at the top of the browse stack, and update topItemList if it
        // changes. We do this so that when the top stack page changes, we don't recompose all of
        // the old items. Previous to this change, the caller would observeAsState() on both the
        // top page of the stack and the items list of the top page. Whenever the top page changed,
        // the items list would look like it changed even though all the items initially were the
        // same. This caused all the old items to recompose once before the new list loaded.
        val page = pageStack.peek()
        if (page.parent != null)
            page.getItemList().observe(activity) {
                if (topItemList.value != it) {
                    topItemList.value = it
                }
            }
    }

    internal fun selectedRootId(): String? {
        return if (pageStack.isEmpty()) null else pageStack.firstElement().getId()
    }

    internal fun getTopPage(): LiveData<Page?> {
        return topPage
    }

    internal fun getTopItemList(): LiveData<FutureData<List<MediaItemMetadata>>?> {
        return topItemList
    }

    internal fun size(): Int {
        return pageStack.size
    }
}
