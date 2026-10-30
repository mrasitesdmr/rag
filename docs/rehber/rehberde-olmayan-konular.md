# Rehberde olmayan konular

Raşit Bankası Ürün ve Hizmet Rehberi'nin iki sürümünde (2026.10 ve 2026.11) de bilerek yer almayan konular. Bu konulardaki müşteri sorularına asistanın tek doğru cevabı şudur:

> Bu konuda rehberimizde bilgi bulamadım.

Bu liste, ileride golden set'teki (TASK-016) "cevabı olmayan soru" örneklerinin kaynağıdır. Konuların rehbere dolaylı olarak da girmemesi için her konunun yasak kelimeleri vardır. `dogrula.sh` (D10) bu kelimeleri iki PDF'in metninde arar ve bir tanesi bile bulunursa kontrol KALDI olur.

**Arama kuralı:** Kelime Türkçe küçük harfe çevrilir ve tam kelime olarak aranır. Kelimenin hemen önünde ve arkasında harf ya da rakam olmamalıdır; kesme işaretiyle eklenen ekler (`Bitcoin'i`) yine eşleşir.

**Rehberde bulunan ama bu listeye girmeyen konular:** Başka bankaya EFT, FAST ve SWIFT; başka banka ATM'lerinin kullanımı ve ücretleri; kredilere bağlı zorunlu sigortalar (DASK, konut sigortası, kasko) ve kredi hayat sigortası; genel kavram olarak "eksi bakiye". Bunlar Raşit Bankası'nın kendi hizmetleridir ve rehberde anlatılır.

## 1. Kişisel bakiye
Müşterinin kendi hesabındaki para. Rehber kişisel bilgi göstermez ve bu bilginin nereden görüleceğini de anlatmaz.
- **Örnek sorular:** "Hesabımdaki bakiyeyi söyler misin?", "Vadesiz hesabımda ne kadar param var?", "Kumbara hesabımda kaç lira birikti?"
- **Yasak kelimeler:** `bakiyeniz`, `bakiyenizi`, `bakiye sorgulama`, `bakiye sorgulamak`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 2. Hesap hareketleri
Müşterinin geçmiş işlemleri, hesabına gelen ya da giden belirli bir para.
- **Örnek sorular:** "Dün hesabıma gelen para ne kadardı?", "Geçen ay kime para gönderdim?", "Son 5 işlemimi gösterir misin?"
- **Yasak kelimeler:** `hesap hareketleriniz`, `hesap hareketlerinizi`, `hesap dökümü`, `hesap dökümünüz`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 3. Kripto para
Kripto para alım satımı, saklanması ya da kripto paraya transfer.
- **Örnek sorular:** "Bitcoin alıp satabilir miyim?", "Kripto para hesabı açabilir miyim?", "Kripto borsasına para gönderebilir miyim?"
- **Yasak kelimeler:** `kripto`, `kriptopara`, `kripto para`, `bitcoin`, `ethereum`, `blokzincir`, `blockchain`, `stablecoin`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 4. Başka bankaların ürünleri
Başka bir bankanın kredisi, kartı, faizi ya da kampanyası. Raşit Bankası'ndan başka bankaya transfer ve başka banka ATM'lerinin kullanımı bu konuya girmez.
- **Örnek sorular:** "X Bankası'nın konut kredisi faizi kaç?", "Başka bankanın kartı sizinkinden daha mı avantajlı?", "Diğer bankalar EFT için ne kadar ücret alıyor?"
- **Yasak kelimeler:** `diğer bankaların faiz`, `başka bankaların faiz`, `diğer bankaların ürün`, `başka bankaların ürün`, `rakip banka`
- **Not:** Gerçek banka adlarının hiçbirinin geçmemesi ayrıca D12'de kontrol edilir (`dogrulama/kontroller.txt`, `[GERCEK_BANKA]`).
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 5. Kişiye özel diğer bilgiler
Müşterinin kendi limiti, borcu, başvurusunun durumu ya da kendisine uygulanan oran.
- **Örnek sorular:** "Kredi kartı limitim ne kadar?", "Başvurum ne durumda?", "Bu ayki kart borcum ne kadar?"
- **Yasak kelimeler:** `güncel limitiniz`, `güncel limitinizi`, `başvurunuzun durumu`, `başvuru durumunuz`, `başvuru durumunuzu`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 6. Ticari ve kurumsal bankacılık
İşletmelere, şirketlere ve esnafa yönelik ürünler.
- **Örnek sorular:** "İşletme kredisi faiz oranınız ne kadar?", "Şirketim için POS cihazı alabilir miyim?", "Çek karnesi nasıl alırım?"
- **Yasak kelimeler:** `işletme kredisi`, `ticari kredi`, `ticari müşteri`, `kurumsal bankacılık`, `kobi`, `pos cihazı`, `çek karnesi`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 7. Bankanın kurumsal bilgileri
Yöneticiler, ortaklık yapısı, kuruluş tarihi, mali tablolar, genel müdürlük.
- **Örnek sorular:** "Raşit Bankası'nın genel müdürü kim?", "Bankanız ne zaman kuruldu?", "Bankanın sahibi kim?"
- **Yasak kelimeler:** `genel müdür`, `genel müdürü`, `genel müdürlük`, `yönetim kurulu`, `kuruluş yılı`, `hissedar`, `ortaklık yapısı`, `bilanço`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 8. Piyasa tahmini ve yatırım tavsiyesi
Kur, faiz, altın ya da hisse fiyatlarının gelecekteki yönü; hangi üründen alınması gerektiği.
- **Örnek sorular:** "Dolar yarın yükselir mi?", "Şimdi altın almalı mıyım?", "Hangi fonu almamı önerirsiniz?"
- **Yasak kelimeler:** `yatırım tavsiyesi`, `yatırım danışmanlığı`, `piyasa tahmini`, `kur tahmini`, `fiyat tahmini`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.

## 9. Bireysel emeklilik (BES) ve sigorta ürünlerinin ayrıntıları
Bireysel emeklilik sistemi, devlet katkısı, sağlık ve seyahat sigortası poliçeleri, hasar ve tazminat süreçleri. Kredilere bağlı zorunlu sigortalar ve kredi hayat sigortası 4.8'de anlatılır; bu konuya girmez.
- **Örnek sorular:** "BES'te devlet katkısı yüzde kaç?", "BES fon dağılımımı kaç kez değiştirebilirim?", "Hasar bildirimini bankaya mı yapıyorum?"
- **Yasak kelimeler:** `bes`, `bireysel emeklilik`, `devlet katkısı`, `otomatik katılım`, `emeklilik şirketi`, `tamamlayıcı sağlık`, `hasar bildirimi`, `hasar ihbar`
- **Beklenen cevap:** Bu konuda rehberimizde bilgi bulamadım.
