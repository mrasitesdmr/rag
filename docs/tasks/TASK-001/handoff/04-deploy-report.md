TASK: TASK-001
# Deploy raporu

Docker değişmedi, komut çalıştırılmadı (doküman task'ı; `docker`, `./mvnw`, `npm` çalıştırılmadı, model indirilmedi, `ollama.md` yazılmadı).

## Değişen dosyalar
- `deployment/CLAUDE.md` (D-1)
- `README.md` (D-2)

## Servisler
Ayağa kaldırılan servis yok. Hedef tablo `deployment/CLAUDE.md`'de (plan M3 ile birebir aynı):

| Servis | İmaj / etiket | Port | Durum (ps çıktısı) |
|--------|---------------|------|--------------------|
| web | — | 3000 | çalıştırılmadı |
| assistant-api | — | 8090 | çalıştırılmadı |
| retrieval-api | — | 8080 | çalıştırılmadı |
| postgres | `pgvector/pgvector:0.8.1-pg18` (hedef) | 127.0.0.1:5432 | çalıştırılmadı |
| pgadmin | — | 127.0.0.1:5050 | çalıştırılmadı |

## Smoke test
Yapılmadı; sistem yok (iskelet task'ı).

## Nasıl çalıştırılır
Henüz compose dosyası yok. Hedef komutlar `deployment/CLAUDE.md` "Komutlar" bölümünde; ilk Docker task'ında gerçek komutlarla değiştirilecek.

## Düzeltme
Yok.

STATUS: DONE
