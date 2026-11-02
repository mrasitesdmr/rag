import { Navigate, Route, Routes } from 'react-router'
import { ConversationDetailPage } from '../features/conversation/pages/ConversationDetailPage.tsx'
import { ConversationListPage } from '../features/conversation/pages/ConversationListPage.tsx'
import { EvaluationPage } from '../features/evaluation/pages/EvaluationPage.tsx'
import { PreviewPage } from '../features/preview/pages/PreviewPage.tsx'
import { SourceDetailPage } from '../features/source/pages/SourceDetailPage.tsx'
import { SourceListPage } from '../features/source/pages/SourceListPage.tsx'
import { AdminLayout } from './AdminLayout.tsx'
import { NotFoundPage } from './NotFoundPage.tsx'

export function App() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<Navigate to="/onizleme" replace />} />
        <Route path="onizleme" element={<PreviewPage />} />
        <Route path="konusmalar" element={<ConversationListPage />} />
        <Route path="konusmalar/:conversationId" element={<ConversationDetailPage />} />
        <Route path="kaynaklar" element={<SourceListPage />} />
        <Route path="kaynaklar/:sourceId" element={<SourceDetailPage />} />
        <Route path="degerlendirme" element={<EvaluationPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
