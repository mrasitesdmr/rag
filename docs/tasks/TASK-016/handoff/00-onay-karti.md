TASK: TASK-016
# Onay kartı: Değerlendirme (golden set, recall@5, MRR)

**Senden 5 karar bekliyorum (aşağıda, "Kararını bekleyen sorular"). Soru 1 cevaplanmadan plan yazılmaz. Her soru tek başına okunabilir.**

## Ne yapılacak
- Değerlendirme ekranında ölçüm gerçekten yapılır. "Ölçümü çalıştır"a basınca golden set'teki her soru için bilgi bankasında gerçek arama (ve rerank) yapılır. Doğru parçanın modele giden 5 parçaya girip girmediğine bakılır. Dil modeli çağrılmaz.
- Ekran lab'daki gibidir: recall@5, MRR, Kaçan ve Retrieval kutuları, "Çalıştırmalar" tablosu, "Kaçanlar ve sırası değişenler" tablosu.
- Golden set, rehber 2026.10'a göre yazılmış ve her birinde cevabın geçtiği yer işaretli sorulardan oluşur. Dosya olarak durur ve sen yükleyince sisteme girer. Kendiliğinden yüklenmez.
- Düğme etkin stratejiyi gösterir ("· rerank 20→5" ya da "· top-5"). Sadece LLM modunda düğme kapalıdır ve lab'daki uyarı görünür. Ölçüm sürerken de kapalıdır. Sayfa yenilense de süren ölçüm görünür. Aynı anda ikinci bir ölçüm başlatılamaz.
- Her ölçüm kaydedilir: golden set sürümü, strateji, rehber sürümü, soru sayısı, her sorunun aramadaki ve prompt'taki sırası, metrikler ve süre.
- Önizleme'de ve Konuşmalar'da, soru golden set'te varsa cevabın geçtiği parça işaretlenir (sarı / "cevap burada"). Ayrıntısı soru 5'te.

## Bittiğini nasıl anlayacaksın
1. Golden set'in tam listesini görür ve onaylarsın. Listede her soru için soru metni, kategori, sayfa ve cevabın geçtiği yer yazar.
2. Golden set yüklenmemişken Değerlendirme'yi açarsın. Ekran, soru 3'teki seçimine göre davranır.
3. Rehber 2026.10'u ve golden set'i yükleyip ölçümü çalıştırırsın. Ölçüm sürerken ilerleme sayacı artar. Bitince kutular gerçek recall@5 (x/n soru) ve MRR değerini gösterir; recall@5 0,95 ve üzeriyse yeşil, altındaysa kırmızıdır. Kaçan sorular tabloda "yok" diye listelenir.
4. Ölçüm sürerken sayfayı yenilersin: ilerleme kaldığı yerden görünür. İkinci bir sekmeden başlatmayı denersin: reddedilir.
5. Stratejiyi top-5'e alıp sistemi yeniden başlatır ve ölçümü tekrarlarsın. İki çalıştırma "Çalıştırmalar" tablosunda yan yana görünür.
6. "Kaçanlar" tablosunda bir satıra basarsın. Soru Önizleme'de sorulur ve doğru parça sarı görünür.
7. Verileri sıfırlar, rehberi yeniden yükler ve ölçümü tekrarlarsın. Sonuç aynı çıkar; işaretler bozulmaz (soru 1'deki karara bağlı).
8. Sadece LLM modunda düğme kapalıdır ve uyarı görünür.
9. Raporda iki stratejinin gerçek sonuçları yazar. Metrik hesabı elle hesaplanmış örneklerle test edilmiştir. Derlemeler ve testler geçer.

## Yapılmayacaklar
- Cevap kalitesinin ölçülmesi (doğru mu, uyduruyor mu).
- Golden set'i ekrandan görmek ya da düzenlemek.
- Stratejiyi ekrandan değiştirmek. Strateji bugünkü gibi ayarla değişir ve sistem yeniden başlatılır.
- Rehberde cevabı olmayan sorular golden set'e girmez. "Bulamadım" cevabını qa ayrıca dener.
- Rehber 2026.11 için ayrı golden set. Yeni sürüm yükleme projeden çıkarıldığı için yok.
- Docker ve model değişikliği.

## Benim verdiğim kararlar
1. **Golden set'i lead hazırlar**, tıpkı rehberi yazdığı gibi. Rehber 2026.10'u okuyarak 100 soru yazar. Sorular rehberin 1-11. bölümlerine sayfa sayısına göre dağıtılır. Kategori, sorunun geçtiği bölümün adıdır (Kartlar, Hesaplar…). Tam listeyi sana sunar ve onayın rapora yazılır. qa, rastgele sorularda işaretin PDF'teki yerini kontrol eder.
2. **Sürüm adı lab'daki gibi "rehber-golden-v1" olur.** Ad, dosyanın içinde yazar. İçerik değişince ad da değişir (v2). Yüklü setle aynı ada sahip ama içeriği farklı bir dosya reddedilir; böylece aynı ad hep aynı soruları gösterir.
3. **Reranker kapalıyken** o soru vektör sırasıyla ölçülür, Önizleme'de olduğu gibi. Retrieval kutusunda "reranker DOWN" ve bunun kaç soruda olduğu yazar.
4. **Bilgi bankası ya da embedding modeli ölçüm sırasında cevap vermezse** ölçüm "başarısız" biter. Yarım sonuç metrik olarak gösterilmez; ekranda sebep yazar. Servis ölçüm sırasında yeniden başlarsa yarım kalan ölçüm de "başarısız" sayılır ve düğme yeniden açılır.
5. **İlerleme:** Ölçüm sürerken düğme "Ölçülüyor · 37/100" yazar. Lab'da bu yoktu, çünkü lab'da ölçüm kayıttan anında bitiyordu.
6. **"Kaçanlar ve sırası değişenler" tablosu lab'daki kuralla dolar.** Doğru parçası prompt'ta olmayan ya da prompt'taki yeri arama sırasından farklı olan sorular, golden set sırasıyla, en fazla 24 satır listelenir. Doğru parça aramanın ilk 20 parçasında yoksa "Vector #" sütununda "20+" yazar. Top-5 ölçümünde de arama sırası için 20 parça getirilir ve ilk 5'i kullanılır; sonuç aynıdır.
7. **Kaçanlar satırına basınca Önizleme'de yeni bir konuşma açılır ve soru orada sorulur.** Lab soruyu açık konuşmada soruyordu. Yeni konuşmada açılmazsa soru, önceki soruların takip sorusu gibi yeniden yazılabilirdi.

## Çalışacak roller
1. planner.
2. lead (ana oturum): golden set dosyasını yazar ve sana sunar.
3. backend-coder: asistan servisi (golden set'in yüklenmesi, ölçümün çalışması ve kaydı, metrik hesabı, Önizleme/Konuşmalar işareti). Bilgi bankası servisi soru 1'e bağlı: doğru parçayı metinle bulmak gerekirse yeni bir arama adresi eklenir.
4. frontend-coder: Değerlendirme ekranı, Önizleme'deki sarı işaret, Konuşmalar'daki "cevap burada" etiketi.
5. tester: otomatik testler. Metrik hesabı elle hesaplanmış değerlerle test edilir.
6. qa: sistemi kurar, iki stratejiyle gerçek ölçüm yapar, sıfırlayıp yeniden yükleme senaryosunu dener, görünümü lab'la karşılaştırır.

Atlanan: devops. Docker, ayarlar ve modeller değişmiyor; strateji bugünkü ayarla değişir.

## Kararını bekleyen sorular

1. Golden set'te "cevabın geçtiği parça" neyle işaretlensin?
   Neden önemli: Belge yeniden yüklenince ya da parçalama ayarı değişince işaretin geçerli kalıp kalmayacağını bu seçim belirler (TASK'taki 3. örnek senaryo).
   Bugünkü durum: Üründe belge silme yok, yani "silip yeniden yüklemek" verileri sıfırlayıp belgeyi yeniden yüklemek demek. Sıfırlamadan sonra belgeler aynı sırayla yüklenirse parça numaraları tesadüfen aynı çıkabilir. Başka bir sırayla yüklenirse, başka bir belge önce girerse ya da parçalama ayarı değişirse (TASK-024'teki gibi) numaralar kayar.
   Gerçek projelerdeki yaygın uygulama: Arama kıyas setlerinde (BEIR, MS MARCO gibi) metinler hiç değişmediği için parçalar numarayla işaretlenir. Ürün içi RAG değerlendirmesinde (Ragas, LlamaIndex gibi araçlar) ise parçalama sık değiştiği için golden set parçanın numarasını değil metnini tutar (referans metin, ya da belge ve sayfa ile cümle). Eşleşme metinle yapılır. Numara ancak parçalama sabitse ve numara parçanın içeriğinden türetiliyorsa kullanılır.
   Seçenekler:
   A) Parça numarası (lab gibi).
      Artı: En basiti; eşleşme kesin.
      Eksi: Yeniden yüklemede ya da parçalama değişince işaretler bozulur ve 100 soru elle yeniden işaretlenir. 3. örnek senaryoyu karşılamaz.
   B) Parça numarası, ama içeriği aynı kalan parça aynı numarayı korur.
      Artı: Golden set bugünkü biçimiyle kalır.
      Eksi: Veriler sıfırlanınca eski numaraların kaydı da silinir. Bu yüzden numaranın parçanın metninden türetilmesi gerekir; bilgi bankasının numara düzeni baştan değişir. Parçada tek bir harf ya da parçalama ayarı değişince numara yine kayar.
   C) **(Önerilen)** Sayfa + cevabın geçtiği cümle (rehberden birebir kopya). Doğru parça, bu cümleyi içeren parçadır. Sayfa sadece hazırlayanın ve okuyanın kontrolü içindir. Büyük/küçük harf ve boşluk farkları yok sayılır. Cümle örtüşme yüzünden iki parçada geçiyorsa ikisi de doğru sayılır.
      Artı: Yeniden yükleme, yükleme sırası ve parçalama değişikliği işareti bozmaz. İnsan okuyup PDF'te doğrulayabilir.
      Eksi: Rehberde cümle değişirse (ör. 1.450 TL → 1.650 TL) o soru eşleşmez ve golden set'te güncellenmesi gerekir. Bu durum sessizce kaçan sayılmaz, "ölçülemedi" diye görünür (soru 3). Cümleyi bulmak için bilgi bankası servisine yeni bir arama adresi eklenir.
   D) Beklenen cevap olgusu (ör. "1.650 TL").
      Artı: Yazması en kolayı.
      Eksi: Kısa olgular birçok parçada geçer ("1.650 TL" kart tablosunda, sık sorulanlarda ve ücret tarifesinde, "3 iş günü" onlarca yerde). Yanlış parça da doğru sayılır ve recall olduğundan yüksek çıkar.
   E) Birleşim: eşleşme C'deki gibi cümleyle yapılır; olgu da her soruya yazılır ama bu task'ta kullanılmaz (ileride cevap doğruluğu ölçülürse işe yarar).
      Artı: İleriye hazırlık.
      Eksi: Bu task'ta karşılığı olmayan ek iş.
   Önerim: C. Gerçek projelerdeki yaygın uygulamaya uyuyor ve 3. örnek senaryoyu gerçekten karşılıyor. Rehber değişince hangi sorunun güncellenmesi gerektiği de açıkça görünüyor.

2. Golden set nasıl sisteme girsin, ve kendi yüklediğin belgeler için de golden set hazırlayabilsin misin?
   Neden önemli: Ürüne hiçbir veri kendiliğinden girmiyor (TASK-027). Golden set'in hangi belgeye ait olduğu ve başka belgelerin ölçülüp ölçülemeyeceği bu seçime bağlı.
   Seçenekler:
   A) **(Önerilen)** Golden set bir dosyadır ve bir adrese gönderilerek yüklenir, Önizleme'deki soru önerileri gibi (TASK-012). Ekranı yoktur ve kendiliğinden yüklenmez. Rehber için hazır dosya rehberin yanında durur. Kendi belgen için aynı biçimde bir dosya yazıp yüklersin. Bir anda tek set geçerlidir; yenisi yüklenince eskisinin yerini alır. Bir set birden fazla belgenin sorularını içerebilir.
      Artı: "Kendiliğinden yüklenmez" kuralına ve soru önerilerindeki yönteme uyar. Her belge ölçülebilir.
      Eksi: Dosya biçimini bilmek gerekir. Ekrandan düzenleme yok (TASK'ta kapsam dışı).
   B) Rehberin seti repoda durur ve servis açılışta kendiliğinden okur. Başka set yüklenemez.
      Artı: Kurulumda tek adım az.
      Eksi: "Açılışta kendiliğinden hiçbir şey yüklenmez" kuralına aykırı. Kendi belgelerin ölçülemez.
   C) A'ya ek olarak aynı anda birden fazla set (her belge için ayrı) ve ekranda set seçimi.
      Artı: Belgeler ayrı ayrı ölçülür.
      Eksi: Lab'da yok; ekranın düzeni değişir ve iş büyür.
   Önerim: A. Kurallarla uyumlu, en az işle her belgeyi ölçmeye izin veriyor.

3. Golden set yüklenmemişken ya da belgesi bilgi bankasında yokken Değerlendirme ekranı ne yapsın?
   Neden önemli: Bilgi bankası boş açılıyor. Golden set'in belgesi yüklenmemiş ya da farklı bir sürümü yüklenmiş olabilir. Bu durumda recall'un neyi gösterdiği bu seçime bağlı.
   Seçenekler:
   A) **(Önerilen)**
      - Golden set yüklenmemişse düğme kapalıdır ve "Golden set yüklenmemiş." bilgisi görünür.
      - Set yüklü ama hiçbir sorunun doğru parçası bilgi bankasında bulunamıyorsa ölçüm başlamaz. "Golden set'in belgesi bilgi bankasında yok. Önce belgeyi yükle." yazar.
      - Sadece bir kısmı bulunamıyorsa ölçüm bulunan sorularla yapılır. recall@5 kutusunun alt satırında "x/n soru · k soru ölçülemedi" yazar; ölçülemeyen sorular metriklere girmez.
      Artı: Belge eksikliği retrieval hatasıyla karışmaz; neyin ölçüldüğü açıkça görünür.
      Eksi: Farklı sayıda soruyla yapılmış iki ölçüm yan yana durabilir.
   B) Doğru parçası bulunamayan sorular "kaçan" sayılır.
      Artı: En basiti.
      Eksi: Belge eksikliği retrieval hatası gibi görünür ve recall anlamsız biçimde düşer.
   C) Tek bir sorunun bile doğru parçası bulunamazsa ölçüm başlamaz.
      Artı: En katı kural; her ölçüm tam setle yapılır.
      Eksi: Rehber değişince golden set güncellenene kadar hiç ölçüm yapılamaz.
   Önerim: A. Kısmi uyuşmazlığı gizlemeden ölçmeye devam ediyor. Hiç uyuşma yoksa da açıkça durduruyor.

4. Önceki ölçümler ekranda nasıl görünsün?
   Neden önemli: TASK önceki ölçümlerin karşılaştırma için saklanmasını istiyor. Not: Lab'da kutular ve "Kaçanlar" tablosu sadece son ölçümü gösteriyor, ama altında bütün ölçümlerin yer aldığı bir "Çalıştırmalar" tablosu da var (sütunlar: Çalıştırma, Retrieval, Rehber, recall@5, MRR, Saat).
   Seçenekler:
   A) **(Önerilen)** Lab gibi: kutular ve "Kaçanlar" tablosu son ölçümü gösterir. "Çalıştırmalar" tablosu kayıtlı bütün ölçümleri en yeni üstte, sayfalı (20'şer) listeler. Saat sütununda bugünkü ölçüm için sadece saat, eski ölçüm için tarih ve saat yazar (Konuşmalar'daki gibi). Satırlara basılmaz.
      Artı: Lab'la aynı. İki stratejinin sonuçları yan yana okunur.
      Eksi: Eski bir ölçümün kaçan soruları ekranda görülemez; sadece kayıtta durur.
   B) A'ya ek olarak "Çalıştırmalar" satırına basınca kutular ve "Kaçanlar" tablosu o ölçümü gösterir.
      Artı: Eski ölçümün kaçanları da incelenebilir.
      Eksi: Lab'da yok; hangi ölçümün gösterildiği ayrıca belirtilmeli.
   C) A'ya ek olarak iki ölçüm seçilip soru soru karşılaştırılır (hangi sorular kurtuldu, hangileri kaçtı).
      Artı: Rerank'in kazancı soru düzeyinde görünür.
      Eksi: Lab'da olmayan yeni bir ekran parçası; iş belirgin büyür.
   Önerim: A. Lab'la aynı ve TASK'taki "iki çalıştırma karşılaştırılabilir" senaryosunu karşılıyor.

5. Önizleme'de ya da Konuşmalar'da sorulan bir soru golden set'teki bir soruyla nasıl eşleşsin?
   Neden önemli: Eşleşen soruda doğru parça sarı görünür ve "cevap burada" etiketini alır. TASK-014'te "bulamadım" turları şimdilik hep BİLMİYOR çipiyle gösteriliyordu; KAÇIRDI ve ÜRETİM HATASI çipleri golden set'e bırakılmıştı. Eşleşen bir soruda asistan "bulamadım" derse tur, lab'daki gibi şu çipi alır: doğru parça modele gitmediyse KAÇIRDI, gittiyse ÜRETİM HATASI. TASK-014'teki gibi bu durum saklanmaz, her açılışta kayıttan hesaplanır; golden set değişirse eski turların işareti de değişir.
   Seçenekler:
   A) **(Önerilen)** Tam metin: büyük/küçük harf, noktalama ve fazla boşluk farkı yok sayılır, Türkçe harfler doğru eşlenir (soru önerilerindeki kuralın aynısı). Başka her fark eşleşmez. Müşterinin yazdığı soru karşılaştırılır. Eşleşen "bulamadım" turları KAÇIRDI ya da ÜRETİM HATASI olur.
      Artı: Yanlış eşleşme olmaz. Öneri listesinden ya da "Kaçanlar" tablosundan sorulan sorular eşleşir.
      Eksi: Aynı anlamın farklı yazımı eşleşmez ("Gold aidatı ne kadar?" ile "Raşit Gold yıllık üyelik ücreti ne kadar?").
   B) Anlam benzerliği: soru, golden set sorularıyla embedding modeliyle karşılaştırılır ve belli bir eşiğin üstündeyse eşleşir. Çipler A'daki gibi.
      Artı: Farklı yazımları da yakalar.
      Eksi: Eşik keyfidir. "Gold aidatı" ile "Platinum aidatı" birbirine çok yakın çıkar; bu durumda yanlış parça sarı görünür ve yanlış KAÇIRDI çipi çıkar. Her tur için ek model çağrısı gerekir.
   C) Tam metin (A gibi), ama sadece sarı işaret ve "cevap burada" etiketi gösterilir. "Bulamadım" turları BİLMİYOR kalır.
      Artı: Tur çipleri bugünkü gibi kalır.
      Eksi: TASK-014'te bu task'a bırakılan KAÇIRDI ve ÜRETİM HATASI ayrımı yine gelmez.
   Önerim: A. Kesin olduğu için yanlış çip üretmez ve TASK-014'teki "durum kayıttan hesaplanır" kararıyla aynı mantıkta.

## Riskler
- Gerçek sistemde recall@5 0,95'in altında çıkabilir. Kırmızı görünmesi hata değil, ölçümün sonucudur; eşik değişmez.
- Soru 1'de C seçilirse cümle, PDF'ten çıkarılan metinle karşılaştırılır. Metin çıkarılırken oluşan farklar (satır sonu, özel karakter) eşleşmeyi bozabilir. Böyle bir soru sessizce kaçan sayılmaz, "ölçülemedi" diye görünür.
- Ölçüm sürerken Önizleme'deki cevaplar yavaşlar, çünkü ikisi aynı embedding modelini ve reranker'ı kullanır. 100 soruluk bir ölçüm birkaç dakika sürebilir.

## Kullanıcının cevapları (2026-11-02)
1. C: sayfa + cevabın geçtiği cümle.
2. A: golden set yüklenen tek dosya, kendiliğinden yüklenmez.
3. A: set yoksa düğme kapalı; hiç eşleşme yoksa ölçüm başlamaz; kısmi eşleşmede "k soru ölçülemedi".
4. A: lab gibi, "Çalıştırmalar" sayfalı (20'şer).
5. A: tam metin eşleşme; eşleşen "bulamadım" turları KAÇIRDI / ÜRETİM HATASI.
"Benim verdiğim kararlar" bölümü olduğu gibi onaylandı.

Golden set: `docs/rehber/golden/rehber-golden-v1.json` (100 soru) kullanıcıya tam liste olarak sunuldu ve 2026-11-02'de onaylandı. Gerçek sistemde `unmatchedCount` 0 (backend-coder ölçümü).

ONAY: VERİLDİ
