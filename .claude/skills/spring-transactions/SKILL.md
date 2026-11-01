---
name: spring-transactions
description: Spring'de @Transactional ile transaction sınırları, propagation, isolation, rollback kuralları, readOnly, optimistic/pessimistic locking, retry + idempotency ve commit sonrası event yayınlama (WebSocket bildirimi dahil). Bir service metoduna transaction eklerken, birden fazla adımlı yazma işlemi tasarlarken, eşzamanlı yazma çakışması (ör. aynı kaynağa iki içe alma işi, aynı tura iki geri bildirim, aynı konuşmaya iki soru, tur kaydının akış bitince yazılması, dış model çağrısında transaction'ı açık tutmama) çözerken, LazyInitializationException veya "rollback olmadı" sorunu incelerken kullan.
license: MIT
metadata:
  sources:
    - https://github.com/HoangNguyen0403/agent-skills-standard/tree/main/skills/database/database-transactions
    - https://github.com/affaan-m/ECC/tree/main/skills/jpa-patterns
  adapted-for: Spring Framework 7 / Spring Boot 4.1
---

# Spring Transactions

Bir iş aksiyonu = bir transaction = tek bir tutarlılık stratejisi.

## Temel kurallar

- `@Transactional` sadece **service** katmanında, `public` metotlarda. Controller'da ve repository'de yok (Spring Data repository metotları zaten transactional).
- Okuma metotları `@Transactional(readOnly = true)`. Sınıf seviyesinde `readOnly = true`, yazma metodunda `@Transactional` ile override et.
- Transaction gövdesi kısa olsun: içinde HTTP çağrısı, AI/dış servis çağrısı, dosya I/O, `Thread.sleep`, WebSocket mesajı gönderme yok.
- `spring.jpa.open-in-view=false`. Lazy ilişkileri transaction içinde DTO'ya map'le; entity'yi controller'a döndürme.
- Domain exception'ları `RuntimeException`'dan türet. Checked exception varsayılan olarak **rollback yapmaz**; zorunluysa `rollbackFor` ver.
- Exception'ı transaction içinde yakalayıp yutma; rollback-only işaretlenmiş transaction commit'te `UnexpectedRollbackException` fırlatır.

## Proxy tuzakları

```java
@Service
public class ConversationService {

    public void sendMessage(...) {
        this.saveTurn(...);
    }

    @Transactional
    public void saveTurn(...) { }
}
```

`this.saveTurn(...)` proxy'den geçmez; `@Transactional`, `@Async`, `@Retryable`, `@Observed` **çalışmaz**. Çözüm: metodu ayrı bir bean'e taşı. Aynı kural `private`/`final` metotlar için de geçerli.

## Propagation ve isolation

| Ayar | Ne zaman |
|------|----------|
| `REQUIRED` (varsayılan) | Neredeyse her zaman |
| `REQUIRES_NEW` | Ana işlem rollback olsa bile kalıcı olması gereken kayıt (audit). Ayrı bağlantı tutar; dikkatli kullan |
| `MANDATORY` | Metot sadece mevcut bir transaction içinde çağrılmalıysa |
| `NESTED`, `NOT_SUPPORTED`, `NEVER` | Gerekçe yazmadan kullanma |

Isolation: veritabanı varsayılanını kullan (PostgreSQL'de `READ_COMMITTED`). Yükseltmek yerine aşağıdaki locking stratejilerini tercih et.

## Eşzamanlı yazma: strateji seç

"Kontrol et, sonra yaz" (kaynak zaten işleniyor mu? tur zaten geri bildirim aldı mı? konuşmada süren tur var mı?) iki request aynı anda geldiğinde bozulur. Stratejiyi açıkça seç:

1. **Veritabanı constraint'i (ilk tercih)** — tekillik kuralları DB'de: ör. `CREATE UNIQUE INDEX uk_ingestion_jobs_source_id_active ON ingestion_jobs (source_id) WHERE stage NOT IN ('DONE', 'FAILED')` (koşullu ya da ifade içeren tekillik `UNIQUE` constraint ile değil unique index ile yazılır). İhlal ancak flush anında oluşur: service'te `saveAndFlush(...)` ile flush'ı zorla, `DataIntegrityViolationException`'ı yakala ve `ConflictException("INGESTION_IN_PROGRESS", ...)`'a çevir; aksi halde hata commit'te, service metodunun dışında fırlar. Exception'ı yakaladıktan sonra aynı transaction'da DB işine devam etme (transaction rollback-only olur).
2. **Pessimistic lock** — sayaç/kapasite gibi kısa, sıcak kontroller:
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select s from Source s where s.workspace.id = :workspaceId and s.id = :id")
   Optional<Source> findByWorkspaceIdAndIdForUpdate(@Param("workspaceId") long workspaceId, @Param("id") long id);
   ```

   Lock sorgusu `@Transactional` service metodundan çağrılır; lock commit/rollback'e kadar tutulur.
3. **Optimistic lock** — çakışmanın nadir olduğu güncellemeler: entity'ye `@Version` ekle, `ObjectOptimisticLockingFailureException`'da retry et veya 409 dön. Dikkat: `@Version` sadece entity'nin kendi kolonları ve sahibi olduğu (owning side) ilişkiler değiştiğinde artar; `mappedBy` (inverse) koleksiyona eleman eklemek parent'ın versiyonunu **artırmaz**. Bu yüzden "konuşmaya tur ekle" gibi koleksiyon büyüten bir kural optimistic lock ile korunamaz.

**retrieval-api, içe almayı yeniden deneme:** bir kaynağın aynı anda tek aktif işi olur. `retryIngestion` aktif iş varsa (`existsBySourceIdAndStageIn(sourceId, activeStages())`) `INGESTION_IN_PROGRESS` (409) döner. `ingestion_jobs` üzerindeki kısmi unique index (`WHERE stage NOT IN ('DONE', 'FAILED')`) iki eşzamanlı isteğe karşı son güvencedir; `saveAndFlush` sırasında gelen `DataIntegrityViolationException` da `INGESTION_IN_PROGRESS`'e çevrilir. İş, transaction commit edildikten sonra (`TransactionSynchronization.afterCommit`) executor'a verilir; böylece arka plan işi henüz commit edilmemiş satırı aramaz. Kuyruk biçiminde iş alma gerekirse `FOR UPDATE SKIP LOCKED` (`postgres-patterns`).

**assistant-api, yeni soru ve tur kaydı:** yeni soru konuşmayı kilitleyerek okur; `STREAMING` durumda tur varsa `TURN_IN_PROGRESS` (409) döner. Tur, `done` olayından önce commit edilir; dil modeli çağrısı (akış boyunca saniyeler) transaction dışındadır. Aynı tura ikinci geri bildirim `FEEDBACK_ALREADY_SUBMITTED` (409) döner.

In-memory `synchronized`/lock tek instance'ta çalışır; DB state'i için yeterli değildir. In-memory state için `java-concurrency` skill'ine bak.

## Retry + idempotency

> Bu projede şu an optimistic lock (`@Version`) ve retry yoktur; aşağıdaki kaynak yeniden adlandırma örneği varsayımsaldır (böyle bir endpoint yok). Desen, ileride çakışma olasılığı olan bir güncelleme eklenirse kullanılır.

Retry, transaction'ın **dışında** olmalı (her deneme yeni transaction). Spring Framework 7'de çekirdekte gelir:

```java
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
class ResilienceConfig {
}

@Service
public class SourceRenameRetryService {

    private final SourceService sourceService;

    public SourceRenameRetryService(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Retryable(includes = ObjectOptimisticLockingFailureException.class, maxRetries = 3, delay = 50, multiplier = 2)
    public SourceResponse renameSource(long workspaceId, long sourceId, String name) {
        return sourceService.renameSource(workspaceId, sourceId, name);
    }
}
```

- Retry'lı metot ayrı bir bean'dedir (self-invocation proxy'yi atlar) ve controller bu bean'i çağırır.
- Örnekteki çakışma `Source`'un kendi kolonlarındaki değişikliktir (worker `status`'u güncellerken kullanıcı adı değiştirir; `@Version` artar); her deneme güncel state'i yeniden okur ve kuralı (kaynak işlenirken ad değişebilir mi?) yeniden uygular.
- Embedding modeli çağrısının retry'ı ayrı bir konudur (dil modeli akışı retry edilmez): ağ hatasında (429/5xx) backoff ile `EmbeddingClient` seviyesinde yapılır, hiçbir transaction açık değilken.
- Idempotent olmayan işlemi retry etme (çift kayıt = bug).
- İçe almayı yeniden deneme retry'a ihtiyaç duymaz: eşzamanlı ikinci istek kısmi unique index'e takılır ve 409 alır (yukarıdaki "retrieval-api, içe almayı yeniden deneme").

## Commit sonrası yan etkiler

Kaynak ve iş commit olmadan arka plan işi başlatılırsa iş, henüz görünmeyen satırları arar; ileride bir "ilerleme güncellendi" mesajı commit'ten önce yayınlanırsa rollback durumunda istemciler hayalet ilerleme görür. Yan etkileri commit'e bağla.

**retrieval-api'de iş başlatma** (`IngestionService`): event yerine doğrudan transaction senkronizasyonu.

```java
@Transactional
public IngestionJobResponse createSource(long workspaceId, String name, MultipartFile file) {
    // ...kaynak ve iş satırları saveAndFlush ile yazılır
    submitAfterCommit(job.getId(), content, kind);
    return IngestionJobResponse.from(job);
}

private void submitAfterCommit(long jobId, byte[] content, SourceKind kind) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            ingestionJobRunner.submit(jobId, content, kind);
        }
    });
}
```

**retrieval-api'de ilerleme kaydı** (`IngestionJobLifecycleService`): tüm içe alma tek transaction değildir. Her aşama kendi kısa transaction'ında commit edilir; embedding çağrısı (ağ, saniyeler) hiçbir transaction açık değilken yapılır. Böylece polling eden istemci her batch'ten sonra ilerlemeyi görür.

```java
@Transactional
public void recordEmbeddingProgress(long jobId, int embeddedChunks) {
    getJob(jobId).recordEmbeddingProgress(embeddedChunks);
}

@Transactional
public void completeJob(long jobId, List<ChunkDraft> drafts, List<float[]> embeddings) {
    IngestionJob job = getJob(jobId);
    Source source = lockSource(job);                     // kaynak satırı PESSIMISTIC_WRITE
    chunkRepository.saveAll(newChunks);                  // parçalar ve kaynak durumu aynı transaction'da
    source.completeIngestion(job.getVersion(), job.getFileName(), newChunks.size(), now);
    job.complete(now);
}
```

Parçaların yazılması ve kaynağın tamamlandı işaretlenmesi aynı transaction'dadır: commit anına kadar arama bu belgeyi görmez, yarım yazılmış parça kümesi aranmaz.

**WebSocket gelirse:** ilerleme yayını da commit'e bağlanır (`@TransactionalEventListener(phase = AFTER_COMMIT)` ya da aynı `afterCommit` senkronizasyonu). Event ve STOMP payload'ı entity değil, immutable `record`'dur; alanları `IngestionJobResponse` ile aynıdır (`spring-websocket`).

## Programatik transaction

Tek bir metot içinde birden fazla kısa transaction gerekiyorsa `TransactionTemplate` kullan; `@Transactional`'ı zorlamak için sınıf bölmekten daha açıksa tercih et.

## Test notları

- `@Transactional` test metodu sonunda rollback yapar; `AFTER_COMMIT` listener'lar **tetiklenmez** ve commit anındaki constraint hataları görünmez. Bu davranışları test ederken test sınıfını `@Transactional` yapma, veriyi her testten **önce** `@BeforeEach` ile temizle (`spring-jpa-testing/references/transactional-tests.md`).
- Eşzamanlılık kuralını (aynı kaynağa eşzamanlı iki yükleme; aynı konuşmaya eşzamanlı iki soru) integration testte paralel isteklerle doğrula.
- Detay: `spring-jpa-testing`.

## Kontrol listesi

```
- [ ] Transaction sınırı tek bir iş aksiyonuna karşılık geliyor
- [ ] Okuma yolları readOnly
- [ ] Transaction içinde dış çağrı / WebSocket gönderimi yok
- [ ] Eşzamanlı yazma stratejisi (constraint / pessimistic / optimistic) açık
- [ ] Retry varsa transaction dışında ve işlem idempotent
- [ ] Yan etkiler AFTER_COMMIT'e bağlı
- [ ] Self-invocation yok
```
