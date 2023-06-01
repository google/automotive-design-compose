package com.android.designcompose

open class FetchException():RuntimeException()
class AccessDeniedException: FetchException()
class FigmaFileNotFoundException() : FetchException()

class RateLimitedException(): FetchException()
class InternalFigmaErrorException(): FetchException()