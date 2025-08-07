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
