# Copyright 2026 Google LLC
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

import xml.etree.ElementTree as ET

def parse_jacoco_report(xml_file):
    tree = ET.parse(xml_file)
    root = tree.getroot()

    for package in root.findall('package'):
        package_name = package.get('name')
        if not package_name.startswith('com/android/designcompose'):
            continue

        for class_elem in package.findall('class'):
            class_name = class_elem.get('name')
            source_filename = class_elem.get('sourcefilename')

            line_counter = class_elem.find('counter[@type="LINE"]')
            if line_counter is not None:
                missed_lines = int(line_counter.get('missed'))
                covered_lines = int(line_counter.get('covered'))
                total_lines = missed_lines + covered_lines
                if total_lines > 0:
                    coverage = (covered_lines / total_lines) * 100
                    if coverage < 50:
                        print(f'{package_name}/{source_filename}: {coverage:.2f}%')

if __name__ == '__main__':
    parse_jacoco_report('build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml')
