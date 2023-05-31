package com.android.designcompose

open class NetworkException: RuntimeException()
class ConnectionFailedException: NetworkException()
class AccessDeniedException: RuntimeException()

open class FetchException(val docID: String): RuntimeException()
class DocumentNotFoundException(docID: String) : FetchException(docID)

