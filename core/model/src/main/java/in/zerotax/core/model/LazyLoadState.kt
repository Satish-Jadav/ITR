package in.zerotax.core.model

/**
 * Lightweight, generic Lazy-Loading state container for deferred on-demand data retrieval.
 * Prevents memory spikes and CPU contention on app launch by deferring heavy database queries,
 * AIS transaction sweeps, and historical audit ledger compilations until explicitly requested.
 */
sealed class LazyLoadState<out T> {

    /** Data has not yet been requested or allocated in memory. */
    object Uninitialized : LazyLoadState<Nothing>()

    /** Asynchronous background fetching/computation in progress. */
    object Loading : LazyLoadState<Nothing>()

    /** Data has been successfully loaded into memory, with optional pagination metadata. */
    data class Success<T>(
        val data: T,
        val hasMorePages: Boolean = false,
        val currentPage: Int = 1,
        val totalCount: Int = 0
    ) : LazyLoadState<T>()

    /** Fetching or calculation failed; retains error reason. */
    data class Failure(
        val errorMessage: String,
        val throwable: Throwable? = null
    ) : LazyLoadState<Nothing>()

    val isLoaded: Boolean get() = this is Success
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}
