/*
 * Copyright 2025 Google LLC
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

package com.android.designcompose.utils

import com.android.designcompose.definition.element.Background
import com.android.designcompose.definition.view.NodeStyle
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.copy
import com.android.designcompose.definition.view.textColorOrNull

//
// This file contains functions to maintain backward compatibility when protobufs change.
//

// Merge deprecated protobuf values so that we can load old DCF files
internal fun protoVersionsMergeStyles(override: ViewStyle, mergedNodeStyle: NodeStyle): NodeStyle {
    return mergedNodeStyle.copy {
        // Check text color for backward compatibility
        if (override.nodeStyle.hasTextColor() && !override.nodeStyle.textColor.hasNone())
            fontColor = override.nodeStyle.textColor
    }
}

// Return the font color from the newer fontColor proto field if it exists, or from the deprecated
// textColor field if it does not.
internal fun protoVersionsFontColor(style: ViewStyle): Background? {
    return if (style.nodeStyle.hasFontColor()) style.nodeStyle.fontColor
    else style.nodeStyle.textColorOrNull
}
