# E-Stock Noted

**E-Stock Noted**, Android (Kotlin + Material 3 + SQLite) ile geliştirilmiş profesyonel bir stok ve envanter yönetim uygulamasıdır.

## Özellikler

- **Rol Bazlı Erişim (RBAC) ve Güvenli Oturum**
  - **Yetkili (`ADMIN`)**: Ürün ekleme, düzenleme, silme, hızlı (`-`/`+`) ve toplu (notlu) stok giriş/çıkış işlemleri.
  - **Kullanıcı (`USER`)**: Stok listesini, KPI özetlerini, ürün detaylarını ve hareket geçmişini salt-okunur görüntüleme.
  - Tuzlanmış `PBKDF2WithHmacSHA256` şifre özetleme, kalıcı oturum (`SessionManager`) ve uygulama içi **Şifre Değiştir** desteği.
  - İlk kurulumda varsayılan yetkili hesabı: `admin` / `12345` (veritabanında hash'li saklanır).
- **Canlı KPI Stok Paneli (`StokBilgiActivity`)**
  - Ürün çeşidi, toplam stok adedi, kritik/tükenen stok uyarıları ve ₺ toplam envanter değeri.
  - Ürün adı, SKU kodu, kategori ve açıklamada anlık arama; durum (*Tümü / Kritik / Tükendi*) ve kategori filtreleme; 4 sıralama modu.
- **Alfanümerik SKU ve Zengin Ürün Modeli**
  - `STK-001`, `0042` gibi metin/sıfırla başlayan ürün kodları, kategori seçimi, kritik stok eşiği ve birim fiyat desteği.
- **Stok Hareket Geçmişi (`StokHareketActivity`)**
  - Tüm giriş, çıkış, güncelleme ve silme işlemlerinin kullanıcı, miktar değişimi, not ve tarih/saatle otomatik kaydı; arama ve hareket türü filtreleme.
- **CSV Rapor Dışa Aktarma (`StokCsvExporter`)**
  - Excel uyumlu UTF-8 BOM CSV stok raporunu Android paylaşım menüsüyle dışa aktarma.

## Derleme ve Test

```bash
cd "E-Stock Noted"
sh ./gradlew testDebugUnitTest assembleDebug lintDebug
```