#!/bin/python
# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


from pathlib import Path
import argparse
import subprocess
import shutil
import tempfile

"""
This script takes a prepared sample standalone project and creates a ready-to-build
exported version of it. Any symlinked files will be dereferenced and copied, and
local / build / generated files will be filtered out.

See the README for more
"""
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('project_dir', type=Path)
    parser.add_argument('-o', '--out', type=Path, default=Path("build"))
    args = parser.parse_args()

    out_dir = args.out

    copy_dir = tempfile.TemporaryDirectory()
    initial_copy = Path(copy_dir.name) / "initial"
    final_copy = Path(copy_dir.name) / args.project_dir.name

    ignore_patterns = shutil.ignore_patterns(
        '.*', 'build', 'local.properties', 'bin'
    )

    shutil.copytree(
        src=args.project_dir,
        dst=initial_copy,
        symlinks=False,
        ignore=ignore_patterns)

    # TODO: Use Gradle version catalog format to trim down the catalog

    shutil.copytree(
        src=initial_copy,
        dst=out_dir,
        symlinks=False,
        ignore=ignore_patterns)
