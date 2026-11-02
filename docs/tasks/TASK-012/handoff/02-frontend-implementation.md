TASK: TASK-012
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `frontend-a11y` (combobox, listbox, dialog). Lab stilleri `kaynaklar/rag-lab.html`'deki `.chips`, `.comp .ib.lt`, `.ac`, `.qlist` ve pencere paragrafından birebir token'a çevrildi. `frontend-design` ve `react-state-management` ayrıca yüklenmedi: yeni görsel karar yok (lab'dan birebir alındı), server state tek bir `useQuery`.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| model/types (K1, K2a) | `model/types.ts` | `SuggestedQuestionResponse`, `SuggestionCategoryResponse`, `SuggestionSetResponse` (openapi ile birebir), `SUGGESTION_KINDS`, `SuggestionKind`, `SuggestionItem` |
| Normalizasyon (Kararlar #6, F15) | `model/suggestionText.ts` | `normalizeSuggestionText`: tr küçük harf, harf/rakam/boşluk dışı → boşluk, boşluklar teke, trim |
| Eşleştirme (K5a, kart soru 3 = A, F1) | `model/suggestionMatcher.ts` | `STEM_LENGTH=4`, `MIN_TOKEN_LENGTH=3`, `PREFIX_BONUS=3`, `STOP_WORDS` (lab listesi birebir), `createSuggestionMatcher(pool)` → `score(input, text)`; df yalnız liste sorularından |
| Çip / takip / otomatik tamamlama (K3a-c, K5a, F2, F3) | `model/suggestions.ts` | `CHIP_MAX_COUNT=2`, `AUTOCOMPLETE_MAX_ITEMS=7`, `listQuestionTexts`, `followUpsFor`, `suggestionChips`, `autocompleteItems`; ayrıca `autocompleteOptionId` (bkz. sapma 1) |
| api (K1) | `api/previewApi.ts`, `api/previewQueries.ts` (yeni) | `getSuggestions()` → `GET /api/assistant/suggestions`; `previewKeys.suggestions = ['preview','suggestions']`, `useSuggestionsQuery()` (yoklama yok, Kararlar #10) |
| hooks (K4) | `hooks/useAnswerStream.ts` | `askInNewConversation(text)`: akış sürüyorsa ya da metin gönderilemezse bir şey yapmaz; tur varsa `startNewConversation` sonra `sendQuestion`, yoksa doğrudan `sendQuestion` |
| Çipler (K2b, K3a, K3d, K4) | `components/SuggestionChips.tsx` + `.module.css` | `role="group"` `aria-label="Soru önerileri"`; boş durum `<p>`; takip `.followUp` + `title="Bu konuşmada devam"`; soru `hasTurns` ise `title="Yeni konuşmada sorulur"`; `disabled={isDisabled}`; uzun metin satıra sarar |
| Otomatik tamamlama listesi (K5a, K5c, K7) | `components/AutocompleteList.tsx` + `.module.css` | `role="listbox"` `aria-label="Soru önerileri"`; takip grubu `role="group"` + görünür başlık "Bu konuşmada devam" (`aria-labelledby`); sorular `hasTurns` ise "Yeni konuşmada" grubunda, değilse grupsuz; `role="option"`, `aria-selected`; `onMouseDown` `preventDefault`; "listeden seç" / "Eşleşen soru yok" paragrafı yok |
| Combobox, "Soru listesi" düğmesi (K5b, K5c, K7, Kararlar #9) | `components/MessageComposer.tsx` + `.module.css` | Yeni props imzası plandaki gibi. Input `role="combobox"`, `aria-autocomplete="list"`, `aria-expanded`, `aria-controls`, `aria-activedescendant`. ↑/↓ döngüsel (null → 0), Enter seçili öğeyi seçer yoksa yazılanı gönderir, Esc listeyi kapatır (değer kalır), blur listeyi kapatır. "Soru listesi" düğmesi lab `.ib.lt` stilinde, akış sürerken de etkin. `.send` sınıfı `.iconButton` oldu, `.light` eklendi |
| Kayıtlı sorular penceresi (K6, K2b, G1) | `components/QuestionListDialog.tsx` + `.module.css` | `shared/components/Dialog` ile. Başlık, açıklama, boş durum, `ErrorAlert`; "Bu konuşmada devam" sonra kategori başlıkları (`<h4>`, lab `h6` görünümü); akış sürerken düğmeler `disabled`; tıklama → `onSelect` + `onClose` (odak açan düğmeye döner) |
| Sayfa (K3b, K4, G1, G5) | `pages/PreviewPage.tsx` | `useSuggestionsQuery`, `useMemo` ile matcher; telefon içinde `MessageList` → `SuggestionChips` → `MessageComposer`; veri yoksa çip yok ve boş durum yok; pencere hata kodu `errorCodeOf` (ApiError değilse `UNEXPECTED_ERROR`); `handleSuggestionSelect`: `followUp` → `sendQuestion`, `question` → `askInNewConversation` |
| Token'lar (AC3) | `src/index.css` | Lab değerleri: `--color-follow-up-line #e1c3cf`, `--color-autocomplete-line #e1d6db`, `--color-group-heading #958890`, `--text-chip 12px`, `--text-autocomplete 12.5px`, `--text-autocomplete-heading 10px`, `--text-question-list 13px`, `--text-question-list-heading 11px`, `--text-dialog-note 13px`, `--radius-suggestion-chip 14px`, `--radius-autocomplete 12px`, `--radius-autocomplete-option 8px`, `--radius-question-list-option 7px`, `--shadow-autocomplete`, `--size-autocomplete-max 250px`, `--size-question-list-max 380px`, `--offset-autocomplete-bottom 56px`, `--opacity-chip-disabled 0.5`, `--z-autocomplete 5`, `--letter-spacing-autocomplete-heading 0.06em`, `--line-height-chip-empty 1.4` |
| errorMessages | — | Yeni kod yok (plan) |
| web/CLAUDE.md | `web/CLAUDE.md` | Durum yorumu ve preview klasör haritası güncellendi. Yeni komut/paket yok |

## Değişen dosyalar
Hepsi `web/` altında.
- Yeni: `src/features/preview/api/previewQueries.ts`, `src/features/preview/model/suggestionText.ts`, `src/features/preview/model/suggestionMatcher.ts`, `src/features/preview/model/suggestions.ts`, `src/features/preview/components/SuggestionChips.tsx` + `.module.css`, `src/features/preview/components/AutocompleteList.tsx` + `.module.css`, `src/features/preview/components/QuestionListDialog.tsx` + `.module.css`
- Değişen: `src/features/preview/model/types.ts`, `src/features/preview/api/previewApi.ts`, `src/features/preview/hooks/useAnswerStream.ts`, `src/features/preview/components/MessageComposer.tsx` + `.module.css`, `src/features/preview/pages/PreviewPage.tsx`, `src/index.css`, `CLAUDE.md`
- Silinen: yok. Paket eklenmedi.
- Kodda sabit soru listesi yok (M5 grep'i `web/src` için boş döner; "kaybettim/asgari/aidat" kelimeleri uygulama kodunda geçmiyor).

## Ekran metinleri
- Çip satırı: grup `aria-label="Soru önerileri"`; boş durum `Henüz soru önerisi yok.`; takip çipi `title="Bu konuşmada devam"`; diğer çip (konuşmada tur varsa) `title="Yeni konuşmada sorulur"`, tur yoksa `title` yok.
- Giriş: liste düğmesi `aria-label="Soru listesi"`, `title="Kayıtlı sorular"`; input `role="combobox"`, `aria-label="Soru"`, placeholder `Sorunu yaz…`; gönder düğmesi `aria-label="Gönder"`.
- Otomatik tamamlama: `role="listbox"` `aria-label="Soru önerileri"`; grup başlıkları `Bu konuşmada devam`, `Yeni konuşmada` (gruplar `role="group"` ve bu adlarla); seçenek id'si `${listId}-option-${index}`.
- Pencere: başlık `Kayıtlı sorular · {questionCount} soru, {followUpCount} takip`, veri yoksa `Kayıtlı sorular`; açıklama `Ürün ekibinin hazırladığı öneri sorular; sorunu her zaman kendin de yazabilirsin. Takip soruları ilk sorudan sonra "devam" önerisi olarak görünür.`; boş durum `Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür.`; ilk bölüm başlığı `Bu konuşmada devam`, sonra kategori adları (`heading` seviye 4); kapatma `aria-label="Kapat"`.
- Hata (G1): `alert` → `messageFor(errorCode)`, 500 `INTERNAL_ERROR` için `Beklenmeyen bir hata oluştu. Tekrar dene.`

## Plandan sapmalar
1. **`autocompleteOptionId` yeri:** Planda `AutocompleteList.tsx`'ten export ediliyordu. oxlint `react/only-export-components` uyarısı verdiği için (uyarı bırakılamaz) `model/suggestions.ts`'e taşındı. İmza ve id biçimi aynı. Lead'e mesajla bildirildi; lead onayladı.
2. **Test dosyasının props uyumu:** Plandaki yeni `MessageComposer` props'u yüzünden mevcut `MessageComposer.test.tsx` derlenmiyor (bkz. Doğrulama). Dosya tester'ın; lead onayıyla frontend-coder dokunmadı, tester güncelleyecek.
3. **`aria-controls`:** Sadece liste görünürken yazılıyor (görünmezken var olmayan bir id'yi göstermesin diye). Liste açıkken değeri plandaki gibi listbox id'si.
4. **Seçenek `div`'lerinde oxlint istisnası:** `jsx-a11y/click-events-have-key-events` ve `interactive-supports-focus` satır içi kapatıldı; gerekçe yorumda: APG combobox kalıbında odak input'ta kalıyor, klavye input'ta işleniyor (Kararlar #9). `Dialog.tsx`'teki mevcut kalıpla aynı.

## Doğrulama
- `cd web && npm run lint` → 0 uyarı, 0 hata (çıkış 0).
- `cd web && npm run build` → **kaldı, sadece tester'ın dosyasında:** `src/features/preview/components/MessageComposer.test.tsx` 4 × TS2739 (eski props `{ isStreaming, onSend }`; plandaki yeni zorunlu props eksik). `tsc -b` test dosyalarını da derlediği için build orada duruyor.
  - Hata çıktısının tamamı (başka hata yok):
    ```
    src/features/preview/components/MessageComposer.test.tsx(9,13): error TS2739: Type '{ isStreaming: false; onSend: Mock<Procedure>; }' is missing the following properties from type 'MessageComposerProps': hasTurns, autocompleteItemsFor, onSuggestionSelect, onQuestionListOpen
    src/features/preview/components/MessageComposer.test.tsx(19,13): error TS2739: Type '{ isStreaming: false; onSend: () => undefined; }' is missing the following properties from type 'MessageComposerProps': hasTurns, autocompleteItemsFor, onSuggestionSelect, onQuestionListOpen
    src/features/preview/components/MessageComposer.test.tsx(31,13): error TS2739: Type '{ isStreaming: false; onSend: Mock<Procedure>; }' is missing the following properties from type 'MessageComposerProps': hasTurns, autocompleteItemsFor, onSuggestionSelect, onQuestionListOpen
    src/features/preview/components/MessageComposer.test.tsx(41,13): error TS2739: Type '{ isStreaming: true; onSend: () => undefined; }' is missing the following properties from type 'MessageComposerProps': hasTurns, autocompleteItemsFor, onSuggestionSelect, onQuestionListOpen
    ```
  - **Uygulama kodu temiz derleniyor:** test dosyaları hariç tutularak yapılan tip kontrolü (`tsconfig.app.json`'u genişleten geçici bir config, sonra silindi) 0 hata verdi. `npx vite build` başarılı.
  - Lead kararı: `MessageComposer.test.tsx`'e frontend-coder dokunmaz; tester başlatılırken güncellemesi istenecek.
- `npx vitest run` → 24 dosyadan 22'si geçti; 423 testten 373'ü geçti, 50'si kaldı. Kalanların hepsi `MessageComposer.test.tsx` ve `PreviewPage.test.tsx`'te ve plandaki değişikliklerden geliyor:
  - Input artık `role="combobox"` olduğu için testlerdeki `getByRole('textbox', { name: 'Soru' })` bulamıyor (45 test). `getByRole('combobox', { name: 'Soru' })` olmalı.
  - MSW'de `GET /api/assistant/suggestions` handler'ı yok (plan: tester varsayılan boş set ekler). Bu `onUnhandledRequest` hatası veriyor ve `givenAnswerDone_whenDone_thenLiveConfigRefetchedAndRerankerDownShown` testini de etkiliyor.
- Model mantığı plandaki fixture ile Node'da ayrıca denendi (`node --experimental-strip-types`, scratchpad'de; repoya dosya eklenmedi). Hepsi plandaki değerlerle aynı:
  - F1: `şifre kart` skorları 0,223 / 1,139 / 0,223 / 0. `asgari` ve `ASGARİ` için sadece `Asgari…` > 0. `ne kaç` için hepsi 0. `kar` için `Kart…` ile başlayanlara sadece 3.
  - F2: altı durum da plandaki sırayla çıktı.
  - F3: `kart` girdisinde ilk öğe `Kartımı nasıl dondururum?` (`followUp`). Boş girdi `[]` döndü.
  - B12/F15 normalizasyonu: `kartımı kaybettim ne yapmalıyım` ve `işlem`.

## Tester için notlar
- **Testlerdeki kırılmalar (uygulama hatası değil, planlı değişiklik):** `MessageComposer.test.tsx` yeni props'la render edilmeli: `hasTurns`, `autocompleteItemsFor`, `onSuggestionSelect` ve `onQuestionListOpen` zorunlu. Soru alanı `combobox` rolünde. `src/test/handlers.ts`'e varsayılan `GET /api/assistant/suggestions` → `{questionCount:0, followUpCount:0, categories:[]}` eklenmeli.
- **Boş set ve çip satırı:** Varsayılan boş sette çip satırında `Henüz soru önerisi yok.` görünür. Mevcut sayfa testlerinde metin eşleşmesi buna takılabilir.
- **Kendi handler'ı olan testler:** Bu testler kendi `GET /api/assistant/suggestions` handler'ını verir (`SUGGESTIONS_FIXTURE`).
- **`MessageComposer` tek başına:** `autocompleteItemsFor` saf bir fonksiyon olarak verilebilir (ör. `(input) => input.includes('asgari') ? [{ text: 'Asgari ödeme tutarı nedir?', kind: 'question' }] : []`). Liste sadece normalize girdi boş değilken ve fonksiyon en az bir öğe döndüğünde görünür.
- **Seçeneğe tıklama:** Seçenekte `onMouseDown` `preventDefault` ve `onClick` var. `user.click(option)` input'tan blur'a yol açmadan seçer.
- **Pencere:** `Dialog` `document.body`'ye portal edilir. Açılınca odak "Kapat" düğmesine gider. Soru tıklanınca ya da Esc'e basılınca pencere kapanır ve odak "Soru listesi" düğmesine döner.
- **Akış sürerken:** Input `disabled` (mevcut davranış). Çipler ve penceredeki soru düğmeleri `disabled`. "Soru listesi" düğmesi etkin kalır (lab).
- **F5 (yeni konuşma):** `askInNewConversation` önce `startNewConversation` ile turları `previousTurns`'e taşır, sonra ilk soruyla yeni `POST /conversations` atar. Ayraç `Yeni konuşma · {id}` mevcut `MessageList`'ten gelir.
- **Takip hesabı:** Takip önerisi sadece son tur `done` ise ve sorusu listedeki bir soruyla normalize hâliyle aynıysa çıkar. `failed` tur takip vermez (G5).

STATUS: DONE
