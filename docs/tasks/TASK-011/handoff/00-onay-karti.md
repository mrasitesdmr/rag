TASK: TASK-011
# Onay kartı: Canlı yapılandırma ve Sadece LLM modu

## Ne yapılacak
- Önizleme'de telefonun yanında, lab'daki gibi bir ayar şeridi çıkar: Model, Embedding, Retrieval, Soru yeniden yazma ve bilgi bankası. Değerler asistanın gerçek ayarlarından gelir; ekranda hiçbir model adı sabit yazılmaz.
- Asistana yeni bir sunucu ayarı eklenir: "Sadece LLM". Bu ayar açıkken asistan rehbere bakmaz; soruyu kısa bir talimatla doğrudan modele sorar. Cevapta kaynak numarası ve "Rehber · s. N" düğmesi olmaz.
- Şerit durumu renkle gösterir: Sadece LLM'de kırmızı "Retrieval kapalı (Sadece LLM)"; sıralama modeli kapalıysa sarı "reranker DOWN"; yeniden yazma kapalıysa sarı "kapalı"; belge yoksa sarı uyarı.
- "Asistan nasıl cevaplıyor?" kartının metni etkin ayara göre değişir (lab'daki üç metin).
- Ayarlar ekrandan değişmez. Değiştirmek için ortam değişkeni değiştirilir ve servis yeniden başlatılır. Üst bardaki "Canlı yapılandırma" rozeti ve ipucu metni olduğu gibi kalır.

## Bittiğini nasıl anlayacaksın
1. Varsayılan ayarlarla ve sadece rehber yüklüyken şerit şunları gösterir: "Model qwen3.5:9b · Embedding bge-m3 · Retrieval rerank 20→5 · Soru yeniden yazma açık" ve bilgi bankası (soru 1'deki biçimde).
2. Hiç belge yokken şeritte sarı "belge yok" görünür.
3. Asistan Sadece LLM ayarıyla yeniden başlatılır: şerit kırmızı "Retrieval kapalı (Sadece LLM)" gösterir. "Kartımı kaybettim, ne yapmalıyım?" cevabında kaynak numarası ve rehber düğmesi yoktur.
4. Sıralama modeli durdurulur: şerit sarı "reranker DOWN" gösterir (soru 2'deki yönteme göre).
5. Raporda aynı sorular iki ayarla gerçek modele sorulmuş, cevaplar yan yana karşılaştırılmıştır.
6. Otomatik testler ve derlemeler geçer. Ekran kodunda sabit model adı olmadığı aramayla doğrulanır.

## Yapılmayacaklar
- Ayarları ekrandan değiştirmek.
- Değerlendirme ekranındaki "Ölçümü çalıştır · …" metni (TASK-016). Aynı ayar bilgisini o task kullanacak.
- Bilgi bankası servisinde değişiklik. Asistan onun mevcut bilgilerini okur.

## Benim verdiğim kararlar
1. **Sadece LLM'de takip sorusu yeniden yazılmaz,** çünkü arama yok. Önceki soru-cevaplar yine modele gider (lab böyle).
2. **Sadece LLM cevabında "[1]" yazsa bile düz yazı kalır,** tıklanmaz. Model "rehberde bulamadım" dese de "cevap yok" sayılmaz.
3. **Sadece LLM cevabından sonra yan kartta parça listesi çıkmaz.** "Asistan nasıl cevaplıyor?" kartı kalır (lab böyle).
4. **"Asistan nasıl cevaplıyor?" metninde küçük bir değişiklik var.** Lab'daki "en iyi 5'i qwen3.5:9b'ye gider" yerine "en iyi 5 parça qwen3.5:9b modeline gider" yazar. Sayı ve model adı ayardan geldiği için "5'i", "'ye" gibi ekler her değerde doğru çıkmaz. Sadece LLM metni lab'dakiyle aynı kalır.
5. **Bilgi bankasında sadece işlenmesi bitmiş belgeler sayılır.** Asistan sadece onlarda arayabilir. İşlenen ya da hata alan belge sayılmaz.
6. **Bilgi bankası servisine ulaşılamazsa şerit yine çıkar.** Bilgi bankası yerinde sarı "ulaşılamıyor", Embedding yerinde "—" görünür.
7. **Ayar bilgisi hiç okunamazsa** şeridin yerinde hata mesajı çıkar. Sohbet çalışmaya devam eder.
8. **Şerit sayfa açılınca ve her cevaptan sonra yenilenir.** Soru 2'de B ya da C seçilirse ayrıca 15 saniyede bir yenilenir.
9. **Sadece LLM kayıtlarında** arama cümlesi ve parça sayısı boş kalır, strateji "yok" yazılır.

## Çalışacak roller
1. planner
2. backend-coder: asistan servisi (Sadece LLM, ayar bilgisi, sıralama modeli ölçümü, kayıt). Gerçek modelle bir kez dener.
3. frontend-coder: Önizleme şeridi, "Asistan nasıl cevaplıyor?" kartı, Sadece LLM cevabının görünümü
4. tester: otomatik testler
5. devops: Docker'a Sadece LLM ayarı ve kurulum notu
6. qa: iki ayarla gerçek model karşılaştırması, şerit denemeleri

Atlanan rol yok. Bilgi bankası servisi değişmiyor.

## Kararını bekleyen sorular
1. Şeritte bilgi bankası nasıl görünsün?
   Neden önemli: Lab'da tek rehber vardı ("Rehber 2026.10"); şimdi birden fazla belge yüklenebiliyor.
   Seçenekler:
   A) "Bilgi kaynakları" başlığıyla. Tek belgede adı ve sürümü ("Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10"; uzun ad 40 harfte kısalır). Birden fazla belgede sayı ("3 belge"). Hiç yoksa sarı "belge yok".
   B) Lab'daki "Rehber" başlığıyla. Tek belgede sadece sürüm ("Rehber 2026.10"). Birden fazla belgede "Rehber 3 belge". Hiç yoksa sarı "Rehber belge yok".
   C) "Bilgi kaynakları" başlığıyla her zaman sayı: "1 belge", "3 belge", sarı "belge yok".
   Önerim: A, çünkü TASK'taki örnekle aynı ve menüdeki adla uyumlu. Tek belgede hangi belgenin kullanıldığı da görünür.

2. Sıralama modeli (reranker) kapandığında şerit bunu nasıl anlasın?
   Neden önemli: Senaryo 4 "reranker durdurulunca şerit DOWN gösterir" diyor. Yönteme göre bu, soru sormadan da görünür ya da görünmez.
   Seçenekler:
   A) Son soruya bakarak. Bir soruda sıralama modeli cevap vermezse "reranker DOWN" çıkar; bir sonraki başarılı soruda kaybolur. Soru sorulmadan değişmez.
   B) Düzenli kontrolle. Asistan 15 saniyede bir sıralama modelinin açık olup olmadığını kontrol eder; ekran da 15 saniyede bir yenilenir. Soru sormadan en geç 30 saniyede görünür.
   C) İkisi birden. Düzenli kontrol yapılır, ayrıca bir sorudaki başarı ya da hata durumu hemen günceller.
   Önerim: C, çünkü senaryo 4 soru sormadan karşılanır. Soru sırasında bozulursa da kullanıcı bunu hemen görür.

## Riskler
- "Asistan nasıl cevaplıyor?" metni lab'dan iki kelime farklı olacak (karar 4).
- Bilgi bankası servisi açık ama çok yavaşsa şeridin görünmesi 15 saniyeye kadar gecikebilir. Servis tamamen kapalıysa uyarı hemen görünür.

## Kullanıcı cevapları (2026-11-02)
- Soru 1: **A** — "Bilgi kaynakları" başlığı; tek belgede ad · sürüm (40 harfte kısalır), çoklu belgede "N belge", hiç yoksa sarı "belge yok".
- Soru 2: **C** — 15 sn'de bir sağlık kontrolü + her sorudaki rerank başarı/hata durumu hemen günceller.
- Kartın geri kalanı (karar 1–9, sse.md değişikliği dahil) olduğu gibi onaylandı.

ONAY: VERİLDİ
