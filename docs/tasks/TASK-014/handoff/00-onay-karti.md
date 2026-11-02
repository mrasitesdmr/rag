TASK: TASK-014
# Onay kartı: Konuşmalar ekranı ve tur durumu

**Senden 5 karar bekliyorum; hepsi tur durum çipiyle ilgili (aşağıda, "Kararını bekleyen sorular").**

## Ne yapılacak
- Konuşmalar ekranı, Önizleme'de açılan gerçek konuşmaları lab'daki tabloyla gösterir: en yeni üstte, sayfalı. Örnek konuşma yoktur.
- Yan menüde "Konuşmalar"ın yanında gerçek konuşma sayısı görünür.
- Bir satıra basınca detay açılır. Her turda soru, gerekirse arama sorgusu, alıntılı cevap, durum çipi, üretim şekli, rehber sürümü, token sayıları ve süre görünür. Altında da modele giden parçaların tablosu yer alır.
- Tablodaki bir satıra ya da cevaptaki [n]'e basınca parça penceresi açılır.
- Cevabı hâlâ yazılan tur "işleniyor" görünür; bitince ekran kendiliğinden güncellenir.
- Geri bildirim ve temsilciye aktarma, TASK-013'te seçtiğin gibi görünür: listede "Temsilcide" çipi, detayda aktarma satırı ve her turda "geri bildirim: Evet/Hayır".

## Bittiğini nasıl anlayacaksın
1. Önizleme'de iki soru sor. Konuşmalar'da en üstte bu konuşma "2" tur ve "Önizleme" olarak görünür, menüdeki sayı artar.
2. Detayda 2. turun altında "Arama sorgusu: …" satırını görürsün. Parça tablosunda her parçanın aramadaki sırası (Vector #) ve prompt'taki yeri ([n]) görünür.
3. "Kripto para hesabı açabilir miyim?" sorusu BİLMİYOR çipiyle görünür. Diğer çipler aşağıdaki cevaplarına göre çıkar.
4. Hiç konuşma yokken ekranda boş durum metni çıkar. Olmayan bir konuşmanın adresi açılınca "Konuşma bulunamadı." yazar.
5. Lab ile yan yana karşılaştırılır; testler ve derlemeler geçer.

## Yapılmayacaklar
- Konuşma silme, dışa aktarma, filtreleme. Ayrıca gerçek bir müşteri kanalı yok (TASK dışı).
- Parça tablosundaki "cevap burada" etiketi ile KAÇIRDI ve ÜRETİM HATASI durumları. Bunlar golden set'e bağlıdır ve TASK-016'da ele alınır.
- Bilgi bankası servisinde, cevap akışında ve Docker kurulumunda değişiklik.

## Benim verdiğim kararlar
1. **Listede sadece en az bir sorusu olan konuşmalar görünür.** Sayaç da bunları sayar. Açılıp sorusu reddedilen boş konuşmalar görünmez.
2. **Durum kaydedilmez, her açılışta kayıttan hesaplanır.** Bu yüzden "Hayır" geri bildirimi gelince çip hemen değişir.
3. **Konuşma kimliği lab'daki gibi `cnv_12` biçiminde görünür.** Adres `/konusmalar/12` olur.
4. **Saat:** bugünkü konuşmada sadece saat (14:32), eski konuşmada tarih ve saat görünür.
5. **Üretim etiketi turun kendi ayarından gelir:** "Sadece LLM", "RAG · top-5", "RAG · rerank 20→5" ya da "RAG · reranker DOWN".
6. **Rehber sürümü, o turda modele giden parçaların sürümüdür.** Sadece LLM turunda gösterilmez.
7. **Detayda bir konuşmanın bütün turları tek sayfada gösterilir** (lab'daki gibi).

## Çalışacak roller
1. planner
2. backend-coder: asistan servisi (liste ve detay uçları, durum hesabı)
3. frontend-coder: Konuşmalar listesi, detay ve menü sayacı
4. tester: otomatik testler (her durum kuralı ayrı test)
5. qa: sistemi kurar, üç örnek senaryoyu dener, görünümü lab'la karşılaştırır.

Atlanan: devops. Docker, ayarlar ve modeller değişmiyor.

## Kararını bekleyen sorular
Lab altı durumu, hangi sorunun cevabının rehberde olduğunu ve doğru parçanın hangisi olduğunu önceden bildiği için hesaplayabiliyordu. Gerçek soruda bu bilgi yok. Her soru tek bir tur türünü kapsar ve diğerlerinden bağımsız cevaplanabilir.

1. Asistan "Bu konuda rehberimizde bilgi bulamadım." dediğinde tur hangi çiple görünsün?
   Neden önemli: Lab bu durumda, cevabın rehberde olup olmadığına göre üç durumdan birini veriyordu: BİLMİYOR, KAÇIRDI ya da ÜRETİM HATASI.
   Seçenekler:
   A) **(Önerilen)** Şimdilik her zaman BİLMİYOR (yeşil). KAÇIRDI ve ÜRETİM HATASI golden set ile TASK-016'da eklenir. Orada sorunun doğru parçası biliniyorsa ayrım yapılır.
   B) Golden set gelene kadar bu turlarda çip gösterilmez.
   C) Ayrı bir değerlendirici model, modele giden 5 parçayı okur. Cevap içlerindeyse ÜRETİM HATASI, değilse BİLMİYOR verir. KAÇIRDI yine ayırt edilemez, çünkü model rehberin tamamına bakmaz.
   Önerim A, çünkü sahte bilgi üretmez: asistan gerçekten "bilmiyorum" dedi. Rehberde cevabı olan sorular TASK-016'ya kadar BİLMİYOR görünür, ama parça tablosundan anlaşılır.

2. Cevap veren bir RAG turu ne zaman DAYANAKLI sayılsın?
   Neden önemli: Lab, kaynak numarası hiç olmayan cevaba da DAYANAKLI diyordu.
   Seçenekler:
   A) **(Önerilen)** Cevapta en az bir geçerli kaynak numarası ([1]…[5]) varsa DAYANAKLI, hiç yoksa DAYANAKSIZ (kırmızı).
   B) Lab gibi: kaynak numarası olmasa da DAYANAKLI.
   Her iki seçenekte Sadece LLM turu lab'daki gibi DAYANAKSIZ olur. Önerim A, çünkü kaynak göstermeyen cevabın dayanağı ekrandan doğrulanamaz.

3. Müşteri bir cevaba "Faydalı mı? → Hayır" dediğinde tur HATALI (kırmızı) görünsün mü?
   Neden önemli: Lab'da HATALI'yı bir insan işaretliyordu. Bu üründe cevabı değerlendiren tek insan müşteri.
   Seçenekler:
   A) **(Önerilen)** Evet. "Hayır" denen cevaplı tur HATALI olur, "Evet"e dönerse eski çipine döner. "Bulamadım" turu ve Sadece LLM turu kendi çipinde kalır.
   B) Hayır. HATALI hiç kullanılmaz; "Hayır" sadece turun bilgi satırında görünür.
   C) Yönetici detay ekranında turu "Hatalı" diye işaretleyebilsin. Bunun için yeni bir düğme ve kayıt gerekir; lab'da yok.
   Önerim A, çünkü TASK-013'teki "bu tur incelemeye düştü" bildirimi Konuşmalar'da görünür bir karşılık bulur.

4. Cevapları ayrı bir dil modeli (değerlendirici model) kontrol etsin mi?
   Neden önemli: Bu model cevabın parçalarla çelişip çelişmediğini ve "bulamadım" dediği hâlde cevabın parçalarda olup olmadığını bulabilir. Bedeli var.
   Seçenekler:
   A) **(Önerilen)** Hayır. Çipler sadece kayıtlı gerçeklerden (alıntı, "bulamadım" cümlesi, geri bildirim, hata) hesaplanır.
   B) Evet. Her cevaptan sonra aynı lokal model (qwen3.5:9b) arka planda cevabı parçalarla karşılaştırır. Sonuç birkaç saniye sonra çipe yansır. Cevaplama ile aynı ekran kartını paylaştığı için sorular yavaşlar. Model de yanılabilir. Bu işin kapsamı büyüdüğü için plan yeniden yazılır.
   Önerim A. Bu task'ın amacı "doğru parça prompt'a girdi mi" sorusunu tablodan göstermek; model kararı yeni bir belirsizlik ekler.

5. Hatayla biten tur (dil modeline ya da bilgi bankasına ulaşılamadı, cevap yarıda kesildi) nasıl görünsün?
   Neden önemli: Lab'da bu durum yoktu; gerçek sistemde kayıtlarda var.
   Seçenekler:
   A) **(Önerilen)** Gri "TAMAMLANMADI" çipi. Turun bilgi satırında sebep yazar: "dil modeline ulaşılamadı", "bilgi bankasına ulaşılamadı", "cevap yarıda kesildi" ya da "beklenmeyen hata". Yarım cevap olduğu gibi görünür.
   B) Çip yok, sadece bilgi satırında sebep.
   C) İki çip: kırmızı "HATA" (servis hatası) ve gri "İPTAL" (cevap yarıda kesildi).
   Önerim A, çünkü durum kesin biliniyor ve tek çip listeyi sade tutar.

## Riskler
- Golden set gelene kadar, rehberde cevabı olduğu hâlde bulunamayan sorular da BİLMİYOR görünür (soru 1, A).
- "İşleniyor" bilgisi asistan servisinin belleğinde tutulur. Servis cevap yazılırken yeniden başlarsa o cevap hiç kaydedilmez (bugünkü davranış) ve listeden düşer; kayıtlı turlar etkilenmez.

ONAY: VERİLDİ
