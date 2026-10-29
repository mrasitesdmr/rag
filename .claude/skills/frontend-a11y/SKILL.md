---
name: frontend-a11y
description: React için accessibility pattern'leri - semantic HTML, ARIA attribute'ları, form label'ları, klavye navigasyonu, focus yönetimi ve screen reader desteği. Form, modal, dropdown, tooltip veya tab oluştururken ya da review ederken, a11y lint veya code review bulgularını düzeltirken, klavye navigasyonu ve focus yönetimini bağlarken kullan.
license: MIT
metadata:
  origin: ECC (community)
  source: https://github.com/affaan-m/ECC/tree/main/skills/frontend-a11y
---

# Frontend Accessibility Pattern'leri

React için pratik accessibility pattern'leri. Code review'da en sık işaretlenen sorunları kapsar: eksik form label'ları, yanlış ARIA kullanımı, semantic olmayan etkileşimli elemanlar ve bozuk klavye navigasyonu.

## Ne zaman devreye girer

- Form component'leri (`<input>`, `<select>`, `<textarea>`) oluştururken veya review ederken
- Etkileşimli elemanlar (modal, dropdown, tooltip, tab) oluştururken
- `onClick`'li `<div>` veya `<span>` kullanırken
- Herhangi bir elemana `aria-*` attribute'u eklerken
- Klavye navigasyonu veya focus yönetimi uygularken
- Code review araçlarından (CodeRabbit, oxlint/ESLint a11y kuralları) accessibility geri bildirimi alırken
- Screen reader'ları desteklemesi gereken component'ler oluştururken

## Form accessibility

Eksik `htmlFor` / `id` eşleşmesi ve bağlantısız hata mesajları code review'da en sık işaretlenen sorunlardır.

### Label bağlantısı

```tsx
// KÖTÜ: label'ın input'la bağlantısı yok — screen reader'lar onları ilişkilendiremez
<label>Email</label>
<input type="email" />

// İYİ: htmlFor input id'siyle eşleşiyor
<label htmlFor="email">Email</label>
<input id="email" type="email" />
```

### Zorunlu alanlar

```tsx
// KÖTÜ: sadece görsel yıldız screen reader'lara hiçbir şey iletmez
<label htmlFor="email">Email *</label>
<input id="email" type="email" />

// İYİ: required native tarayıcı validation'ını açar; aria-required bunu screen reader'lara bildirir
<label htmlFor="email">
  Email <span aria-hidden="true">*</span>
</label>
<input id="email" type="email" required aria-required="true" />
```

### Hata mesajları

```tsx
// KÖTÜ: hata metni görsel olarak var ama input'a bağlı değil
<input id="email" type="email" />
<span className="error">Invalid email address</span>

// İYİ: aria-describedby input'u hata mesajına bağlar
// aria-invalid geçersiz durumu screen reader'lara bildirir
<input
  id="email"
  type="email"
  aria-describedby="email-error"
  aria-invalid={!!error}
/>
{error && (
  <span id="email-error" role="alert">
    {error}
  </span>
)}
```

Bu projede backend'den gelen ve bir alana ait hatalar (`VALIDATION_FAILED`'daki `violations[].field` → soru ya da sürüm etiketi) ilgili input'a `aria-describedby` ile bağlanır; input'a ait olmayan hatalar (ör. `CHAT_MODEL_UNAVAILABLE`, `EMBEDDING_UNAVAILABLE`) form üstünde `role="alert"` ile gösterilir. Eşleme tablosu ve metin kaynakları (`messageFor(errorCode)`, alan kuralı mesajları) `frontend-conventions` bölüm 7'dedir.

### Tam erişilebilir form

Proje kalıbı: `useActionState` + native `<form action>` (`react-patterns`), alan kuralları `messageRules.ts`'ten, hata metinleri Türkçe ve `errorMessages.ts`'ten (`frontend-conventions` bölüm 7 ve 10). Erişilebilirlik açısından önemli olan kısımlar: erişilebilir ad (lab'daki gibi görünür etiket yoksa `aria-label`), `aria-invalid`, `aria-describedby`, alan hatasının input'un hemen altında olması ve input'a ait olmayan hatanın form üstünde `role="alert"` ile gösterilmesi.

```tsx
type MessageComposerState = { questionError: string | null }

const INITIAL_STATE: MessageComposerState = { questionError: null }

export function MessageComposer({ conversationId }: MessageComposerProps) {
  const answerStream = useAnswerStream(conversationId)
  const questionId = useId()
  const questionErrorId = `${questionId}-error`

  async function sendMessageAction(_previous: MessageComposerState, formData: FormData): Promise<MessageComposerState> {
    const text = String(formData.get('text') ?? '').trim()
    if (!isValidMessage(text)) {
      return { questionError: MESSAGE_RULE_MESSAGE }
    }
    answerStream.send(text)
    return INITIAL_STATE
  }

  const [state, formAction] = useActionState(sendMessageAction, INITIAL_STATE)
  const isStreaming = answerStream.status === 'streaming'

  return (
    <form action={formAction} noValidate>
      {answerStream.errorCode ? <p role="alert">{messageFor(answerStream.errorCode)}</p> : null}
      <textarea
        id={questionId}
        name="text"
        aria-label="Soru"
        placeholder="Sorunu yaz…"
        required
        maxLength={MESSAGE_MAX_LENGTH}
        aria-invalid={state.questionError !== null}
        aria-describedby={state.questionError ? questionErrorId : undefined}
      />
      {state.questionError ? (
        <p id={questionErrorId} role="alert">
          {state.questionError}
        </p>
      ) : null}
      <button type="submit" aria-label="Gönder" disabled={isStreaming}>
        Gönder
      </button>
    </form>
  )
}
```

Cevaptaki alıntı numaraları (`[1]`) düğmeye çevrilir ve erişilebilir adı numarayı değil ne yaptığını söyler (lab: `aria-label="Kaynak 1"`; ekran okuyucu "Kaynak 1, düğme" der). Düğme parçayı bir diyalogda açar; diyalog kapanınca odak düğmeye döner.

```

Bu projede canlı güncellenen içerikler için `aria-live="polite"` bir bölge kullan: cevap bitince ("Cevap tamamlandı"; her token duyurulmaz), ingestion ilerlemesinde sadece durum değişiklikleri ("İçe alma tamamlandı"). Her ilerleme mesajını duyurma (her batch'te bir duyuru ekran okuyucuyu boğar); ilerleme çubuğu `role="progressbar"` + `aria-valuenow`/`aria-valuemax` ile kendi değerini taşır. Tüm cevabı ya da parça listesini live region yapma, sadece özet mesajı duyur.

### aria-expanded ve aria-controls

```tsx
export function Accordion({ title, children }: { title: string; children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const contentId = useId();

  return (
    <div>
      <button aria-expanded={isOpen} aria-controls={contentId} onClick={() => setIsOpen(prev => !prev)}>
        {title}
      </button>
      <div id={contentId} hidden={!isOpen}>
        {children}
      </div>
    </div>
  );
}
```

## Klavye navigasyonu

Her etkileşimli eleman sadece klavyeyle erişilebilir ve kullanılabilir olmalıdır.

### Özel seçim listesi (select-only combobox)

Önce native `<select>` kullan; klavye, screen reader ve mobil davranışı ücretsiz gelir. Özel görünüm şartsa [WAI-ARIA APG select-only combobox](https://www.w3.org/WAI/ARIA/apg/patterns/combobox/examples/combobox-select-only/) kalıbını izle: focus combobox'ta kalır, aktif seçenek `aria-activedescendant` ile duyurulur ve listbox combobox'ın içinde değil kardeşidir.

```tsx
type OptionPickerProps = { label: string; options: string[]; onSelect: (value: string) => void }

export function OptionPicker({ label, options, onSelect }: OptionPickerProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(0)
  const baseId = useId()
  const labelId = `${baseId}-label`
  const listboxId = `${baseId}-listbox`
  const optionId = (index: number) => `${baseId}-option-${index}`

  function selectOption(index: number) {
    setActiveIndex(index)
    onSelect(options[index])
    setIsOpen(false)
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault()
        setIsOpen(true)
        setActiveIndex((index) => Math.min(index + 1, options.length - 1))
        break
      case 'ArrowUp':
        event.preventDefault()
        setIsOpen(true)
        setActiveIndex((index) => Math.max(index - 1, 0))
        break
      case 'Enter':
      case ' ':
        event.preventDefault()
        if (isOpen) {
          selectOption(activeIndex)
        } else {
          setIsOpen(true)
        }
        break
      case 'Escape':
        setIsOpen(false)
        break
    }
  }

  return (
    <div>
      <span id={labelId}>{label}</span>
      <div
        role="combobox"
        tabIndex={0}
        aria-labelledby={labelId}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-controls={listboxId}
        aria-activedescendant={isOpen ? optionId(activeIndex) : undefined}
        onKeyDown={handleKeyDown}
        onClick={() => setIsOpen((open) => !open)}
      >
        {options[activeIndex]}
      </div>
      <ul id={listboxId} role="listbox" aria-labelledby={labelId} hidden={!isOpen}>
        {options.map((option, index) => (
          <li
            key={option}
            id={optionId(index)}
            role="option"
            aria-selected={index === activeIndex}
            onClick={() => selectOption(index)}
          >
            {option}
          </li>
        ))}
      </ul>
    </div>
  )
}
```

## Focus yönetimi

UI state'i değiştiğinde focus mantıklı şekilde hareket etmelidir; özellikle modal'larda ve route geçişlerinde.

### Modal focus'unun geri yüklenmesi

> Bu örnek ilk focus'u ve geri yüklemeyi kapsar. Tam bir focus trap (modal içinde Tab/Shift+Tab döngüsü) için dinamik içerik ve iç içe portal gibi uç durumları yöneten [`focus-trap-react`](https://github.com/focus-trap/focus-trap-react) gibi bir kütüphane kullan. Native `<dialog>` elemanı ve `showModal()` de focus trap'i tarayıcıya bırakan iyi bir alternatiftir.

```tsx
export function Modal({ isOpen, onClose, title, children }: { isOpen: boolean; onClose: () => void; title: string; children: React.ReactNode }) {
  const modalRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (isOpen) {
      // Şu an focus'taki elemanı kaydet ve focus'u modal'a taşı
      previousFocusRef.current = document.activeElement as HTMLElement;
      modalRef.current?.focus();
    } else {
      // Focus'u modal'ı açan elemana geri ver
      previousFocusRef.current?.focus();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div ref={modalRef} role="dialog" aria-modal="true" aria-labelledby="modal-title" tabIndex={-1} onKeyDown={e => e.key === 'Escape' && onClose()}>
      <h2 id="modal-title">{title}</h2>
      {children}
      <button onClick={onClose}>Close</button>
    </div>
  );
}
```

## Görseller ve icon'lar

```tsx
// KÖTÜ: dekoratif icon etiketsiz bir görsel olarak duyurulur
<img src="/icon.svg" />

// İYİ: dekoratif görsel screen reader'lardan gizlenmiş
<img src="/decoration.png" alt="" aria-hidden="true" />

// İYİ: anlamlı görsel, açıklayıcı alt text ile
<img src="/chart.png" alt="Monthly revenue increased 23% from January to March" />

// İYİ: erişilebilir label'lı icon butonu
<button aria-label="Delete item">
  <TrashIcon aria-hidden="true" />
</button>
```

## Reduced motion

İşletim sistemi ayarlarında hareketin azaltılmasını isteyen kullanıcılara saygı göster.

```tsx
export function useReducedMotion(): boolean {
  const [prefersReduced, setPrefersReduced] = useState(false);

  useEffect(() => {
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    setPrefersReduced(mq.matches);
    const handler = (e: MediaQueryListEvent) => setPrefersReduced(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  return prefersReduced;
}

// Kullanım
export function AnimatedCard({ children }: { children: React.ReactNode }) {
  const reduceMotion = useReducedMotion();

  return (
    <div
      style={{
        transition: reduceMotion ? 'none' : 'transform 300ms ease'
      }}
    >
      {children}
    </div>
  );
}
```

Sadece CSS ile yönetilen animasyonlar için `@media (prefers-reduced-motion: reduce)` yeterlidir; hook'a sadece JS tarafında karar vermen gerektiğinde ihtiyaç var.

## Anti-pattern'ler

```tsx
// KÖTÜ: klavye desteği olmayan etkileşimsiz elemanda onClick
<div onClick={handleClick}>Click me</div>

// KÖTÜ: rolü olmayan bir div'de aria-label
<div aria-label="Navigation">...</div>

// KÖTÜ: label yerine placeholder kullanmak
<input placeholder="Enter your email" />

// KÖTÜ: pozitif tabIndex öngörülemeyen bir tab sırası oluşturur
<button tabIndex={3}>Submit</button>

// KÖTÜ: focus alabilen bir elemanda aria-hidden — klavye kullanıcıları sıkışır
<button aria-hidden="true">Open</button>

// KÖTÜ: klavye handler'ı olmayan div'de role="button"
<div role="button" onClick={handleClick}>Submit</div>
// Eksik: tabIndex={0}, Enter/Space için onKeyDown
```

## Kontrol listesi

Herhangi bir etkileşimli component'i review'a göndermeden önce (diğer örneklerdeki İngilizce metinler sadece kalıbı gösterir; projede `aria-label` dahil tüm görünen/duyurulan metinler Türkçedir):

- [ ] Her `<input>`, `<select>` ve `<textarea>`'nın `htmlFor`/`id` ile bağlı bir `<label>`'ı var
- [ ] Hata mesajları `aria-describedby` ile bağlı ve `role="alert"` ile işaretli
- [ ] `role`, `tabIndex` ve `onKeyDown` olmadan `<div>` veya `<span>` üzerinde `onClick` yok
- [ ] Sadece icon içeren butonların `aria-label`'ı var
- [ ] Dekoratif görseller `alt=""` ve `aria-hidden="true"` kullanıyor
- [ ] Modal'lar kapanınca focus'u geri yüklüyor (Tab/Shift+Tab döngülü tam focus trap için `focus-trap-react` gibi bir kütüphane kullan)
- [ ] Dinamik içerik güncellemeleri `aria-live` kullanıyor
- [ ] Animasyonlarda `prefers-reduced-motion`'a saygı gösteriliyor

## İlgili skill'ler

- `react-patterns` - genel React component ve state pattern'leri
- `react-testing` - axe ile accessibility assertion'ları
- `web-design-guidelines` - UI kodunu Web Interface Guidelines'a göre review etme
- `frontend-design` - görsel tasarım ve erişilebilirlik kalite tabanı
