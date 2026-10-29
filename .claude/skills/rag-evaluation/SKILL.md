---
name: rag-evaluation
description: RAG değerlendirmesi - golden set (cevabın geçtiği parçası işaretli sorular), recall@k ve MRR hesabı, top-k ile rerank'in karşılaştırılması, değerlendirme çalıştırmasının 202 + polling ile yürütülmesi, deterministik metrik testleri ve gerçek modelle ölçümün yorumlanması. Değerlendirme ekranı, EvalRun, metrik hesabı ya da golden set üzerinde çalışırken kullan.
---

# RAG Değerlendirmesi

> **Proje kuralı:** İsimler `backend-conventions` / `frontend-conventions` sözlüğüne uyar (`evalRun`, `recallAtK`, `mrr`). Ekran lab'ın sol yarısındaki Değerlendirme ekranının aynısıdır. Ölçüm yöntemi (strateji) sunucu ayarıdır; ekranda değiştirilmez.

## 1. Ne ölçülür

Lab'daki gibi **retrieval** ölçülür: cevabın geçtiği parça, modele giden 5 parçanın arasına girdi mi? Doğru parça modele gitmiyorsa en iyi prompt bile doğru cevabı yazdıramaz. Cevabın kalitesi (doğru mu, alıntı doğru parçayı mı gösteriyor, rehberde olmayan soruda uyduruyor mu) bu ölçümün değil, qa'nın gerçek modelle denemesinin konusudur.

## 2. Golden set

- Her kayıt: soru + cevabın geçtiği parça kimliği (ya da kimlikleri) + kategori (lab'daki soru listesi kategorileri).
- Parça kimlikleri retrieval-api'den gelir ve rehberin yeni sürümünde değişir; bu yüzden altın parça sürüme bağlıdır (lab: "rehber 2026.10"). Sürüm değişince golden set güncellenir ya da ölçüm hangi sürüme karşı yapıldığını gösterir.
- Rehberde cevabı olmayan sorular ayrıca işaretlenir; recall hesabına girmez, qa'nın "bulamadım" denemesinde kullanılır.
- Golden set'in nerede durduğu (migration, kaynak dosyası, tablo) plan kararıdır.

## 3. Metrikler

- **recall@5**: altın parçası ilk 5'te olan soruların oranı.
- **MRR**: altın parçanın sırasının tersinin ortalaması; 1. sıra 1, 2. sıra 0,5, bulunmazsa 0.
- Elle hesaplanmış örnek (test beklentisi bu şekilde koddan bağımsız türetilir): 5 soru, altın parçanın sıraları `1, 2, 1, 4, yok`
  - recall@5 = 4 / 5 = **0,8**
  - MRR = (1 + 0,5 + 1 + 0,25 + 0) / 5 = **0,55**
- "Kaçan": altın parçası ilk 5'e girmeyen soru sayısı.

## 4. Stratejiler

- `TOP_K`: retrieval-api'den 5 parça, sıra vektör sırası.
- `RERANK`: retrieval-api'den 20 aday, reranker sırası, ilk 5.
- `LLM_ONLY` modunda ölçüm yapılmaz (lab: düğme pasif, "Sadece LLM modunda arama yok" uyarısı).
- Strateji sunucu ayarıdır; düğme etkin stratejiyi lab'daki gibi gösterir ("Ölçümü çalıştır · rerank 20→5"), değeri backend verir. İki stratejiyi karşılaştırmak için ayar değiştirilip sistem yeniden başlatılır; her çalıştırma kendi stratejisini kaydeder, Çalıştırmalar listesi yan yana gösterir.

## 5. Çalıştırma

- `POST /api/assistant/evals` → `202 Accepted` + `Location` (`/api/assistant/evals/{evalRunId}`) + çalıştırma DTO'su; `GET` ile durum sorgulanır.
- `EvalRun` durumları (örnek): `RUNNING`, `DONE`, `FAILED`; ilerleme (işlenen soru / toplam) taşır. Sonuç: `recallAtK`, `mrr`, kaçan sayısı, strateji, rehber sürümü, soru başına altın parçanın sırası.
- Çalıştırma arka planda yürür; her soru için sadece retrieval (ve rerank) yapılır, dil modeli çağrılmaz.
- Web polling ile izler: `evaluationKeys.run(evalRunId)` key'i, `refetchInterval` iş sürerken `EVAL_POLL_INTERVAL_MS`, bitince `false` (`frontend-conventions` bölüm 8).
- Ekran (lab): recall@5, MRR, Kaçan ve Retrieval kartları; Çalıştırmalar listesi; "Kaçanlar ve sırası değişenler" tablosu.

## 6. Test

- Metrik hesabı saf fonksiyondur ve unit test edilir; retrieval sahte sıralamalarla verilir.
- Beklenen değerler elle hesaplanır (bölüm 3); kayan nokta karşılaştırması `isCloseTo(0.55, within(1e-6))`.
- Sınırlar: hiç soru yoksa ölçüm başlamaz (hata ya da boş sonuç TASK kararıdır); altın parça hiç gelmeyen soru MRR'a 0 katar; aynı parça iki kez gelirse ilk sırası sayılır.
- Örnek test adları: `givenFiveQuestionsWithKnownRanks_whenEvaluate_thenRecallAtFiveIsPointEightAndMrrIsPointFiftyFive`, `givenLlmOnlyMode_whenStartEval_thenRejects`, `givenEvalRunning_whenPolled_thenShowsProgress`.

## 7. Yorum

- recall düşükse sorun parçalama ya da retrieval'dadır (parça çok büyük/küçük, başlık embed edilmemiş, sorgu yeniden yazma kapalı).
- recall yüksek ama cevap yanlışsa sorun prompt ya da modeldedir.
- Rerank'in kazancı "sırası değişenler" listesinde görünür; kazanç yoksa reranker'ın maliyeti (gecikme) tartılır.
- Rehberin yeni sürümünden sonra ölçüm yeniden yapılır; sonuçlar sürüm etiketiyle karşılaştırılır.
