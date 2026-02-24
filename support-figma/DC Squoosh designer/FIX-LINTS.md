# Gemini Lint Fixing Process

This document outlines the step-by-step process for fixing lint errors in this project using the Gemini CLI.

## Process

1.  **Start with a clean state:** Ensure that the project is in a clean state by running `git status`. If there are local pending changes to existing, tracked files, deny fixing. Recommend the user to commit, stash or restore the changes. Untracked files can be ignored.
2.  **Get the list of errors:** Run the linter to get the current list of errors using `npm run lint`.
3.  **Fix one error at a time:** Address the topmost error in the lint output.
4.  **Build the project:** After applying a fix, build the project to ensure that the change has not introduced any build errors.
5.  **Verify the fix:** Run the linter again to confirm that the error has been resolved and that no new errors have been introduced.
6.  **Handle regressions:** If the change has introduced a new error (a regression), revert the change using `git restore .` and rebuild to confirm that the regression is gone. Then, proceed to the next lint error.
7.  **Wait for confirmation (CRITICAL):** After a successful build and lint check, **STOP** and wait for the user to confirm that they have tested the changes and are ready to proceed. The user **MUST** explicitly say "continue" before you proceed to the next step. **DO NOT** stage the changes or continue without this confirmation.
8.  **Stage the changes:** Once the user has confirmed, stage the changes using `git add -u`.
9.  **Repeat:** Repeat the process until all lint errors have been resolved.
