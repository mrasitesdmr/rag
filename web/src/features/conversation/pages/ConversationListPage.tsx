import { useState } from 'react'
import { errorCodeOf } from '../../../shared/api/ApiError.ts'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import { Pagination } from '../../../shared/components/Pagination.tsx'
import { useConversationsQuery } from '../api/conversationQueries.ts'
import { ConversationEmptyState } from '../components/ConversationEmptyState.tsx'
import { ConversationTable } from '../components/ConversationTable.tsx'
import { CONVERSATION_PAGE_SIZE } from '../model/conversationRules.ts'
import styles from './ConversationPages.module.css'

export function ConversationListPage() {
  const [page, setPage] = useState(0)
  const conversationsQuery = useConversationsQuery({ page, size: CONVERSATION_PAGE_SIZE })
  const conversationPage = conversationsQuery.data

  return (
    <>
      <PageHeader
        section="Konuşmalar"
        title="Konuşmalar"
        description="Her tur, modele giden parçalarla birlikte saklanır. Yanlış bir cevapta önce doğru parçanın prompt'a girip girmediğine bakılır."
      />
      {conversationPage === undefined ? (
        conversationsQuery.isError ? (
          <ErrorAlert errorCode={errorCodeOf(conversationsQuery.error)} />
        ) : (
          <p className={styles.status}>Yükleniyor…</p>
        )
      ) : conversationPage.totalElements === 0 ? (
        <ConversationEmptyState />
      ) : (
        <>
          <ConversationTable conversations={conversationPage.content} />
          {conversationPage.totalPages > 1 ? (
            <Pagination
              label="Konuşma sayfaları"
              page={page}
              totalPages={conversationPage.totalPages}
              onPageChange={setPage}
            />
          ) : null}
        </>
      )}
    </>
  )
}
