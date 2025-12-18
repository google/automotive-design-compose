// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::sync::Arc;

use image::{DynamicImage, RgbaImage};
use usvg::{NodeExt, Rect};

use crate::Error;

/* This file contains code for extracting SVG trees, as well as doing custom parsing
*  In the futurem, any SVG related methods should go here.
*/

/// Calculate the bounding box of a node in an SVG tree, including child transforms
/// and filter bounds. This bounding box should represent the entire area that will
/// have pixels set when rendering.
fn calc_node_bbox(
    tree: &usvg::Tree,
    node: &usvg::Node,
    ts: usvg::Transform,
) -> Option<usvg::PathBbox> {
    match *node.borrow() {
        usvg::NodeKind::Path(ref path) => path.data.bbox_with_transform(ts, path.stroke.as_ref()),
        usvg::NodeKind::Image(ref img) => {
            let path = usvg::PathData::from_rect(img.view_box.rect);
            path.bbox_with_transform(ts, None)
        }
        usvg::NodeKind::Svg(_) => {
            let mut bbox = usvg::PathBbox::new_bbox();

            for child in node.children() {
                let mut child_tx = ts.clone();
                child_tx.append(&child.transform());

                if let Some(c_bbox) = calc_node_bbox(tree, &child, child_tx) {
                    bbox = bbox.expand(c_bbox);
                }
            }

            Some(bbox)
        }
        usvg::NodeKind::Group(usvg::Group { ref filter, .. }) => {
            let mut bbox = usvg::PathBbox::new_bbox();

            for child in node.children() {
                let mut child_tx = ts.clone();
                child_tx.append(&child.transform());

                if let Some(c_bbox) = calc_node_bbox(tree, &child, child_tx) {
                    bbox = bbox.expand(c_bbox);
                }
            }

            // Offset/outset the bounds based on the filters.
            for f in filter {
                if let Some(def) = tree.defs_by_id(f.as_str()) {
                    match *def.borrow() {
                        usvg::NodeKind::Filter(ref x) => {
                            bbox = bbox.expand(x.rect.to_path_bbox());
                        }
                        _ => (),
                    }
                }
            }

            Some(bbox)
        }
        _ => None,
    }
}

fn rasterize(tree: &usvg::Tree, bbox: &Rect, sf: f32) -> Result<(DynamicImage, Vec<u8>), Error> {
    let mut img = tiny_skia::Pixmap::new(
        (bbox.width() as f32 * sf) as u32,
        (bbox.height() as f32 * sf) as u32,
    )
    .ok_or(usvg::Error::InvalidSize)?;

    resvg::render(
        &tree,
        usvg::FitTo::Original,
        tiny_skia::Transform::from_translate(-bbox.x() as f32, -bbox.y() as f32).post_scale(sf, sf),
        img.as_mut(),
    );

    // We should import `png` directly so we can map errors appropriately.
    let encoded_bytes = img.encode_png().ok().ok_or(usvg::Error::InvalidSize)?;
    let raw_img = RgbaImage::from_raw(img.width(), img.height(), img.data().to_vec())
        .ok_or(usvg::Error::InvalidSize)?;

    Ok((DynamicImage::ImageRgba8(raw_img), encoded_bytes))
}

pub struct RasterizedVector {
    pub content_box: Rect,
    // We want to make serialized docs that work on multiple different kinds of device,
    // so we rasterize vectors to several different scale factors.
    pub encoded_bytes_1x: Arc<serde_bytes::ByteBuf>,
    pub encoded_bytes_2x: Arc<serde_bytes::ByteBuf>,
    pub encoded_bytes_3x: Arc<serde_bytes::ByteBuf>,
    pub width: u32,
    pub height: u32,
}

/// Render an SVG without applying the root-level "viewBox" clip (but honoring clips
/// set further down in the tree). This function parses and walks the SVG document to
/// find the largest paint rect, and then rasterizes the entire document. The rasterized
/// image is returned as an encoded PNG and as a `ToolkitImage` instance. A `content_box`
/// which contains a translation from the clipped SVG to the unclipped image is also
/// returned.
pub fn render_svg_without_clip(svg_content: &str) -> Result<RasterizedVector, Error> {
    let tree = usvg::Tree::from_str(svg_content, &usvg::Options { ..Default::default() }.to_ref())?;

    let svg_node = tree.svg_node();

    // Take the bounding box of the vector content (including filter bounds) and union it with
    // the reported bounding box from Figma.
    let bbox = {
        // Compute the union'd bounding box.
        let bbox = calc_node_bbox(&tree, &tree.root(), usvg::Transform::default())
            .ok_or(usvg::Error::InvalidSize)?
            .expand(
                usvg::PathBbox::new(0.0, 0.0, svg_node.size.width(), svg_node.size.height())
                    .ok_or(usvg::Error::InvalidSize)?,
            );

        // Various layout implementations that we work with only operate on integral quantities
        // (even though we use floats). So let's round out the bounding box to the smallest integer
        // box that fully covers the computed box, and avoid rounding errors at layout time.
        let x = bbox.x().floor();
        let y = bbox.y().floor();
        let w = bbox.right().ceil() - x;
        let h = bbox.bottom().ceil() - y;
        Rect::new(x, y, w, h).ok_or(usvg::Error::InvalidSize)?
    };

    let (img, encoded_bytes_1x) = rasterize(&tree, &bbox, 1.0)?;
    let (_, encoded_bytes_2x) = rasterize(&tree, &bbox, 2.0)?;
    let (_, encoded_bytes_3x) = rasterize(&tree, &bbox, 3.0)?;

    Ok(RasterizedVector {
        content_box: bbox,
        encoded_bytes_1x: Arc::new(serde_bytes::ByteBuf::from(encoded_bytes_1x)),
        encoded_bytes_2x: Arc::new(serde_bytes::ByteBuf::from(encoded_bytes_2x)),
        encoded_bytes_3x: Arc::new(serde_bytes::ByteBuf::from(encoded_bytes_3x)),
        width: img.width(),
        height: img.height(),
    })
}
