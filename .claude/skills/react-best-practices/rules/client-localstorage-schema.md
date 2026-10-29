---
title: localStorage Verisini Versiyonla ve Küçült
impact: MEDIUM
impactDescription: schema çakışmalarını önler, storage boyutunu azaltır
tags: client, localStorage, storage, versioning, data-minimization
---

## localStorage Verisini Versiyonla ve Küçült

Key'lere versiyon prefix'i ekle ve sadece gereken alanları sakla. Bu, schema çakışmalarını ve hassas verinin yanlışlıkla saklanmasını önler. Kural `sessionStorage` için de aynen geçerlidir.

> **Proje notu:** `localStorage` sadece gizli olmayan, kişiye özel görünüm tercihleri için ve tek bir `model/<topic>Settings.ts` modülü üzerinden kullanılır; key `rag:<topic>:v1` biçiminde versiyonludur (`frontend-conventions` bölüm 9). Asistan ayarları (üretim modu, retrieval stratejisi) sunucu yapılandırmasıdır, tarayıcıda tutulmaz. Müşteri soruları, cevaplar, API anahtarı, auth token'ı veya PII tarayıcı storage'ına yazılmaz. Aşağıdaki örnekler genel kalıptır.

**Yanlış:**

```typescript
// Versiyon yok, her şeyi saklıyor, hata yönetimi yok
localStorage.setItem('userConfig', JSON.stringify(fullUserObject))
const data = localStorage.getItem('userConfig')
```

**Doğru:**

```typescript
const VERSION = 'v2'

function saveConfig(config: { theme: string; language: string }) {
  try {
    localStorage.setItem(`userConfig:${VERSION}`, JSON.stringify(config))
  } catch {
    // Incognito/private browsing'de, quota aşıldığında veya devre dışıyken throw eder
  }
}

function loadConfig() {
  try {
    const data = localStorage.getItem(`userConfig:${VERSION}`)
    return data ? JSON.parse(data) : null
  } catch {
    return null
  }
}

// v1'den v2'ye migration
function migrate() {
  try {
    const v1 = localStorage.getItem('userConfig:v1')
    if (v1) {
      const old = JSON.parse(v1)
      saveConfig({ theme: old.darkMode ? 'dark' : 'light', language: old.lang })
      localStorage.removeItem('userConfig:v1')
    }
  } catch {}
}
```

**Server response'larından minimum alanı sakla:**

```typescript
// User nesnesinin 20+ alanı var, sadece UI'ın ihtiyaç duyduğunu sakla
function cachePrefs(user: FullUser) {
  try {
    localStorage.setItem('prefs:v1', JSON.stringify({
      theme: user.preferences.theme,
      notifications: user.preferences.notifications
    }))
  } catch {}
}
```

**Her zaman try-catch ile sar:** `getItem()` ve `setItem()` incognito/private browsing'de (Safari, Firefox), quota aşıldığında ya da devre dışıyken throw eder.

**Faydaları:** Versiyonlama ile schema evrimi, daha küçük storage, token/PII/internal flag saklanmasının önlenmesi.
