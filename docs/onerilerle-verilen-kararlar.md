# Önerilerle verilen kararlar

Kullanıcı 2026-11-01 gecesi, onay kartlarındaki soruların hepsinin lead tarafından planner'ın önerdiği seçenekle cevaplanmasına ve task'ların bu kararlarla yürütülmesine izin verdi. Bu dosya her task için verilen kararları, seçilmeyen seçenekleri ve durdurulan noktaları listeler. Kullanıcı kontrol eder; istemediği bir karar varsa ayrı bir düzeltme task'ı açılır.

Not: TASK-024'ün kararlarını kullanıcı kendisi verdi; kayıt olsun diye buraya da yazıldı.

## TASK-024: Parça örtüşmesi ve sayfa etiketi (kullanıcı kendisi onayladı)
Commit: `2fad1a2`

| # | Soru | Seçilen | Seçilmeyenler |
|---|------|---------|---------------|
| 1 | Parçanın sayfası nasıl gösterilsin? | İlk satırın sayfası, tek sayfa ("s. 55") | Sayfa aralığı ("s. 55-56"); sayfa sonunda kesme |
| 2 | SSS kısımlarında örtüşme | Yok; her parça bir soruyla başlar | Son çifti bütün taşı; sadece sığarsa |
| 3 | Soru satırı nasıl tanınır | Satırın tamamı kalın | "?" ile biten; kalın ya da "?" |
| 4 | Hangi başlıklar SSS sayılır | Sabit liste (Sık sorulanlar, Sık sorulan sorular, Sıkça sorulan sorular, SSS, FAQ, Frequently asked questions), harf farksız | Sadece "Sık sorulanlar" |

Açık kalan Optional bulgular (task'a bölünmedi, kullanıcı karar verecek):
- `TextChunker.java:81` kesme sınırını kesin doğrulayan test yok.
- `PdfDocumentParser`'daki `hasRegular` bayrağı gereksiz; 02 raporundaki açıklama kodla çelişiyor.

## TASK-006: Vektör arama ucu (lead önerilenle onayladı)

| # | Soru | Seçilen | Seçilmeyenler |
|---|------|---------|---------------|
| 1 | Arama sorgusu en fazla kaç karakter? | 1000 karakter (TASK-008 farklı bir sınır seçerse güncellenir) | 500; 2000 |

Planner'ın kendi kararları (kartta): cevabı olmayan soruda da en yakın parçalar döner, eleme yok ("bulamadım" kararı TASK-008'de asistanın); istenenden az parça varsa olanlar döner; parça sayısı zorunlu, 1-50; sorgu baş/son boşlukları silinir; model sunucusu kapalıysa ~3 sn'de hata; bilgi bankası boşsa modele gidilmeden boş liste; model uyuşmazlığında arama reddedilir; arama ayarları lab'daki gibi (ef_search 40); ölçüm 12 soruyla yapılır.

Kullanıcının bakması gereken: `.claude/**` altındaki skill'ler (rag-pipeline, indexing.md, problem-details) arama gövdesini lab'daki gibi `{query, mode, limit}` diye anıyor; sözleşme artık `{query, topK}`. Bu dosyalar kullanıcının, değiştirilmedi.

Sonuç: qa DONE. Gerçek `bge-m3` ile 12 sorunun 12'sinde doğru parça ilk 5'te (11'i 1. sırada). 257 test geçti.
Açık kalan bulgular (task'a bölünmedi, kullanıcı karar verecek):
- Optional: `SearchService.java:67` log süresi mutantı yaşıyor (değer düşük).
- Nit: `ChunkSearchRepository.java:80` StringBuilder kapasite sabiti (`* 12`) kaldırılabilir.
- Risk: model sunucusu askıda kalırsa arama hatası 3 dakikaya kadar gecikebilir; TASK-008'de ölçülecek.
- Dev veritabanına ikinci belge eklendi: "RAG anlatımı" (kaynak 10002, 63 parça).
