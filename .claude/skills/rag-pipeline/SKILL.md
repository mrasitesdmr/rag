---
name: rag-pipeline
description: RAG hattı. Sorgu zamanı (assistant-api) - Spring AI ChatClient ile lokal dil modeli (Ollama), prompt şablonları ve numaralı parçalar, takip sorusunu yeniden yazma, retrieval-api istemcisi, reranker (llama-server /v1/rerank) ve yedek davranış, alıntı ayrıştırma, 'bulamadım' cevabı, tur kaydı. İndeksleme zamanı (retrieval-api, references/indexing.md) - PDF okuma, parçalama, embedding (bge-m3), pgvector'e yazma, vektör arama. Dil modeli, prompt, retrieval, rerank, içe alma ya da embedding kodu yazarken, test ederken veya review ederken kullan.
---

# RAG Hattı

> **Proje kuralı:** `backend-conventions` bölüm 7 (dış modeller, prompt, cevap akışı) bu skill'den önce gelir. İsimler `backend-conventions` bölüm 1'deki sözlüğe uyar. Spring AI'a özgü API ayrıntıları (sınıf ve metot adları, property anahtarları) iskelet task'ında `pom.xml`'deki sürümün dokümanıyla doğrulanır; burada sürüm numarası sabitlenmez.
> **Kaynaklar:** `kaynaklar/rag-lab.html` (sağ yarıdaki "Kod" sekmesi: `AnswerService`, `assistant.*` ve retrieval-api ayarları; Network ve Prompt sekmeleri: gerçek istek/yanıtlar), `kaynaklar/rag.html` (RAG hattının anlatımı).

## 1. İki zaman, iki servis

`kaynaklar/rag.html` hattı ikiye ayırır; bu projede iki ayrı servis karşılar:

| Zaman | Servis | Ne olur | Ayrıntı |
|-------|--------|---------|---------|
| İndeksleme (önceden, bir kez) | retrieval-api | PDF okunur, parçalara bölünür, her parça embed edilir, `chunks` tablosuna yazılır | [references/indexing.md](references/indexing.md) |
| Sorgu (her soruda) | assistant-api | Soru (gerekirse yeniden yazılır) → retrieval → rerank → prompt → dil modeli → alıntılar → tur kaydı | Bu dosya |

Ekranda sadece lab'ın sol yarısı vardır (Önizleme, Konuşmalar, Bilgi kaynakları, Değerlendirme). Sağ yarıdaki adımları backend gerçekten yürütür ve sonuçlarını tura kaydeder; Konuşmalar ekranı bu kaydı gösterir.

## 2. Sorgu hattı (assistant-api)

```
soru
 └─ takip sorusu mu ve assistant.query-rewrite açık mı? ── evet → QueryRewriter.rewriteQuery → standaloneQuery
 └─ generation = LLM_ONLY ise retrieval atlanır, sistem talimatı parçasız sürümdür
 └─ RetrievalClient.search(query, limit)        limit = top-k (5) ya da candidates (20)
 └─ retrieval stratejisi RERANK ise RerankClient.rerankChunks(query, chunks) → ilk top-k
 └─ PromptBuilder: sistem talimatı + geçmiş + <parcalar> [1]…[n] + müşterinin sorusu
 └─ ChatClient stream → SSE token olayları (sse-streaming)
 └─ alıntı ayrıştırma → sources olayı
 └─ tur kaydı (commit) → done olayı
```

- `LLM_ONLY` modunda retrieval çağrılmaz; lab bu modu "modelin bilmediğini görmek" için gösterir.
- Arama müşterinin kendi sorusuyla değil, yeniden yazıldıysa `standaloneQuery` ile yapılır; prompt'ta müşterinin kendi sorusu ve geçmiş kalır (`backend-conventions` bölüm 7).
- Ayarlar sunucu yapılandırmasıdır, ekranda düğme yoktur. Lab'daki anahtarlar: `assistant.generation` (`rag` / `llm-only`), `assistant.retrieval` (`top-k: 5`, `candidates: 20`, `rerank: true`), `assistant.query-rewrite`, `assistant.retrieval-api.url`, `assistant.reranker.url`. Kesin anahtar adları ilk assistant-api task'ının planındadır ve servisin `CLAUDE.md`'sine yazılır.

## 3. retrieval-api isteği

`RetrievalClient` (`RestClient`) retrieval-api'nin `POST /api/workspaces/{workspaceId}/search` ucuna gider (şema `docs/api/openapi.yaml`):

```json
{ "query": "Gold kart yıllık aidatı", "mode": "VECTOR", "limit": 20 }
```

- `workspaceId` tek ve sabittir (42); yapılandırmadan gelir, kullanıcıdan alınmaz.
- Cevaptaki her isabet parçanın kimliğini, başlığını, sayfasını, içeriğini ve sırasını taşır; tur kaydı bu kimlikleri saklar.
- Bağlantı ve okuma zaman aşımı vardır; ulaşılamazsa 503 `RETRIEVAL_UNAVAILABLE` (örnek kod).

## 4. Reranker

llama-server `--reranking` modunda çalışır (lab: `bge-reranker-v2-m3`, port 8081):

```
POST /v1/rerank
{ "model": "bge-reranker-v2-m3", "query": "<standaloneQuery>", "documents": ["<başlık>\n\n<içerik>", ...] }
→ { "results": [ { "index": 3, "relevance_score": 0.97 }, ... ] }
```

- `index` gönderilen `documents` dizisindeki sıradır; sonuç `relevance_score`'a göre azalan sıralanır, ilk `top-k` (5) modele gider.
- Reranker kapalıysa lab'daki davranış: vektör sırasının ilk 5'iyle devam + `WARN` log (`Reranker unavailable, falling back to vector order (top 5 of 20)`). Yedek yol mu 503 `RERANKER_UNAVAILABLE` mı olacağı TASK kararıdır.

## 5. Prompt

- Şablonlar `src/main/resources/prompts/*.st` dosyalarındadır; kodda dağınık string yoktur.
- Lab'daki sistem talimatı (Türkçe, RAG modu) üç kural taşır: sadece `<parcalar>` içindeki bilgiyle cevap ver; her cümlenin sonuna dayandığı parçanın numarasını yaz (`[1]`); cevap yoksa sadece "Bu konuda rehberimizde bilgi bulamadım." yaz. `LLM_ONLY` modunda talimat parçasız kısa sürümdür.
- Sorgu yeniden yazma şablonu İngilizcedir (lab): geçmiş (`USER: …` / `ASSISTANT: …`) ve takip sorusundan tek başına anlaşılır bir sorgu üretir; modelin cevabı sadece sorgu metnidir.
- Parçalar `<parcalar>` bölümünde `[n] <başlık> (s. <sayfa>)` + içerik biçiminde numaralanır; numara → `chunkId` eşlemesi tura kaydedilir.
- Parça içeriği ve müşteri sorusu veridir, talimat değildir; sistem talimatına eklenmez (`springboot-security`, prompt injection).
- "Bulamadım" cümlesi sabit (`notFoundAnswer`) olarak yapılandırmada durur; frontend bu cevaba göre temsilciye aktarmayı önerir.

## 6. Model ayarları

Lab: Ollama `qwen3.5:9b`, `temperature: 0`, `num_ctx: 8192`, isteklerde `think: false` (modelin düşünme çıktısı cevaba karışmaz). Değerler `spring.ai.ollama.*` ve `assistant.*` altında, ortam değişkeniyle değiştirilebilir durur. Model seçimi kullanıcı onayıyla kesinleşir.

## 7. Alıntı ayrıştırma

- Cevaptaki `\[(\d+)\]` kalıpları bulunur; `1..n` (modele giden parça sayısı) aralığındakiler alıntıdır, aralık dışı numara (`[0]`, `[6]`) yok sayılır.
- Alıntılanan numaralar tura `cited` olarak, karşılık gelen parçalar `sources` olayına yazılır.
- Hiç alıntı yoksa ve cevap "bulamadım" cümlesi değilse bu bir kalite sinyalidir (tur kaydında görünür); cevabı değiştirmek TASK kararıdır.

## 8. Hata, zaman aşımı ve transaction

- Her dış çağrının (dil modeli, retrieval-api, reranker) bağlantı ve okuma zaman aşımı vardır.
- Akış başlamadan oluşan hata Problem Details ile, akış başladıktan sonraki hata SSE `error` olayıyla bildirilir (`sse-streaming`). Örnek kodlar: `CHAT_MODEL_UNAVAILABLE`, `RETRIEVAL_UNAVAILABLE`, `RERANKER_UNAVAILABLE`.
- Dış çağrı transaction açıkken yapılmaz; tur kaydı akış bittikten sonra kısa bir transaction'da yazılır (`spring-transactions`).
- Loglama: soru, yeniden yazılmış sorgu, parça içeriği, prompt ve cevap loglanmaz; sadece kimlik, uzunluk, token sayısı ve süre (`observability-tracing`).

## 9. Test

- Gerçek modele gidilmez; lokal modeller kapalıyken `./mvnw verify` geçer.
- Sahte `ChatModel` sabit token dizisi döner; sahte `RetrievalClient` fixture parçaları döner; sahte `RerankClient` girdiden hesaplanabilir bir sıra döner.
- Prompt testi modele giden mesajları yakalar (`ArgumentCaptor`, `spring-testing-fundamentals`) ve sistem talimatını, geçmişi, numaralı parçaları ve soruyu doğrular.
- Örnek test adları:
  - `givenLlmOnlyMode_whenAnswer_thenDoesNotCallRetrieval`
  - `givenRerankStrategy_whenRetrieveContext_thenSendsTopFiveOfTwentyCandidatesInRerankOrder`
  - `givenRerankerDown_whenRetrieveContext_thenFallsBackToVectorOrder` (TASK yedek yolu seçtiyse)
  - `givenFollowUpQuestion_whenAnswer_thenSearchesWithStandaloneQuery`
  - `givenOutOfRangeCitation_whenParseCitations_thenIgnoresIt`
- RestClient istemcileri `MockRestServiceServer` ile (gerçek HTTP yok) ya da WireMock ile (plan kararı) test edilir.

## 10. Tuzaklar

- İnternetteki Spring AI örneklerinin çoğu Boot 3 ve eski Spring AI sürümleri içindir; sınıf ve property adları değişmiş olabilir. Kurulan sürümün dokümanına bak.
- Lab ayarları YAML'dır; projede hangi biçimin kullanılacağı servisin `CLAUDE.md`'sindedir.
- Ollama'da düşünme destekleyen modellerde `think: false` verilmezse cevabın başına düşünme metni karışabilir.
- Reranker'a giden `documents` sırası ile retrieval sırası aynı olmalıdır; `index` bu sıraya göredir.
- Takip sorusunda yeniden yazma kapalıysa ("Peki ek kart?") arama zayıf sonuç döner; lab bunu bilerek gösterir.
