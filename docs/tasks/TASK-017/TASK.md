# TASK-017: Kimlik doğrulama ve oturum açan kullanıcı

## Ne isteniyor
Yönetim paneline giriş yapılır. Üst bardaki kullanıcı alanı (lab'da "Raşit Eşdemir · Dijital Kanallar · Ürün sahibi" ve baş harfler "RE") oturum açan gerçek kullanıcıyı gösterir. Web'in iki servise attığı istekler kimlik doğrulamalıdır; lab'da tüm istekler `Authorization: Bearer …` başlığıyla gidiyordu (kimlik sağlayıcı `rb-idp-03`). Mock kullanıcı ya da sabit isim yoktur.

## Kurallar
1. **Kimlik sağlayıcı:** Hangi kimlik sağlayıcının kullanılacağı (ör. Docker'da Keycloak ile OpenID Connect, ya da servis içinde kullanıcı tablosu) onay kartında kullanıcıya sorulur. Gerçek bankalardaki uygulamaya en yakın seçenek önerilir. Yeni servis ya da kütüphane bir plan kararıdır.
2. **Erişim:** İki servisin bütün `/api/**` uçları kimlik doğrulama ister. Sağlık uçları açık kalır. Token yoksa ya da geçersizse 401 ve sabit `errorCode` döner. Yetki yoksa 403 ve sabit `errorCode` döner.
3. **Servisler arası:** assistant-api'nin retrieval-api'ye giden isteklerinin nasıl kimlik doğrulayacağı (kullanıcı token'ını iletme ya da servis hesabı) onay kartında belirlenir.
4. **Kullanıcı bilgisi:** Üst barda ad, birim ve rol ile baş harf rozeti gösterilir. Bu bilgiler kimlik sağlayıcıdan gelir.
5. **Oturum:** Giriş ve çıkış ekranı ya da yönlendirmesi lab'da yok; `frontend-design` ile lab'ın görsel diliyle tasarlanır. Oturum süresi dolunca kullanıcı yeniden giriş yapar. Yarım kalan akış ya da yükleme Türkçe bir mesajla bildirilir.
6. **Kayıt:** Turlar, geri bildirimler, aktarmalar, içe alma işleri ve değerlendirme çalıştırmaları onları başlatan kullanıcının kimliğini saklar. Kişisel veri loglanmaz; sadece kullanıcı kimliği loglanır.
7. **SSE:** Cevap akışı da kimlik doğrulamalıdır; token'ın akış isteğinde nasıl taşınacağı `docs/api/sse.md`'ye yazılır.
8. Token tarayıcıda güvenli biçimde tutulur; nerede tutulacağı `frontend-security` skill'ine göre onay kartında yazılır.

## Örnek senaryolar
- Kullanıcı paneli açar, giriş ekranına yönlendirilir, giriş yapar. Üst barda kendi adını ve rolünü görür.
- Token olmadan `GET /api/workspaces/42/sources` → 401.
- Oturum süresi dolmuş kullanıcı soru sorar; yeniden giriş yapması istenir.

## Kabul kriterleri
- Kimlik sağlayıcı kullanıcı onayıyla seçilmiş ve Compose'ta çalışıyor.
- Bütün uçlar için yetkisiz erişim testleri yazılmış (401/403).
- Önceki task'ların E2E testleri girişle birlikte geçiyor; ilgili build komutları geçer.

## Kapsam dışı
- Rol bazlı ekran kısıtlamaları (lab'da tek rol görünüyor); gerekiyorsa ayrı task.
- Müşteri (Raşit Mobil) tarafı kimlik doğrulaması.

## Etkilenen katmanlar
- [x] assistant-api (konuşma, model, prompt, akış, değerlendirme)
- [x] retrieval-api (kaynak, içe alma, embedding, arama)
- [x] Web
- [x] API sözleşmesi (REST)
- [x] Cevap akışı (SSE)
- [ ] Dil modeli / prompt / reranker (lokal model gerekiyor mu?)
- [x] Veritabanı (migration; hangi servis?) (kullanıcı kimliği kolonları)
- [ ] WebSocket
- [x] Docker
- [ ] Ekip altyapısı (`.claude/**`, `CLAUDE.md`'ler, kılavuz)
