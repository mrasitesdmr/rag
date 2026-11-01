TASK: TASK-006
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `postgres-patterns`, `rag-pipeline/references/indexing.md` (§6, dosyadan okundu).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|------------------------|------------|-----|
| K1, K2, Kararlar #10-#11 | `search/domain/dto/SearchChunksRequest.java` | `query`: `@NotBlank` + `@Size(max = QUERY_MAX_LENGTH)` (1000, kırpmadan önce). `topK`: `Integer` + `@NotNull` + `@Min(TOP_K_MIN)` + `@Max(TOP_K_MAX)`. Mesajlar sözleşmedeki gibi (`must not be blank`, `size must be at most 1000 characters`, `must not be null`, `must be between 1 and 50`). Sabitler `public static final`: `QUERY_MAX_LENGTH`, `TOP_K_MIN`, `TOP_K_MAX`, `TOP_K_MESSAGE` |
| K4a, Kararlar #2-#3 | `search/domain/dto/SearchHitResponse.java`, `SearchResponse.java` | 9 alan sözleşmedeki adlarla. `SearchResponse.empty()`; `hits` `List.copyOf` ile kopyalanır |
| K1, Kararlar #8 | `search/controller/SearchController.java` | `POST /api/workspaces/{workspaceId}/search`, `consumes = application/json`, `@Valid @RequestBody`; 200 + `SearchResponse` |
| Kararlar #8, K8 | `search/service/SearchService.java` | Akış: `strip()` → `ensureWorkspaceExists` (404) → `ensureEmbeddingModelMatches` (409) → `existsByWorkspaceIdAndActiveVersionIdIsNotNull` false ise boş yanıt (model çağrılmaz) → `embeddingClient.embed(List.of(stripped))` (transaction dışında) → `chunkSearchRepository.findNearest`. Sınıf `@Transactional` değil. Log: `Search completed workspaceId={} queryLength={} topK={} hitCount={} embeddingMs={} sqlMs={} durationMs={}`; boş bilgi bankasında `hitCount=0 embeddingMs=0 sqlMs=0` |
| K3a-c, K4b, K5, Kararlar #4-#7 | `search/repository/ChunkSearchRepository.java` | `@Repository` sınıf, `JdbcClient` + `SearchProperties`. `@Transactional(readOnly = true) findNearest(...)`: önce `SEARCH_SETTINGS_SQL` (dört `set_config(..., true)`), sonra `NEAREST_CHUNKS_SQL` (planla birebir: `MATERIALIZED` CTE, `ANY (ARRAY(...active_version_id...))`, dış join, `ORDER BY nearest.distance, nearest.id`). İkisi ve `toVectorLiteral` paket görünürlüğünde |
| Kararlar #12 | `config/SearchProperties.java`, `application.properties` | `search.ef-search=40`, `search.max-scan-tuples=20000`. Dosya saf ASCII (`LC_ALL=C grep -c '[^ -~]'` → 0) |
| Kararlar #8 (4) | `source/repository/SourceRepository.java` | `boolean existsByWorkspaceIdAndActiveVersionIdIsNotNull(long workspaceId)` |
| K6 | — | `EmbeddingClient` ve `GlobalExceptionHandler` değişmedi; 503 mevcut yoldan döner |
| K7 | — | Yazan kod ve migration yok |
| `retrieval-api/CLAUDE.md` | `retrieval-api/CLAUDE.md` | Durum yorumu (TASK-006); "Teknoloji"de `Hedef: search.default-ef-search…` satırı gerçek ayarlar ve SQL biçimiyle değişti; "Henüz olmayanlar"dan arama ucu ve `search.*` çıktı; "Kapsam"daki arama maddesi `{query, topK}` (mode yok, 1-1000 karakter, 1-50, güncel sürüm, kontrol sırası, boş bilgi bankası); "Paket yapısı"na `search/` ve `SearchProperties`; "Komutlar"a arama `curl` örneği; "Kurallar"daki log örneği yeni satır |
| AC1, AC2, M0 | — | Aşağıda "Doğrulama → M0" |

## Değişen dosyalar
- Yeni: `retrieval-api/src/main/java/com/rasitbank/retrieval/config/SearchProperties.java`
- Yeni: `retrieval-api/src/main/java/com/rasitbank/retrieval/search/controller/{SearchController,package-info}.java`
- Yeni: `retrieval-api/src/main/java/com/rasitbank/retrieval/search/service/{SearchService,package-info}.java`
- Yeni: `retrieval-api/src/main/java/com/rasitbank/retrieval/search/repository/{ChunkSearchRepository,package-info}.java`
- Yeni: `retrieval-api/src/main/java/com/rasitbank/retrieval/search/domain/dto/{SearchChunksRequest,SearchResponse,SearchHitResponse,package-info}.java`
- Değişen: `retrieval-api/src/main/java/com/rasitbank/retrieval/source/repository/SourceRepository.java`
- Değişen: `retrieval-api/src/main/resources/application.properties`
- Değişen: `retrieval-api/CLAUDE.md`
- Dependency, plugin, migration: yok.

## Plandan sapmalar
1. **M0 ortamı sıfırdan kurulmadı.** Plan "dev postgres sıfırdan (`down -v` + `up -d`)" diyordu. Lead veri silme komutu çalıştırılmamasını, var olan dev verisinin kullanılmasını istedi. `docker compose -f deployment/docker-compose.dev.yml start` ile açıldı. İçinde TASK-024'ten kalan Rehber 2026.10 vardı: kaynak 10001, iş 1 `DONE`, sürüm 1 aktif, 154 parça. Rehber yeniden yüklenmedi.
2. **Dev veritabanında artık ikinci bir kaynak var.** M0.4 için Belge B yüklendi: kaynak 10002 "RAG anlatımı", sürüm `v1`, iş 2 `DONE`, 63 parça. qa M1'i bu ortamda yaparsa iki kaynak görür. Hiçbir veri silinmedi.
3. **`SEARCH_SETTINGS_SQL`'de dört değer de parametre:** `:efSearch`, `:iterativeScan`, `:maxScanTuples`, `:enableSort`. Değerleri `String` olarak bağlanıyor, çünkü `set_config` text alıyor. `relaxed_order` ve `off` paket görünürlüğündeki `ITERATIVE_SCAN` ve `ENABLE_SORT` sabitlerinde. Planda yazan "parametreli"nin uygulanma biçimi bu; davranış farkı yok.
4. **Boş bilgi bankası canlı ölçülmedi.** Bunun için veri silmek gerekirdi. T25b (tester) ve qa M1 bunu kapsar; qa ölçecekse kendi boş ortamını kurmalı.

Sözleşmede eksik ya da hata görülmedi.

## Doğrulama

### Derleme ve testler
- `cd retrieval-api && ./mvnw -q compile` → geçti.
- `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD SUCCESS**, Tests run: 211, Failures: 0, Errors: 0, Skipped: 0. Bunlar mevcut testler; yeni kodun testi yok. Eşik kontrolü kapalıydı ama JaCoCo yine "All coverage checks have been met." yazdı. Logdaki iki `ERROR` satırı, mevcut testlerin bilerek ürettiği satırlar (`IngestionStartupListener`, `GlobalExceptionHandler` probe).

### M0: gerçek `bge-m3` ile ölçüm (dev ortamı)
Ortam: dev postgres (`pgvector/pgvector:0.8.1-pg18`), Ollama `bge-m3:latest`, `DATABASE_PASSWORD=retrieval-local ./mvnw spring-boot:run` (8080). Ölçüm sırasında tek kaynak vardı: Rehber 2026.10, 154 parça. `topK` 5.

**1. Ölçüm soruları (AC1).** Doğru parçalar arama çalıştırılmadan önce Kararlar #14'e göre belirlendi. Önce beklenen alt başlıktaki cevap cümlesi seçildi. Sonra bu cümleyi `content LIKE` ile içeren parçalar bulundu. Son olarak SSS ve ücret tarifesi parçaları elle tarandı ve aynı cevabı tekrar eden parçalar listeye eklendi.

| # | Soru | Seçilen cevap cümlesi (parça) | Doğru parçalar (aramadan önce) |
|---|------|-------------------------------|-------------------------------|
| 1 | Kartımı kaybettim, ne yapmalıyım? | "1. Kartınızı aşağıdaki kanallardan biriyle kapatın: … Kayıp/çalıntı bildir" (2.16) | 29 |
| 2 | Kredi kartımın yıllık üyelik ücreti iade edilir mi? | "Yıllık üyelik ücretinin yansıtıldığı tarihten sonraki 30 gün içinde kartınızı kapatırsanız ücretin tamamı iade edilir." (2.4) | 9; SSS 36 ("Aidat ödendikten sonra kartı kapatırsam param geri gelir mi?") |
| 3 | Asgari ödeme tutarını ödemezsem ne olur? | "Asgari tutarı ödemezseniz ödenmeyen asgari kısma gecikme faizi…" / "Asgari tutarı ödemezseniz • Gecikme, Kredi Kayıt Bürosu'na bildirilir…" (2.8) | 15, 16; SSS 36 ("Asgari tutarı üst üste ödemezsem ne olur?") |
| 4 | Vadeli hesabımı vadesinden önce bozarsam faiz alır mıyım? | "…vade dolmadan para çekerseniz hesap bozulur ve o vade için faiz ödenmez" (3.2) | 41; SSS 55 ("Vadeli hesabı vadesinden önce bozarsam ne olur?") |
| 5 | Kredimi erken kapatırsam tazminat öder miyim? | "…erken ödenen anapara üzerinden aşağıdaki oranlarda erken ödeme tazminatı alır" (4.10) | 72; `LIKE '%erken ödeme tazminatı%'` → 64 (4.5), 76 (SSS); tarife 144 |
| 6 | Hafta sonu EFT yapabilir miyim? | "EFT 09.00-17.00 Yapılmaz; ilk iş günü gönderilir" (5.6) | 85 |
| 7 | Yanlış hesaba para gönderdim, geri alabilir miyim? | "Raşit Bankası, gerçekleşmiş bir transferi alıcının onayı olmadan geri alamaz." (5.11) | 93; SSS 94 ("Yanlış kişiye para gönderdim. Geri alabilir miyim?") |
| 8 | Mobil uygulama şifremi unuttum, nasıl yenilerim? | "Raşit Mobil'de Şifremi unuttum: Kart numarası, kart şifresi ve SMS doğrulama kodu" (6.3) | 99; SSS 110 |
| 9 | Kartım ATM'de kaldı, ne yapmalıyım? | "ATM'de kalan kartlar güvenlik nedeniyle iade edilmez ve imha edilir…" (8.6) | 130 |
| 10 | Şikâyetime ne kadar sürede cevap verilir? | "Raşit Bankası başvurulara en geç 10 iş günü içinde yazılı ya da elektronik olarak cevap verir." (9.1) | 136; SSS 140 |
| 11 | Hafta sonu döviz alırsam hangi kur uygulanır? | "Hafta sonu kuru, cuma günü saat 17.30'daki kura göre belirlenir…" (7.4) | 116; SSS 122 |
| 12 | Kiralık kasa ücreti ne kadar? | "Küçük 5 × 25 × 45 cm 2.400 TL …" tablosu (8.2) | 125; SSS 135; tarife 148 |

Sonuçlar (`chunkId` | başlık | mesafe; ✓ = doğru parça):

| # | 1. | 2. | 3. | 4. | 5. | İlk doğru |
|---|----|----|----|----|----|-----------|
| 1 | 29 ✓ 2.16 Kayıp ya da çalıntı bildirimi 0.2980 | 30 2.16 Kayıp ya da çalıntı bildirimi 0.3308 | 108 6.10 Telefonunuz kaybolursa… 0.3821 | 28 2.15 Kart şifresi… 0.3892 | 130 8.6 ATM'de yaşanan sorunlar 0.3924 | 1 |
| 2 | 9 ✓ 2.4 Yıllık üyelik ücreti… 0.2127 | 36 ✓ 2 Kartlar · SSS 0.3500 | 154 11 Terimler sözlüğü 0.3507 | 35 2 Kartlar · SSS 0.3746 | 142 10.1 Kart ücretleri 0.3783 | 1 |
| 3 | 15 ✓ 2.8 Asgari ödeme ve gecikme 0.3539 | 16 ✓ 2.8 0.3883 | 17 2.8 0.3944 | 149 11 Terimler sözlüğü 0.4498 | 76 4 Krediler · SSS 0.4784 | 1 |
| 4 | 55 ✓ 3 Hesaplar · SSS 0.2846 | 40 3.2 Vadeli mevduat 0.3266 | 41 ✓ 3.2 Vadeli mevduat 0.3281 | 53 3.12 Stopaj 0.3287 | 76 4 Krediler · SSS 0.3330 | 1 |
| 5 | 72 ✓ 4.10 Erken ödeme… 0.2506 | 76 ✓ 4 Krediler · SSS 0.2566 | 71 4.9 Cayma hakkı 0.3799 | 36 2 Kartlar · SSS 0.3804 | 16 2.8 0.3929 | 1 |
| 6 | 79 5.2 EFT 0.2858 | 85 ✓ 5.6 İşlem saatleri, hafta sonu… 0.3014 | 94 5 Para transferi · SSS 0.3500 | 116 7.4 Döviz… 0.4063 | 80 5.3 FAST ve Kolay Adres 0.4190 | 2 |
| 7 | 93 ✓ 5.11 Yanlış gönderim ve iade 0.2488 | 94 ✓ 5 Para transferi · SSS 0.3540 | 130 8.6 0.3543 | 32 2.17 Harcama itirazı 0.3586 | 83 5.4 SWIFT 0.3664 | 1 |
| 8 | 110 ✓ 6 Dijital bankacılık · SSS 0.3056 | 99 ✓ 6.3 Şifre sıfırlama 0.3199 | 28 2.15 0.3630 | 98 6.2 İlk giriş ve şifre alma 0.3832 | 108 6.10 0.3944 | 1 |
| 9 | 130 ✓ 8.6 ATM'de yaşanan sorunlar 0.2543 | 127 8.3 ATM hizmetleri… 0.3453 | 135 8 Şube, ATM · SSS 0.3625 | 29 2.16 0.3765 | 129 8.5 Başka banka ATM'leri 0.3857 | 1 |
| 10 | 140 ✓ 9 Şikâyet · SSS 0.2453 | 136 ✓ 9.1 Şikâyet kanalları ve cevap süresi 0.3023 | 132 8.7 0.3949 | 31 2.17 0.4026 | 131 8.7 0.4216 | 1 |
| 11 | 116 ✓ 7.4 Döviz alım satımı ve hafta sonu kuru 0.2772 | 27 2.14 Yurt dışında kullanım ve kur 0.3721 | 85 5.6 0.3811 | 151 11 Terimler sözlüğü 0.4001 | 122 ✓ 7 Yatırım · SSS 0.4140 | 1 |
| 12 | 125 ✓ 8.2 Şubede yapılan işlemler ve kiralık kasa 0.2883 | 148 ✓ 10.7 Şube ve ATM ücretleri 0.3026 | 126 8.2 0.3082 | 135 ✓ 8 Şube, ATM · SSS 0.4188 | 42 3.3 Kumbara hesabı 0.4233 | 1 |

**Toplam: 12/12 soruda doğru parça ilk 5'te.** 11 soruda doğru parça 1. sırada; soru 6'da 2. sırada (1. sırada 5.2 EFT bölümü var). S1: soru 1'in ilk iki sonucu "2.16 Kayıp ya da çalıntı bildirimi" (29, 30).
Bu ölçüm golden set değildir, TASK-016'nın yerini tutmaz. Ham çıktı: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/dac6477c-4160-485c-baee-6539420abc13/scratchpad/m0-results.txt`.

**2. `EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)` (AC2).** Tek bir `BEGIN READ ONLY` transaction'ında koşuldu. Önce `SEARCH_SETTINGS_SQL` değerleri verildi (`40`, `relaxed_order`, `20000`, `off`). Sonra `NEAREST_CHUNKS_SQL` çalıştırıldı: kaynak koddaki metin, soru 1'in gerçek `bge-m3` vektörü, `workspaceId` 42, `bge-m3@1024`, `topK` 5. Vektör literal'i kısaltıldı:
```
 Incremental Sort (actual time=0.333..0.334 rows=5.00 loops=1)
   Sort Key: nearest.distance, c.id
   Presorted Key: nearest.distance
   CTE nearest
     ->  Limit (actual time=0.265..0.287 rows=5.00 loops=1)
           Buffers: shared hit=253
           InitPlan 1
             ->  Bitmap Heap Scan on sources (actual time=0.014..0.015 rows=1.00 loops=1)
                   Recheck Cond: (workspace_id = 42)
                   Filter: (active_version_id IS NOT NULL)
                   ->  Bitmap Index Scan on sources_workspace_id_lower_name_key (...)
           ->  Index Scan using chunks_embedding_hnsw_idx on chunks (actual time=0.264..0.285 rows=5.00 loops=1)
                 Order By: (embedding <=> '[…1024 sayı…]'::vector)
                 Filter: ((workspace_id = 42) AND (embedding_model = 'bge-m3@1024'::text) AND (source_version_id = ANY ((InitPlan 1).col1)))
                 Buffers: shared hit=253
   ->  Nested Loop  (chunks_pkey, Memoize + sources_pkey, Memoize + source_versions_pkey)
 Planning Time: 0.717 ms
 Execution Time: 0.386 ms
```
- `Index Scan using chunks_embedding_hnsw_idx` görünüyor.
- Güncel sürüm filtresi bir kez hesaplanan InitPlan oldu ve index taramasının `Filter`'ında kaldı.
- Dış sıralama `Incremental Sort`. `enable_sort=off` olmasına rağmen planlayıcı bunu seçti, çünkü başka yol yok; sıra `nearest.distance, c.id` olarak kesinleşiyor.
- Tam çıktı: `/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/dac6477c-4160-485c-baee-6539420abc13/scratchpad/explain-clean.txt`.

**3. Model sunucusuna ulaşılamıyorken (K6).** İkinci bir instance açıldı: `OLLAMA_BASE_URL=http://127.0.0.1:9 SERVER_PORT=8081`. Ollama'nın kendisi durdurulmadı.
```
POST localhost:8081/api/workspaces/42/search {"query":"Kartımı kaybettim, ne yapmalıyım?","topK":5}
→ status=503 content_type=application/problem+json time_total=3.095746
{"detail":"Embedding model did not answer after 3 attempts","instance":"/api/workspaces/42/search","status":503,"title":"Service Unavailable","type":"https://rag.example/errors/embedding-unavailable","errorCode":"EMBEDDING_UNAVAILABLE"}
```
Log: `Embedding request failed attempt=1/3`, `2/3` ve `3/3 status=ConnectException` (aralar 1 sn ve 2 sn).

**4. İkinci belge (S4).** Belge B, TASK-027 planındaki Chrome komutuyla üretildi (`--generate-pdf-document-outline`, `kaynaklar/rag.html`). `name='RAG anlatımı' version=v1` ile yüklendi: kaynak 10002, iş 2 `DONE`, 63 parça, yaklaşık 3,3 sn.
- `"Embedding nedir?"`: 5 sonucun hepsi 10002 "RAG anlatımı" `v1`. Parçalar 224 Reranking 0.4837, 213 Indexing hattı 0.5155, 225 0.5160, 214 0.5183, 220 0.5236.
- Soru 1 tekrar: sonuçlar tek kaynakla yapılan ölçümle aynı. 5 sonucun hepsi 10001 "Raşit Bankası Ürün ve Hizmet Rehberi" `2026.10`; parçalar 29, 30, 108, 28, 130, mesafeler de aynı.
- Karışık sorgu `"Banka asistanı müşterinin kart sorusuna rehberden nasıl cevap verir?"`: tek yanıtta iki kaynak birlikte geldi. Sıra: 131, 132, 149, 2 (10001 Rehber `2026.10`), 258 (10002 RAG anlatımı `v1`, "Kendini test et", 0.4447).

**5. Loglar (K8).**
```
Search completed workspaceId=42 queryLength=33 topK=5 hitCount=5 embeddingMs=1419 sqlMs=37 durationMs=1492   (ilk arama, model belleğe yükleniyor)
Search completed workspaceId=42 queryLength=33 topK=5 hitCount=5 embeddingMs=18 sqlMs=7 durationMs=29
```
- 8080 logunda 15 `Search completed` satırı var.
- `grep -c 'Kartımı'`: 8080 logunda 0, 8081 logunda 0. `grep -ci 'kiralık'`: 0.

**Elle denenen hata yanıtları (8080):**
- `{"query":"   ","topK":51}` → 400 `VALIDATION_FAILED`, iki ihlal: `query` "must not be blank", `topK` "must be between 1 and 50".
- `topK` yok → 400, `topK` "must not be null".
- 1001 × `a` → 400, `query` "size must be at most 1000 characters".
- `topK: 0` → 400, `violations[0].field = topK` (S2).
- Bozuk JSON ve `"topK":"beş"` → 400 `BAD_REQUEST`.
- `text/plain` → 415 `UNSUPPORTED_MEDIA_TYPE`.
- Çalışma alanı 7 → 404 `WORKSPACE_NOT_FOUND`.

İki instance da durduruldu. Dev postgres açık bırakıldı; verisi duruyor (iki kaynak).

## Tester için notlar
- **Sahtelenecek nokta:** `EmbeddingClient` (`@MockitoBean`). `SearchService` önce `modelLabel()`'ı çağırır; `bge-m3@1024` döndürmeli, yoksa 409 alırsın. Sonra `embed(List.of(strippedQuery))` çağırır ve sonucun `getFirst()`'ünü kullanır. Sahte yanıt 1024 boyutlu tek vektörlük bir liste olmalı.
- **`SearchService` sırası:** `modelLabel()` → `ensureWorkspaceExists` → `ensureEmbeddingModelMatches(workspaceId, modelLabel)` → `sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull` → `embed` → `findNearest(workspaceId, vector, modelLabel, topK)`. T3, T4 ve T2'de `embed` çağrılmamalı. `modelLabel()` bunlardan önce çağrılır (dış çağrı değil); `shouldHaveNoInteractions` yerine `should(never()).embed(any())` kullan.
- **Log (T7):** logger `com.rasitbank.retrieval.search.service.SearchService`, INFO. Biçim: `Search completed workspaceId=42 queryLength=33 topK=5 hitCount=3 embeddingMs=… sqlMs=… durationMs=…`. `queryLength` kırpılmış sorgunun `length()`'i.
- **Paket görünürlüğündeki sabitler (T24):** `ChunkSearchRepository.SEARCH_SETTINGS_SQL`, `NEAREST_CHUNKS_SQL`, `ITERATIVE_SCAN` (`relaxed_order`), `ENABLE_SORT` (`off`), `toVectorLiteral(float[])`. Test `com.rasitbank.retrieval.search.repository` paketinde olmalı.
  - `SEARCH_SETTINGS_SQL` adlı parametreleri `:efSearch`, `:iterativeScan`, `:maxScanTuples`, `:enableSort`; hepsini `String` bağla (`"40"`, `"relaxed_order"`, `"20000"`, `"off"`).
  - `NEAREST_CHUNKS_SQL` adlı parametreleri `:embedding` (`toVectorLiteral` çıktısı), `:workspaceId`, `:embeddingModel`, `:topK`. `:workspaceId` iki kez geçer; `JdbcClient` adlı parametrede bunu destekler.
  - `EXPLAIN` + `NEAREST_CHUNKS_SQL` aynı transaction'da koşmalı (`TransactionTemplate` ya da `@Transactional` test metodu). `set_config(..., true)` transaction dışında hemen düşer.
- **Fixture uyarıları (T17-T23):**
  - `chunks.id` `chunks_id_seq`'ten gelmez; kimliği açıkça ver.
  - `source_versions.ingestion_job_id` NOT NULL ve UNIQUE; her sürüme ayrı bir iş gerekir.
  - `sources.active_version_id` FK'si `source_versions`'a gider; sürümü yazdıktan sonra `UPDATE sources SET active_version_id = …` yap.
  - `chunks` CHECK'leri: heading 1-200, content 1-2000, `token_count` ≥ 1, `page_number` ≥ 1.
  - `chunk_no` sürüm içinde tekil.
  - `embedding_model` değeri `bge-m3@1024` olmalı (`EmbeddingProperties.modelLabel()`).
- **Küçük tabloda HNSW:** 3-5 satırlık tabloda da `enable_sort=off` sayesinde plan HNSW'ye gidiyor (154 satırda doğrulandı). T24 plan metninde `Index Scan using chunks_embedding_hnsw_idx` aramalı.
- **Eşitlik sırası (T18):** iç CTE `relaxed_order` ile kabaca sıralı gelir; kesin sırayı dış `ORDER BY nearest.distance, nearest.id` verir. Aynı vektörlü 7001 ve 7002'de sonuç [7001, 7002] olmalı.
- **Yanıt tipi:** controller `SearchResponse` döner; 200'de `Content-Type` `application/json`.
- **Doğrulama:** controller testinde `GlobalExceptionHandler`'ın `@WebMvcTest` kapsamına girdiğinden emin ol; mevcut controller testleri nasıl yapıyorsa öyle. `violations` sırası serbest (T14).

## Düzeltme
—

STATUS: DONE
