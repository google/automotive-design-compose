#!/bin/bash
set -e
shopt -s globstar

# This is a helper script to be called by androidx.sh
# This script locates, parses, and merges build profiling information from various report files
# A history of the results of running this script can be visualized at go/androidx-build-times

GIT_ROOT=$(git rev-parse --show-toplevel)

cd "$GIT_ROOT" || exit

PROFILE_FILES="**/build/reports/profile/*.html"
for PROFILE in $PROFILE_FILES; do
  # parse the profile file and generate a .json file summarizing it
  PROFILE_JSON="${PROFILE%.html}.json"
  # Because we run Gradle twice (see TaskUpToDateValidator.kt), we want the second-to-last profile
  ./dev-scripts/parse-profile-html.py --input-profile "$PROFILE" --output-summary "$PROFILE_JSON"
done
