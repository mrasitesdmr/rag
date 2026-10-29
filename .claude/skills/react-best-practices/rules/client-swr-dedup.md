---
title: Otomatik Deduplication için Server-State Kütüphanesi Kullan
impact: MEDIUM-HIGH
impactDescription: otomatik deduplication
tags: client, tanstack-query, swr, deduplication, data-fetching
---

## Otomatik Deduplication için Server-State Kütüphanesi Kullan

Server-state kütüphaneleri (TanStack Query, SWR) component instance'ları arasında request deduplication, caching ve revalidation sağlar. Bu projede seçim **TanStack Query**'dir (`frontend-conventions` bölüm 1); SWR eklenmez. Orijinal Vercel kuralı SWR örneği verir; aşağıda aynı fikir TanStack Query ile gösterilmiştir.

**Yanlış (deduplication yok, her instance fetch yapar):**

```tsx
function SourceStatusBadge({ sourceId }: { sourceId: number }) {
  const [status, setStatus] = useState<SourceStatus | null>(null)
  useEffect(() => {
    fetch(`/api/workspaces/42/sources/${sourceId}`)
      .then((response) => response.json())
      .then((source) => setStatus(source.status))
  }, [sourceId])
}
```

**Doğru (aynı query key'i kullanan tüm instance'lar tek bir request'i paylaşır):**

```tsx
function SourceStatusBadge({ sourceId }: { sourceId: number }) {
  const { data: source } = useSourceQuery(sourceId)
}
```

`useSourceQuery`, `features/source/api/sourceQueries.ts`'te `sourceKeys.detail(sourceId)` key'i ile tanımlıdır. Bilgi kaynakları listesi ve kaynak detayı aynı anda bu badge'i onlarca kez render etse de tek bir request gider.

**Değişmeyen (immutable) veri için:**

```tsx
export function useEmbeddingModelsQuery() {
  return useQuery({
    queryKey: embeddingKeys.models(),
    queryFn: listEmbeddingModels,
    staleTime: Infinity,
  })
}
```

**Mutation'lar için:**

```tsx
function UploadSourceVersionButton({ sourceId, file }: { sourceId: number; file: File }) {
  const uploadMutation = useUploadSourceVersionMutation(sourceId)
  return (
    <button type="button" disabled={uploadMutation.isPending} onClick={() => uploadMutation.mutate({ file })}>
      Yükle ve işle
    </button>
  )
}
```

Referans: [TanStack Query](https://tanstack.com/query/latest), [SWR](https://swr.vercel.app)
