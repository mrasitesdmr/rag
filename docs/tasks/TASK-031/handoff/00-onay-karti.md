TASK: TASK-031
# Onay kartı: Rehberde karşılığı olmayan mesajlarda uydurma alıntının önlenmesi

## Ne yapılacak
- "Selam", "Teşekkürler", "Görüşürüz" gibi mesajlara asistan hazır, kısa ve nazik bir karşılık verir; rehberle ilgili ne sorulabileceğini söyler. Numara ve kaynak düğmesi olmaz.
- Rehberde karşılığı olmayan mesajda (Bitcoin, hava durumu, şiir, "asdfgh") cevap sadece "Bu konuda rehberimizde bilgi bulamadım." olur. Model bu cümleye numara eklese bile numara ekranda bir an bile görünmez, kayda girmez.
- Model bulamadım'ı başka kelimelerle yazıp numara eklerse ("Diğer bankaların EFT ücretlerini rehberimizde bulamadım. [3]") cevap yine sabit bulamadım cümlesine çevrilir, numara atılır. (Düzeltme turu 1)
- Müşteri kendi hesap bilgisini (bakiye, limit, borç, başvuru durumu, hesap hareketleri), başka bankaların ürünlerini ya da yatırım tavsiyesi sorarsa asistan önce bunu göremediğini ya da veremediğini söyler. Rehberde ilgili genel bilgi varsa ardından alıntıyla ekler. (Düzeltme turu 1)
- Bu giriş cümlesinin içine ya da hemen arkasına eklenen numara sistemce silinir, kayda ve son cevaba girmez. (Düzeltme turu 1)
- Asistan kendinden "asistanıyım" diye söz eder; "asistanısınız" çıkmaz.
- Rehberdeki sorularda cevap ve alıntı bugünkü gibi kalır. Bu, 100 rehber sorusuyla değişiklikten önce ve sonra ölçülür.

## Bittiğini nasıl anlayacaksın
- Önizleme'de "Selam", "selaaam", "Teşekkürler" yazarsın: hazır karşılık gelir, numara ve kaynak düğmesi yoktur.
- "Bitcoin alıp satabilir miyim?", "Bugün hava nasıl?", "Bana bir şiir yaz", "asdfgh" yazarsın: her birinde sadece bulamadım cümlesi ve "Temsilciye bağlan" önerisi.
- "Gold kart aidatı ne kadar?" sonra "Teşekkürler": ilk cevap alıntılı, ikincisi alıntısız.
- "Kartımı kaybettim, ne yapmalıyım?": cevap ve kaynak düğmesi bugünkü gibi.
- Konuşmalar'da selamlaşma turları gri "SOHBET" durumuyla görünür.
- "Kumbara hesabımda kaç lira birikti?" yazarsın: cevap "Hesabına özel bilgileri (…) göremiyorum." diye başlar; ardından alıntı varsa rehberdeki genel bilgiye dayanır.
- Yeniden ölçüm raporunda (değişti, düzeltme turu 1):
  - Kişisel hesap bilgisi, başka banka ve yatırım tavsiyesi soruları (15 mesaj): her cevap ya sadece bulamadım cümlesidir ya da kendi giriş cümlesiyle başlar ve bu cümlede numara yoktur. Bunu ölçüm betiği otomatik sayar. Varsa her alıntılı cümle, qa tarafından alıntıladığı rehber parçasıyla elle karşılaştırılır; parçaya dayanmayan alıntı 0.
  - Diğer 40 negatif mesajda alıntılı cevap 0.
  - 18 selamlaşma mesajının 18'i hazır karşılık almış.
  - 100 rehber sorusunda "bulamadım"a düşen en fazla 2. Rehber cevaplarının hiçbirinde "rehberimizde … bulamadım" kalıbı yok.
  - 100 rehber cevabından en fazla 2'si üç giriş cümlesinden biriyle ("… göremiyorum.", "… veremiyorum.") başlar. Bunu ölçüm betiği otomatik sayar.

## Yapılmayacaklar
- Sadece LLM modu. Cevap doğruluğunun modelle ölçülmesi. Dil modelinin değişmesi.
- Normal cevaplarda "bu numara gerçekten o parçaya mı dayanıyor" denetimi (bunu ancak bir model yapabilir).
- Değerlendirme ekranındaki ölçüm değişmez; eşik oradaki sonuçları etkilemez.

## Kullanıcının verdiği kararlar
1. Rehberde karşılığı olmadığı üçü birlikte anlaşılır: reranker puan eşiği, talimat ve cevabın denetlenmesi.
2. "top-5" ayarında ya da reranker kapalıyken eşik atlanır; talimat ve denetim yine çalışır.
3. Selamlaşmaya hazır metin verilir (aşağıdaki üç metin). Tanıma: geniş bir kalıp listesi; listede olmayan mesaj eşiğin altında kalırsa dil modeline sadece "selam mı, teşekkür mü, veda mı, başka mı" diye sorulur.
4. Selamlaşma turu Konuşmalar'da yeni, gri "SOHBET" durumuyla görünür.
5. Rehberdeki 100 sorudan en fazla 2'sinin "bulamadım"a düşmesi kabul edilir.

İlk ölçümden sonra (düzeltme turu 1):

6. Bulamadım'ın başka kelimelerle yazılmış hâli de yakalanır: cevapta "rehberimizde … bulamadım" geçerse cevap sabit bulamadım cümlesine çevrilir, alıntılar atılır.
7. Kişisel hesap bilgisi, başka bankalar ve yatırım tavsiyesi için hedefli talimat eklenir: asistan önce göremediğini ya da veremediğini söyler, varsa genel bilgiyi alıntıyla ekler. Bu üç grupta "alıntı 0" yerine yukarıdaki yeni ölçüt geçerlidir.
8. Selamlaşma etiketi için talimata farklı selam, teşekkür ve veda örnekleri eklenir. Örnekler, ölçüm listesindeki mesajlardan ("Çok makbule geçti" ve listede olmayan diğer 4 selam) alınmaz. 18/18 ölçütü aynı kalır.
9. Golden 100 soru ve 55 negatif mesaj yeni kodla yeniden ölçülür; golden sınırı yine en fazla 2.
10. Giriş cümlesinin içine ya da hemen arkasına eklenen numara sistemce silinir, kayda ve son cevaba girmez. Bundan sonra üç gruptaki 15 mesaj yeniden ölçülür.

Hazır metinler:
- Selam: "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılıkla ilgili sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplayabilirim. Ne sormak istersin?"
- Teşekkür: "Rica ederim. Kart, hesap, kredi, transfer ya da dijital bankacılıkla ilgili başka bir sorun olursa yazabilirsin."
- Veda: "Görüşmek üzere. Kart, hesap, kredi, transfer ya da dijital bankacılıkla ilgili bir sorun olursa buradayım."

## Benim verdiğim kararlar
- **Selamlaşma nasıl tanınır:**
  - Önce kalıp listesine bakılır. Mesajın tamamı selam, teşekkür, veda kalıplarından (ve "dostum", "kanka" gibi hitaplardan) oluşmalıdır: "slm", "sa", "selamün aleyküm", "günaydın", "iyi akşamlar", "naber", "tşk", "saol", "eyv", "görüşürüz" ve benzerleri. Harf büyüklüğü, noktalama, emoji, Türkçe karakter yerine düz harf ("tesekkurler") ve harf uzatma ("selaaam") fark etmez. Bu yolda arama yapılmaz, bilgi bankası kapalıyken de karşılık gelir.
  - Listede yoksa mesaj normal yoldan aranır. En uygun parçanın puanı eşiğin altındaysa dil modeline sadece etiket sorulur. Modele parça verilmez, model cevap yazmaz. Etiket selam, teşekkür ya da vedaysa hazır metin, değilse bulamadım cümlesi verilir. Model hata verir, geç kalır (10 sn) ya da anlamsız yazarsa bulamadım verilir.
  - "top-5" ayarında ya da reranker kapalıyken eşik yolu çalışmaz; kalıp listesi ve (düzeltme turu 2) bir sorudan sonra gelen kısa mesaj için arama öncesi etiket çalışır.
  - "Merhaba, kartımı kaybettim" gibi soru içeren mesaj selamlaşma sayılmaz.
- **Selamlaşma turları** sonraki soruların geçmişine girmez.
- **Sistem talimatına üç ek:** kendinden birinci tekil şahısla söz et; hiçbir parçaya dayanmayan cümle yazma; bulamadım cümlesine numara ya da başka bir şey ekleme.
- **Bulamadım cümlesi tek başına kalır:** Model cümleyi başka metinle yazarsa ("Maalesef, bu konuda …") cevap yine sadece cümle olur. Cevap bu cümleyle başlıyorsa cümle tamamlanana kadar ekrana bir şey yazılmaz, sonra tek seferde görünür.
- **Karşılığı olmayan numara silinir:** 5 parça varken cevaptaki "[6]" kayıttan ve ekrandan silinir.
- **Hedefli talimattaki giriş cümleleri (düzeltme turu 1):**
  - Kişisel hesap bilgisi: "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum."
  - Başka bankalar: "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum."
  - Yatırım: "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum."
  Cümleler sabit olduğu için ölçümde otomatik sayılabilir. Rehberde ilgili bilgi yoksa cevap sadece bu cümledir.
- **Ölçüm listesindeki 27 "rehberde olmayan" sorunun 15'i bu üç gruba ayrılır** (bakiye, hesap hareketleri, kişiye özel bilgi: 9; başka bankalar: 3; yatırım: 3). Metinler değişmez.
- **Eşik nasıl seçilir:** Önce bugünkü sistemle 100 rehber sorusunda en uygun parçanın puanı ölçülür. Eşik, bu puanların en düşüğünün 1 puan altına konur; yani eşik tek başına hiçbir rehber sorusunu kesmez. Ayardan değiştirilebilir, boş bırakılırsa kapanır.
- **Eşiğe takılıp bulamadım alan soruda** Önizleme'nin sağ kartında parça listesi yerine "Bu turda modele hiçbir parça gitmedi: en iyi parçanın rerank puanı eşiğin altında kaldı." yazar. Ekranda lab'da olmayan metin sadece bu satır ve "SOHBET" etiketidir.
- **Ölçüm listesi (55 mesaj):**
  - "Rehberde olmayan konular" dosyasındaki 27 örnek soru.
  - 13 kalıp listesinde olan selamlaşma: "Selam", "selamün aleyküm", "eyv", "Naber dostum, ne var ne yok", "selaaam", "tesekkurler" vb.
  - Etiket yolunu denemek için listede bilerek olmayan 5 selamlaşma: "Selamlar herkese", "Keyifler nasıl?", "Çok makbule geçti", "Görüşmek dileğiyle", "Merhabalar efendim, hayırlı işler".
  - 5 konu dışı mesaj, 4 anlamsız mesaj ve 1 takip senaryosu.
- **Ölçümde doğrulanacak:** Listede olmayan 5 selamın puanı gerçekten eşiğin altında mı (önceki ölçümden okunur)? Eşiğin üstünde kalan varsa raporlanır, ne yapılacağı sana sorulur.

## Çalışacak roller
planner, backend-coder (ölçümler dahil), frontend-coder, tester, qa. Atlanan: devops (Docker ve kurulum değişmez, yeni model yok).
Düzeltme turu 1'de çalışacaklar: backend-coder (talimatlar, denetim, yeniden ölçüm), tester (yeni testler), qa. frontend-coder etkilenmez (ekran ve cevap akışının alanları değişmez).
Düzeltme turu 2'de çalışacaklar: backend-coder (yeni sıra, etiket örnekleri, yeniden ölçüm), tester, qa. frontend-coder etkilenmez (bu yolla gelen selamlaşma ekranda kalıp listesindekiyle aynı görünür).

## Düzeltme turu 2
**Ne oldu:** qa, cevaplı bir sorudan sonra listede olmayan teşekkürü denedi ("Çok işime yaradı sağ olun", "Allah razı olsun", "Harikasınız, eyvallah"…). 6 denemenin 5'inde asistan "… memnuniyetle yardımcı oldum [1]" gibi uydurma alıntı yazdı. Sebep: sistem teşekkürü önceki soruyla birleştirip arıyor; rehberde puanı yüksek parça bulunduğu için etiket hiç sorulmuyor.

**Kullanıcının kararı: "önce etiket".** Konuşmada daha önce cevaplanmış bir soru varsa ve yeni mesaj kısaysa, aramadan önce dil modeline sadece "selam mı, teşekkür mü, veda mı, başka mı" diye sorulur.
- Selam, teşekkür ya da vedaysa hazır metin verilir; arama yapılmaz.
- "Başka" ise mesaj bugünkü gibi aranır ve cevaplanır.
- Konuşmanın ilk mesajı (rehber soruları) bu adımdan geçmez.

**"Kısa" ne demek?** En fazla 10 kelime ve içinde soru işareti yok. İlk tanım 6 kelimeydi; ölçümde "Her şey için sağ olun, iyi akşamlar" (7 kelime) bu yüzden kaçtı. Senin kararınla sınır 10 oldu. "Çok işime yaradı sağ olun" kısadır; "Peki Platinum?" soru işareti taşıdığı için bu adıma girmez, doğrudan aranır.

**Etiket örnekleri değişti:** Talimattaki "Çok işime yaradı", "Allah razı olsun", "Minnettarım, sağ olasın" örnekleri, qa'nın denediği mesajlara çok benzediği için çıkarıldı. Yerine başka teşekkür örnekleri ve kısa takip sorusu örnekleri ("Ya bireysel kredide" → başka) kondu. Ölçülen hiçbir mesaj örneklerde yok; bunu bir test kontrol eder.

**Bittiğini nasıl anlayacaksın (eklenenler):**
- Önizleme'de "Gold kart aidatı ne kadar?" sonra "Allah razı olsun" yazarsın: hazır teşekkür metni gelir, numara ve kaynak düğmesi yoktur.
- Aynı soruyu "ya kredi kartında" ile sürdürürsün: hazır metin değil, soruya cevap gelir.
- Yeniden ölçüm raporunda:
  - Bir sorudan sonra gelen 10 selam / teşekkür / veda mesajının 10'u hazır metin almış, hiçbirinde alıntı yok. Bunlar qa'nın 6 mesajı ve 4 yeni mesajdır: "Merhaba tekrar", "Her şey için sağ olun, iyi akşamlar", "Tamamdır, kolay gelsin", "Size de iyi çalışmalar".
  - 8 kısa gerçek takip sorusundan hiçbiri hazır metin almamış. Bunlar: "Peki Platinum?", "ya kredi kartında", "Ya yurt dışında", "Yenisi kaç günde gelir", "Peki ücreti var mı", "Mobil uygulamadan da olur", "Ek kart için de aynı mı", "Hangi kartlarda yok".
  - 7-10 kelimelik, soru işaretsiz 7 gerçek takip sorusundan da hiçbiri hazır metin almamış (ör. "Peki bunu mobil uygulamadan da ödeyebilir miyim acaba").
  - Sınır değişikliğinden sonra sadece iki mesajlı senaryolar yeniden ölçülür. Rehber soruları ve tek mesajlı ölçümler bu adımdan hiç geçmediği için önceki ölçüm geçerli kalır.
  - Önceki bütün ölçütler aynı kalır: 100 rehber sorusu, 55 mesaj, 18/18 selam.

Yeniden ölçümden önce sistem sıfırlanır (kullanıcı kararı); qa uçtan uca testleri ölçümden sonra koşar.

## Kararını bekleyen sorular
Yok.

## Riskler
- Eşiği geçen konu dışı bir mesajda model "bulamadım" demeden uydurma, numaralı bir cevap yazabilir; bu otomatik yakalanmaz. Ölçümde çıkarsa raporlanır, eşiği yükseltmek senin kararın olur.
- İlk mesaj olarak gelen, listede olmayan bir selamın puanı eşiği geçerse etiket sorulmaz, cevap modeline gider. (Bir sorudan sonra gelen teşekkür için risk düzeltme turu 2'de kapatıldı.)
- Eşiğin altında kalan her mesajda, ve bir sorudan sonra gelen her kısa mesajda (gerçek takip soruları dahil, ör. "ya kredi kartında") etiket sorusu yaklaşık 1-2 saniye ek süre getirir.
- Kısa bir gerçek takip sorusu yanlışlıkla selam etiketi alırsa soruya cevap yerine hazır metin gelir. Ölçümde 15 takip sorusuyla (8 kısa, 7 tanesi 7-10 kelime) kontrol edilir; sınır 0.
- Bulamadım'ın başka kelimelerle yazılmış hâlinde numara, cevap yazılırken bir an görünebilir. Cevap bitince sabit cümleye döner ve kayda numarasız girer. Bunu yazılırken önlemek mümkün değil, çünkü cümlenin nasıl biteceği önceden bilinmez.
- Hedefli talimat rehber sorularını da etkileyebilir; ör. "Kart limiti nasıl belirlenir?" cevabı gereksiz yere "göremiyorum" ile başlayabilir. Sınır en fazla 2 rehber cevabıdır; aşılırsa iş yeniden ele alınır.
- Alıntıların parçaya gerçekten dayanıp dayanmadığı elle okunarak kontrol edilir; otomatik değildir.

ONAY: VERİLDİ
