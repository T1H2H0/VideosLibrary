package com.t1h2h0.videos.data

/**
 * Use to represent the result of business operation. Either in [UseCaseResult.Success] or
 * [UseCaseResult.Failure].
 *
 * Note:
 *      Although we already have similar class in the codebase. Such as AmplifyResponse,
 *      GoogleBillingResponse or ApiResponse. The purpose of each class are different.
 *      AmplifyResponse and GoogleBillingResponse are used solely for the sake of the library's
 *      logic. We should not mess it around with TELASA's business logic. We should treat these
 *      two type of response as part of library.
 *
 *      On the other hand, ApiResponse is primary used to represent the result of api request.
 *      Often the time, our asynchronous business logic operation is not only about api request.
 *      Therefore, there will be some limit when using ApiResponse to represent the type of
 */
sealed class UseCaseResult<out SuccessData, out FailureError> {

    data class Success<out Data>(val data: Data) : UseCaseResult<Data, Nothing>()

    data class Failure<out Error>(val error: Error) : UseCaseResult<Nothing, Error>()
}
