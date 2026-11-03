package in.zerotax.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LazyLoadStateTest {

    @Test
    fun testLazyLoadState_UninitializedState() {
        val state: LazyLoadState<String> = LazyLoadState.Uninitialized
        assertFalse(state.isLoaded)
        assertFalse(state.isLoading)
        assertNull(state.getOrNull())
    }

    @Test
    fun testLazyLoadState_LoadingState() {
        val state: LazyLoadState<String> = LazyLoadState.Loading
        assertFalse(state.isLoaded)
        assertTrue(state.isLoading)
        assertNull(state.getOrNull())
    }

    @Test
    fun testLazyLoadState_SuccessWithPagination() {
        val items = listOf("Record 1", "Record 2")
        val state: LazyLoadState<List<String>> = LazyLoadState.Success(
            data = items,
            hasMorePages = true,
            currentPage = 1,
            totalCount = 10
        )

        assertTrue(state.isLoaded)
        assertFalse(state.isLoading)
        assertEquals(items, state.getOrNull())

        val successState = state as LazyLoadState.Success
        assertTrue(successState.hasMorePages)
        assertEquals(1, successState.currentPage)
        assertEquals(10, successState.totalCount)
    }

    @Test
    fun testLazyLoadState_FailureState() {
        val errorState: LazyLoadState<String> = LazyLoadState.Failure(
            errorMessage = "Network timeout fetching AIS ledger"
        )

        assertFalse(errorState.isLoaded)
        assertFalse(errorState.isLoading)
        assertNull(errorState.getOrNull())
        assertEquals("Network timeout fetching AIS ledger", (errorState as LazyLoadState.Failure).errorMessage)
    }
}
