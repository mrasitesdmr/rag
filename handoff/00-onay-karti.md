TASK: TASK-032
# Onay kartı: Konu dışı ve kısa mesajlarda yanlış cevabın düzeltilmesi

## Ne yapılacak
- Selamlaşma, teşekkür ve vedalaşma için tutulan kelime listesi kaldırılır. Bunun yerine her kısa mesaj, konuşmanın ilk mesajı da olsa, rehberde aranmadan önce dil modeline sadece "selam mı, teşekkür mü, veda mı, başka mı" diye sorulur. Selam, teşekkür ya da vedaysa bugünkü hazır metin gelir; numara ve kaynak düğmesi olmaz. Kısa mesajlara yaklaşık 1 saniye eklenir.
- "Nasılsın?", "Merhaba, iyi misin?" gibi soru işaretli selamlar da yakalanır.
- "Hesabına özel bilgileri … göremiyorum", "Başka bankaların … veremiyorum", "Yatırım tavsiyesi … veremiyorum" cümleleri sadece mesaj gerçekten o konudaysa kullanılır. Hava durumu, şiir, sohbet gibi diğer her konuda cevap "Bu konuda rehberimizde bilgi bulamadım." olur.
- Kullanıcı kendi limitini, bakiyesini ya da borcunu sorarsa giriş cümlesinden sonra rehberdeki genel bilgi (nereden görülür, nasıl değiştirilir) numarasıyla eklenir.
- Takip sorusu aranırken asistanın önceki cevabındaki metin (selamlama, kendini tanıtma) arama sorgusuna karışmaz. Önceki konudan bağımsız yeni bir soru olduğu gibi aranır.
- Rehberle ilgisi olmayan mesajları ayıran puan sınırı ölçülerek yeniden seçilir. Kesin değeri ölçüm sonucunu gördükten sonra sen onaylarsın.

## Bittiğini nasıl anlayacaksın
- Önizleme'de "Selam", "Günaydın", "Nber", "Nasılsın?", "Teşekkürler", "Görüşürüz" yazarsın: hazır karşılık gelir, numara ve kaynak düğmesi yoktur.
- "Ahmet" yazarsın: numarasız bir cevap gelir (hangisi olacağı soru 4'e bağlı).
- "Selam" sonra "Hava durumunu verir misin?" yazarsın: "Bu konuda rehberimizde bilgi bulamadım." gelir, yatırım cümlesi gelmez.
- "Kredi limitim ne kadar" yazarsın: "Hesabına özel bilgileri … göremiyorum." cümlesinden sonra rehberin kart limiti bölümüne (2.6) dayanan bilgi ve numarası gelir.
- "Ek kart nasıl alabilirim" sonra "Peki ücretli mi" yazarsın: bugünkü gibi doğru cevap ve numara.
- "Bitcoin alabilir miyim?", "jjkbjknlk" yazarsın: bulamadım cümlesi, numara yok.
- Konuşmalar'da selamlaşma turları bugünkü gibi gri "SOHBET" durumuyla görünür. Önizleme ve Konuşmalar'ın görünümü değişmez.
- Ölçüm raporunda:
  - Selam, teşekkür ve veda mesajlarının (37 mesaj) hepsi hazır karşılık almış, hiçbirinde numara yok.
  - Rehberde olmayan konular, konu dışı ve anlamsız mesajların (27 mesaj) hepsi bulamadım almış; hiçbirinde numara ya da giriş cümlesi yok.
  - Hesap bilgisi, başka banka ve yatırım sorularının (15 mesaj) her biri ya bulamadım ya da kendi konusunun giriş cümlesiyle başlıyor; başka konunun giriş cümlesi hiç yok. Numaralı cümleler rehber parçasıyla elle karşılaştırılmış, parçaya dayanmayan numara yok.
  - Kısa ve uzun takip sorularının (16 mesaj) hiçbiri hazır karşılık almamış.
  - 100 rehber sorusunda bulamadım ya da hazır karşılığa düşen soru sayısı soru 5'teki sınırın içinde. Değerlendirme ekranının ölçümü (recall@5, MRR) raporda.

## Yapılmayacaklar
- Dil modelinin ya da reranker'ın değiştirilmesi; yeni model indirilmesi.
- Sadece LLM modu.
- Cevap doğruluğunun modelle ölçülmesi.
- Ekranda değişiklik.
- Değerlendirme ekranındaki ölçüm değişmez; puan sınırı oradaki sonuçları etkilemez.

## Benim verdiğim kararlar
- **Uzun mesajlar bugünkü gibi:** kısa olmayan mesaj önce aranır; en uygun parçanın puanı sınırın altındaysa dil modeline etiket sorulur. Selamsa hazır metin, değilse bulamadım.
- **Kısa bir mesajın etiketi "başka" çıkarsa** mesaj normal yoldan aranıp cevaplanır. Puan sınırın altında kalırsa etiket ikinci kez sorulmaz, bulamadım verilir.
- **Etiket sorusu hata verirse ya da 10 saniyede cevap gelmezse** mesaj "başka" sayılır, normal yoldan devam edilir; ekrana hata çıkmaz.
- **Arama sorusunu yeniden yazan talimat Türkçe olur** (bugün İngilizce). Görev "talimatlar Türkçe" diyor.
- **Asistanın ana talimatı yeniden düzenlenir:** "bulamadım" kuralı giriş cümlelerinden önce gelir ve rehberde olmayan konuları (hava durumu, şiir, sohbet, kripto para) açıkça sayar. Üç giriş cümlesi sadece kendi konusunda kullanılır. "Her cümleye numara yaz" kuralı "parçaya dayanmayan cümleye numara yazma" olarak değişir.
- **Etiket talimatına yeni örnekler eklenir:** kısa banka soruları ("Şifremi unuttum") "başka" sayılır. Örnekler ölçülen mesajlarla ve 100 rehber sorusuyla çakışmaz; bunu bir test kontrol eder.
- **Ölçüm listesi büyür:** soru işaretli 5 selam, "Nber" gibi 3 kısa selam, 3 isim mesajı ("Ahmet", "Ben Ayşe", "Adım Mehmet"), "Hava durumunu verir misin?", "Bitcoin alabilir miyim?", "jjkbjknlk", "Kredi limitim ne kadar", 3 "önce rehber sorusu, sonra bağımsız konu dışı soru" senaryosu ve "Ek kart nasıl alabilirim → Peki ücretli mi".
- **100 rehber sorusunda giriş cümlesiyle başlayan cevap en fazla 2 olabilir.** Bu, önceki görevde senin verdiğin sınırdır; aynen kalır.
- **Eski kayıtlar değişmez:** kelime listesiyle cevaplanmış eski selam turları Konuşmalar'da yine "SOHBET" görünür.

## Çalışacak roller
planner, backend-coder (ölçümler dahil), tester, qa.
Atlanan: frontend-coder (ekran ve cevap akışının alanları değişmez), devops (Docker, kurulum ve modeller değişmez).
Düzeltme turu 1'de frontend-coder da çalışır (aşağıda); devops yine atlanır.

## Kararını bekleyen sorular
1. "Kısa mesaj" nasıl tanımlansın (aramadan önce selam mı diye sorulacak mesajlar)?
   Neden önemli: Tanım genişledikçe daha çok selam yakalanır. Ama daha çok gerçek soru da bu adımdan geçer ve yaklaşık 1 sn bekler.
   Seçenekler:
   A) En fazla 10 kelime, soru işareti fark etmez. Rehber sorularının neredeyse hepsi bu adımdan geçer: her soruya ~1 sn eklenir ve gerçek bir sorunun yanlışlıkla selam sayılma ihtimali artar.
   B) (Önerilen) Soru işareti yoksa en fazla 10 kelime, soru işareti varsa en fazla 5 kelime. "Nasılsın?", "Merhaba, iyi misin?", "Selam, nasıl gidiyor?" yakalanır. "Ücret değişiklikleri bana ne kadar önceden bildiriliyor?" gibi uzun sorular bu adıma girmez.
   C) En fazla 5 kelime, soru işareti fark etmez. Basit ama "Her şey için sağ olun, iyi akşamlar" (7 kelime) yine kaçar; önceki görevde bu yüzden sınır 10'a çıkmıştı.
   Önerim: B. Soru işaretli selamlar genelde kısadır; uzun rehber sorularına gecikme eklenmez.

2. Takip sorusu yeniden yazılırken asistanın önceki cevabı hiç verilsin mi?
   Neden önemli: Bugün asistanın önceki cevabındaki metin ("Merhaba Ahmet, ben … asistanıyım") arama sorgusuna karışıyor. Bu yüzden sonraki konu dışı soru rehberle ilgiliymiş gibi görünüp yanlış cevap alabiliyor.
   Seçenekler:
   A) (Önerilen) Verilmez. Sadece müşterinin önceki soruları verilir. "Ek kart nasıl alabilirim → Peki ücretli mi" yine çalışır. Asistanın cevabında geçen bir şeye dayanan takip ("Peki o uygulamayı nereden indiririm") biraz zayıflayabilir.
   B) Verilir ama kısaltılarak ve numaralar silinerek, talimata da "cevaptaki selamlamayı aramaya koyma" eklenir. Selamlama genelde ilk cümlede olduğu için yine karışabilir.
   C) Bugünkü gibi tam verilir, sadece talimat güçlendirilir. Bu durumda sorun dil modelinin talimata uymasına kalır.
   Önerim: A, çünkü asistanın metni aramaya hiç giremez; rehber takiplerinin neredeyse hepsi müşterinin önceki sorusuyla tamamlanır.

3. Puan sınırı nasıl ölçülüp seçilsin?
   Neden önemli: Sınır yükseldikçe konu dışı mesajlar daha güvenle "bulamadım" alır. Ama rehberde olan, farklı sorulmuş bir soru da yanlışlıkla kesilebilir.
   Yöntem: Sınır kapalıyken 100 rehber sorusu ve bütün ölçüm listesi gönderilir, her mesajın en iyi puanı kaydedilir. Bugünkü bilgiyle rehber soruları 0,65 ile 4,89 arasında puan alıyor; üç konu dışı örnek -8,28, -8,24 ve -7,12 aldı.
   Aday değerler: -3, -2, -1, -0,5 ve 0. Her biri için bir tablo gelir: kaç rehber sorusu kesilir, kaç konu dışı mesaj kesilir, "Kredi limitim ne kadar" gibi hesap soruları kesiliyor mu.
   Seçenekler:
   A) (Önerilen) Kural: rehber sorularındaki en düşük puanın 1 puan altı, yarım puana aşağı yuvarlanmış. Bugünkü bilgiyle -0,5 çıkar. Sınır tek başına hiçbir rehber sorusunu kesmez.
   B) Rehber sorularının en düşüğü ile konu dışı soruların en yükseğinin tam ortası. Konu dışı tarafa daha çok pay kalır, ama iki taraf yakınsa rehber sorusu kesilebilir.
   C) Rehber sorularındaki en düşük puanın yarım puan altı. Bugünkü bilgiyle 0 çıkar. Daha çok konu dışı mesaj kesilir, ama rehber sorularına neredeyse pay kalmaz.
   Önerim: A. Bu yöntemi şimdi onaylarsan ölçümden sonra tablo ve önerilen değer sana gelir; kesin değeri o zaman sen seçersin.

4. "Ahmet" gibi sadece isim yazan ya da kendini tanıtan mesaja ne cevap verilsin?
   Neden önemli: Bugün "Merhaba Ahmet, … [1]" diye uydurma numaralı bir cevap çıkıyor.
   Seçenekler:
   A) (Önerilen) Selamlaşma sayılır, hazır selam metni gelir: "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. … Ne sormak istersin?"
   B) Bulamadım cümlesi gelir. Ama bu kesin olmaz: mesaj rehberde aranır, puanı sınırı geçerse cevabı dil modeli yazar (numarasız olması beklenir).
   Önerim: A. Kendini tanıtan müşteriye selamla karşılık vermek doğal; sonuç da her zaman aynı olur.

5. 100 rehber sorusundan en fazla kaçının "bulamadım" ya da hazır selam karşılığı alması kabul edilsin?
   Neden önemli: Kısa rehber soruları artık önce etiket adımından geçiyor ve puan sınırı yükseliyor; ikisi de rehber sorusunu yanlışlıkla kesebilir.
   Seçenekler:
   A) (Önerilen) En fazla 2. Önceki görevle aynı sınır; dil modelinin küçük oynamalarına pay bırakır.
   B) 0. Hiçbir rehber sorusu düşmemeli; tek bir oynamada iş yeniden ele alınır.
   C) En fazla 5. Daha esnek ama 5 rehber sorusunun cevapsız kalmasına göz yumar.
   Önerim: A.

## Riskler
- Dil modeli kapalıyken "Selam" artık anında karşılık almaz; diğer sorulardaki gibi "dil modeline ulaşılamıyor" mesajı görünür. Bugün kelime listesi anında cevap veriyor.
- Dil modeli kısa bir gerçek soruyu yanlışlıkla selam sayarsa soruya cevap yerine hazır metin gelir. Bu, 100 rehber sorusu ve 16 takip sorusuyla ölçülür.
- Giriş cümlelerinin sadece kendi konusunda kullanılması dil modelinin talimata uymasına bağlıdır; sistemce denetlenmez, ölçümle kontrol edilir.

## Kullanıcı kararları (2026-11-04)
1. Kısa mesaj: B — soru işareti yoksa en fazla 10 kelime, varsa en fazla 5 kelime.
2. Yeniden yazma: A — asistanın önceki cevabı verilmez, sadece müşterinin önceki soruları.
3. Puan sınırı: A — rehber sorularındaki en düşük puanın 1 altı, yarım puana aşağı yuvarlanmış; kesin değer ölçüm tablosundan sonra kullanıcıya sorulur.
4. İsim mesajı: A — hazır selam metni.
5. Golden sınırı: A — en fazla 2.
6. Puan sınırı kesin değeri (M1 sonrası): -4,0 (kural A'nın sonucu; golden en düşük -2,74).
7. M1 sonrası ek düzeltmeler (kabul kriterlerini karşılamak için, kartı değiştirmez): etiket talimatına kısaltılmış selam örnekleri ("Nber" için); harf ya da rakam içermeyen mesaj ("???") modele gitmeden bulamadım alır.

## Düzeltme turu 1 (karar 7'nin uygulanması)
**Neden yeniden onay:** "???" gibi mesajın kaydı bugünkü tur türlerinin hiçbirine doğru uymuyor. "Boş bilgi bankası" türünde ekran "Bilgi bankasında henüz belge yok." yazar. "Puan sınırının altında" türünde ekran "rerank puanı eşiğin altında kaldı" yazar, oysa "???" hiç aranmadı. Bu yüzden yeni bir tür eklenir: "harf ya da rakam içermeyen mesaj". Bugünkü ekran tanımadığı türdeki cevabı "yarıda kesildi" gibi gösterir; bu nedenle ekranda küçük bir değişiklik gerekir ve **frontend-coder bu turda çalışır**. Çalışacak roller değiştiği için kart yeniden onayına gelir. Başka her şey aynı.

- **"???", "?!", "🙂" gibi harf ya da rakam içermeyen mesaj:** rehberde aranmaz, dil modeline gitmez, etiket de sorulmaz. Cevap "Bu konuda rehberimizde bilgi bulamadım." olur; numara yok, "Temsilciye bağlan" önerisi bugünkü gibi görünür. Önizleme'nin sağındaki "Son cevapta modele giden parçalar" kartı bu turda görünmez (selamlaşma turundaki gibi yerinde bugünkü bilgilendirme kartı durur). Konuşmalar'da durum "BİLMİYOR" olur. Ekranda yeni metin yok.
- **Kısaltılmış selam örnekleri:** etiket talimatına "Mrhb", "Meraba", "Slmlr", "Nbr kanka", "Gnydn" (selam), "Tşkkrler" (teşekkür), "Grşrz" (veda) eklenir. Ölçülen mesajlar ("Nber", "Slm" vb.) örnek olarak eklenmez; bunu bir test kontrol eder.
- **Puan sınırı -4,0** (karar 6) kalır; ölçüm raporu bu değerle yazılır.
- **Ölçüm raporunda (eklenen):** "???" bu yeni yoldan bulamadım almış; "Nber" dahil 37 selam mesajının hepsi hazır karşılık almış.

Kullanıcı kararı (Düzeltme turu 1): yeni tur türü (harf/rakam içermeyen mesaj) onaylandı; frontend-coder çalışır.

Kullanıcı kararı (qa düzeltmesi B1, 2026-11-04): ilk cümle dışında geçen giriş cümleleri kodla silinir (plan notu 2); "Kredi kartı limitim ne kadar?" varyantı da düzeltmeye dahil.

ONAY: VERİLDİ
