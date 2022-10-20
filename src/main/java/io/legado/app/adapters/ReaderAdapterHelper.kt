package io.legado.app.adapters

object ReaderAdapterHelper {
    var readerAdapter: ReaderAdapterInterface = DefaultAdpater()

    public fun setAdapter(adapter: ReaderAdapterInterface) {
        readerAdapter = adapter
    }

    public fun getAdapter(): ReaderAdapterInterface {
        return readerAdapter
    }
}