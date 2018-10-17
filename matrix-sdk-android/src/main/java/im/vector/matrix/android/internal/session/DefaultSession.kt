package im.vector.matrix.android.internal.session

import android.os.Looper
import android.support.annotation.MainThread
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.data.SessionParams
import im.vector.matrix.android.internal.database.SessionRealmHolder
import im.vector.matrix.android.internal.di.SessionModule
import im.vector.matrix.android.internal.events.sync.SyncModule
import im.vector.matrix.android.internal.events.sync.job.SyncThread
import org.koin.core.scope.Scope
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import org.koin.standalone.getKoin
import org.koin.standalone.inject


class DefaultSession(private val sessionParams: SessionParams) : Session, KoinComponent {

    companion object {
        const val SCOPE: String = "session"
    }

    private lateinit var scope: Scope

    private val realmInstanceHolder by inject<SessionRealmHolder>()
    private val roomSummaryObserver by inject<RoomSummaryObserver>()
    private val syncThread by inject<SyncThread>()
    private var isOpen = false

    @MainThread
    override fun open() {
        checkIsMainThread()
        assert(!isOpen)
        isOpen = true
        val sessionModule = SessionModule(sessionParams)
        val syncModule = SyncModule()
        StandAloneContext.loadKoinModules(listOf(sessionModule, syncModule))
        scope = getKoin().getOrCreateScope(SCOPE)
        realmInstanceHolder.open()
        roomSummaryObserver.start()
        syncThread.start()
    }

    override fun realmHolder(): SessionRealmHolder {
        assert(isOpen)
        return realmInstanceHolder
    }

    override fun syncThread(): SyncThread {
        assert(isOpen)
        return syncThread
    }

    @MainThread
    override fun close() {
        checkIsMainThread()
        assert(isOpen)
        syncThread.kill()
        roomSummaryObserver.dispose()
        realmInstanceHolder.close()
        scope.close()
        isOpen = false
    }

    // Private methods *****************************************************************************

    private fun checkIsMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("Should be called on main thread")
        }
    }

}