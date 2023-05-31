//[common](../../../index.md)/[com.android.designcompose.common](../index.md)/[FeedbackImpl](index.md)

# FeedbackImpl

[jvm]\
abstract class [FeedbackImpl](index.md)

## Constructors

| | |
|---|---|
| [FeedbackImpl](-feedback-impl.md) | [jvm]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [addIgnoredDocument](add-ignored-document.md) | [jvm]<br>fun [addIgnoredDocument](add-ignored-document.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [diskLoadFail](disk-load-fail.md) | [jvm]<br>fun [diskLoadFail](disk-load-fail.md)(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentDecodeError](document-decode-error.md) | [jvm]<br>fun [documentDecodeError](document-decode-error.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentDecodeReadBytes](document-decode-read-bytes.md) | [jvm]<br>fun [documentDecodeReadBytes](document-decode-read-bytes.md)(size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentDecodeStart](document-decode-start.md) | [jvm]<br>fun [documentDecodeStart](document-decode-start.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentDecodeSuccess](document-decode-success.md) | [jvm]<br>fun [documentDecodeSuccess](document-decode-success.md)(version: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), lastModified: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentDecodeVersionMismatch](document-decode-version-mismatch.md) | [jvm]<br>fun [documentDecodeVersionMismatch](document-decode-version-mismatch.md)(expected: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), actual: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentSaveError](document-save-error.md) | [jvm]<br>fun [documentSaveError](document-save-error.md)(error: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentSaveSuccess](document-save-success.md) | [jvm]<br>fun [documentSaveSuccess](document-save-success.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentSaveTo](document-save-to.md) | [jvm]<br>fun [documentSaveTo](document-save-to.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentUnchanged](document-unchanged.md) | [jvm]<br>fun [documentUnchanged](document-unchanged.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [documentUpdateCode](document-update-code.md) | [jvm]<br>fun [documentUpdateCode](document-update-code.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |
| [documentUpdated](document-updated.md) | [jvm]<br>fun [documentUpdated](document-updated.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), numSubscribers: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |
| [documentUpdateError](document-update-error.md) | [jvm]<br>fun [documentUpdateError](document-update-error.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), exception: [Exception](https://docs.oracle.com/javase/8/docs/api/java/lang/Exception.html))<br>fun [documentUpdateError](document-update-error.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), errorMessage: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?)<br>fun [documentUpdateError](document-update-error.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), url: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), exception: [Exception](https://docs.oracle.com/javase/8/docs/api/java/lang/Exception.html)) |
| [documentUpdateErrorRevert](document-update-error-revert.md) | [jvm]<br>fun [documentUpdateErrorRevert](document-update-error-revert.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), exception: [Exception](https://docs.oracle.com/javase/8/docs/api/java/lang/Exception.html)) |
| [documentUpdateWarnings](document-update-warnings.md) | [jvm]<br>fun [documentUpdateWarnings](document-update-warnings.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [getMessages](get-messages.md) | [jvm]<br>fun [getMessages](get-messages.md)(): [ArrayDeque](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-deque/index.html)&lt;[FeedbackMessage](../-feedback-message/index.md)&gt; |
| [isDocumentIgnored](is-document-ignored.md) | [jvm]<br>fun [isDocumentIgnored](is-document-ignored.md)(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [logMessage](log-message.md) | [jvm]<br>abstract fun [logMessage](log-message.md)(str: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), level: [FeedbackLevel](../-feedback-level/index.md)) |
| [setLevel](set-level.md) | [jvm]<br>fun [setLevel](set-level.md)(lvl: [FeedbackLevel](../-feedback-level/index.md)) |
| [setMaxMessages](set-max-messages.md) | [jvm]<br>fun [setMaxMessages](set-max-messages.md)(num: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |
| [setStatus](set-status.md) | [jvm]<br>open fun [setStatus](set-status.md)(str: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), level: [FeedbackLevel](../-feedback-level/index.md), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [messagesListId](messages-list-id.md) | [jvm]<br>var [messagesListId](messages-list-id.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
