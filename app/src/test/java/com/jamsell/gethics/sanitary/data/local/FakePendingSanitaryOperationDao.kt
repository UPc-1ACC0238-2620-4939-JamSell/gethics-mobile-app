package com.jamsell.gethics.sanitary.data.local

/** Cola offline en memoria con la misma semantica que el DAO de Room (lo verifica PendingSanitaryOperationDaoTest). */
class FakePendingSanitaryOperationDao(initial: List<PendingSanitaryOperationEntity> = emptyList()) : PendingSanitaryOperationDao {
    val rows = initial.toMutableList()

    override suspend fun insert(operation: PendingSanitaryOperationEntity) {
        check(rows.none { it.clientRequestId == operation.clientRequestId }) { "clientRequestId repetido" } // ABORT
        rows += operation
    }

    override suspend fun getPending() = rows.filter { it.status == PendingSanitaryOperationStatus.PENDING }.sortedBy { it.createdAt }

    override suspend fun delete(clientRequestId: String) {
        rows.removeAll { it.clientRequestId == clientRequestId }
    }

    override suspend fun markFailed(clientRequestId: String, message: String) {
        rows.replaceAll {
            if (it.clientRequestId == clientRequestId) it.copy(status = PendingSanitaryOperationStatus.FAILED, lastError = message) else it
        }
    }
}
