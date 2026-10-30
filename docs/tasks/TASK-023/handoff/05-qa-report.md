TASK: TASK-023
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`. Servis, web ve API sözleşmesi değişmediği için Java, frontend ve e2e skill'leri kullanılmadı.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt | Sonuç |
|---|------------------------|-------|-------|
| 1 | Gerçek kart şeması markası iki PDF'te geçmiyor; D12 listesine eklendi | `kontroller.txt` içinde 11 yeni ad var (Mastercard, Visa, VISA, American Express, AMERICAN EXPRESS, Amex, Troy, Maestro, UnionPay, JCB, Diners Club). Kendi çalıştırmamda `dogrula.sh` D12: "35 gerçek banka adı geçmiyor". Ayrı bir tarama yaptım: `pdftotext` çıktısında `mastercard\|visa\|american express\|amex\|troy\|maestro\|unionpay\|jcb\|diners` iki PDF için 0 eşleşme (büyük/küçük harf duyarsız, alt dize) | ✅ |
| 2 | Yerine gelen ifade kullanıcı seçimi | Onay kartı "Kullanıcı kararı": 4 soruda A; 2.17 için uygulama sırasında "uluslararası kurallar çerçevesinde". Kayıt var. PDF'teki metin kayıtla aynı (s. 7, 15, 17) | ✅ |
| 3 | Olgular değişmedi | s. 15: yurt dışında kullanılabilir, "Yurt dışı kullanım" seyahatten önce açılır, %1,99 ücret aynı. s. 17: 120 gün, ortalama 45 gün, yurt dışında 90 güne kadar aynı. `git diff` kaynakta yalnız 4 satır (4+/4-) | ✅ |
| 4 | Sayfa sayısı ve bölüm başlangıçları değişmedi | İki sürüm de 70 sayfa. D9: başlangıçlar {1=5, 2=7, 3=20, 4=29, 5=38, 6=45, 7=51, 8=57, 9=62, 10=65, 11=68}, bunlar eski değerlerle aynı. Eski PDF'ler (`git show HEAD:`) ile sayfa sayfa karşılaştırma (`pdftotext -layout`, lead'in PDFBox'ından farklı araç): her iki sürümde yalnız s. 7, 15, 17 farklı; diğer 67 sayfa birebir aynı. `degisiklik-ozeti-2026.11.md` değişmedi, D8 GEÇTİ | ✅ |
| 5 | PDF'ler `uret.sh` ile üretilir, `dogrula.sh` geçer | `uret.sh` ile scratch'e kendim ürettim. `cmp` iki PDF için de repodakilerle aynı. SHA-256 lead raporuyla aynı: 2026.10 `7585c2ab…a827c`, 2026.11 `4db9c9e8…e20b`. `dogrula.sh`: 11 GEÇTİ, 0 KALDI | ✅ |
| 6 | Kullanıcı değişen metni onayladı | Lead raporunda "Kullanıcı içerik onayı" kaydı var, M1-M4 sayfa numaralarıyla. Bunu kendim doğrulayamam, lead'in kaydına dayanıyor | ✅ (lead kaydı) |
| 7 | TASK-005 öncesi yapılmalı | `docs/tasks/TASK-005/` altında yalnız `TASK.md` var, handoff yok. Rehber henüz içe alınmamış | ✅ |
| 8 | Örnek senaryo: 2.14 marka adı olmadan aynı olguyu anlatıyor | s. 15 PDF'ten okundu ve görüntü olarak incelendi: "Kartlarınız yurt dışında, kartınızın üzerindeki uluslararası kart logosunun bulunduğu her yerde kullanılabilir…" Ardından açma koşulu ve %1,99 ücret var. Düzen bozuk değil | ✅ |

## Sözleşme uyumu
Uygulanamaz. `docs/api/`, servis ve web değişmedi (`git status`: yalnız `docs/rehber/**` ve `handoff/`). `kaynaklar/**` değişmedi.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|-------|-------|---------------------|
| `./docs/rehber/uret.sh <scratch>` | 70 / 70 sayfa. `cmp` repodaki PDF'lerle aynı. SHA-256 aynı | Evet |
| `./docs/rehber/dogrula.sh` | 11 GEÇTİ, 0 KALDI; D12 "35 gerçek banka adı" | Evet |
| Eski ve yeni PDF'lerin sayfa sayfa `pdftotext -layout` karşılaştırması | Farklı sayfalar: s. 7, 15, 17 (iki sürümde de) | Evet (lead: PDFBox, aynı sonuç) |
| `mvn`, `npm`, Playwright, PIT, Stryker | Uygulanamaz (doküman task'ı, kod yok) | — |

### Kendi negatif denemem
Repo dışında kopya: `<scratch>/neg`. Lead'inkinden farklı adlar eklendi: 2.1'e "American Express altyapısıyla", 2.17'ye "AMEX kuralları çerçevesinde", 2.14'e "Maestro'nun ve JCB'nin logosunun". Kopyada PDF üretilip `dogrula.sh` çalıştırıldı. Sonuç: D12 KALDI, iki sürümde de American Express, Amex, Maestro ve JCB gösterildi; toplam 10 GEÇTİ, 1 KALDI. Kontrol büyük harfli "AMEX"i de yakaladı ve "Maestro'nun" gibi kesme işaretli eki de yakaladı. Aynı sayfa düzeni D1 ve D9'u geçti. Repo dosyalarına dokunulmadı; `git status` yalnız beklenen 5 dosyayı ve `handoff/`'u gösteriyor.

## Plandan sapma değerlendirmesi (2.17)
- Onaylı A ifadesi ("uluslararası kart kuralları") 2026.11'i 71 sayfaya çıkarıp D1, D8 ve D9'u kırıyordu; kural 4 (sayfa sayısı ve bölüm başlangıçları sabit) bu ifadeyle sağlanamıyordu.
- Lead ifadeyi kendi başına değiştirmedi: varyantları scratch'te denedi ve kullanıcıya sordu. Kullanıcının seçimi kartın "Kullanıcı kararı" bölümüne tarihle yazılmış.
- Seçilen ifade ("uluslararası kurallar çerçevesinde") olguyu korur: işyeri bankasından belge istenir. 120 gün, 45 gün ve 90 gün süreleri s. 17'de aynı.
- Sapma meşru ve belgelenmiş. README yazım kuralı da aynı ifadeyi listeliyor, tutarlı.

## Mutation testi
Kurulu değil / uygulanamaz (kod yok).

## Uçtan uca
Uygulanamaz (ekran ve servis yok). Dil modeli sorusu: rehber henüz içe alınmadı (TASK-005), bu yüzden model senaryosu çalıştırılamadı. Yerine PDF metninde 2.14 olgularının varlığı doğrulandı (kural 8).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|----------|-------------|-------|-------------|
| Optional | `docs/rehber/dogrulama/kontroller.txt` | Alt dize taramam 0 sonuç verdi, ama kalıcı kontrol tam kelime arar ve "Mastercardla" gibi kesme işaretsiz ek kaçabilir. Onay kartı bu riski biliyor ve ayrı task'a bırakmış (TASK-021 sağlamlaştırma kapsamı). Durdurmaz | lead / planner |
| FYI | `docs/rehber/README.md` | README'de marka adları yazım kuralı olarak geçiyor. README PDF'e girmez, kasıtlı | — |
| FYI | s. 7, 15, 17 | "uluslararası kart altyapısı" ifadesi 2.1 ve 2.2'de, 2.17'de "uluslararası kurallar" kullanılıyor. Kullanıcı kararı gereği farklı ifade | — |

Critical ya da Required bulgu yok.

## Düzeltme
Gerekmiyor.

STATUS: DONE
