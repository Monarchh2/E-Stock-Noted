# E-Stock Noted (v3.0 — Ticari Stok, Grafiksel Raporlama & Güvenlik)

**E-Stock Noted**, Android (Kotlin + Material 3 + SQLite) ile geliştirilmiş ticari ölçekli stok, kârlılık analizi ve envanter yönetim uygulamasıdır.

## Öne Çıkan Özellikler

### 1. Ticari Envanter & Cari Yönetimi
- **Maliyet, Satış, KDV ve Kâr Marjı**: Her ürün için alış fiyatı, satış fiyatı, KDV oranı (`%`), depo/raf konumu ve tedarikçi eşleştirmesi; otomatik birim kâr, toplam kâr ve kâr marjı hesaplaması.
- **Tedarikçi & Cari Yönetimi (`TedarikciActivity`)**: Tedarikçi kayıtları (firma, yetkili, telefon, e-posta, vergi no, not) ve ürün formlarında otomatik tedarikçi seçimi.
- **Canlı KPI Paneli (`StokBilgiActivity`)**: Ürün çeşidi, toplam stok adedi, kritik/tükenen uyarıları, toplam envanter değeri; anlık arama, durum/kategori filtresi, 4 sıralama modu, tek dokunuşla `-/+` ve notlu toplu giriş/çıkış (`İşlem`).
- **Stok Hareket Geçmişi (`StokHareketActivity`)**: Her giriş, çıkış, güncelleme ve silme kaydının kullanıcı/tarih/notla otomatik kaydı ve arama/tür filtreleme.
- **PDF, CSV ve JSON Yedekleme**: Çok sayfalı A4 ticari PDF raporu (`PdfReportGenerator`), formül enjeksiyonuna karşı korumalı UTF-8 BOM CSV (`StokCsvExporter`) ve doğrulanmış JSON yedek dışa aktarımı (`BackupJsonCodec`).

### 2. Detaylı Grafiksel Raporlama (`RaporlarActivity`)
- **Kategori Dağılımı Donut Grafiği (`DonutChartView`)**
- **Günlük Stok Giriş / Çıkış Trend Çubuk Grafiği (`BarTrendChartView`)** — *Son 7 Gün / Son 30 Gün / Tüm Zamanlar*
- **En Değerli 5 Ürün — ABC Yatay Sıralama Grafiği (`HorizontalRankingChartView`)**
- **Stok Sağlığı Çubuğu, Finansal Kârlılık Kartları ve Otomatik Sipariş Öneri Listesi**

### 3. Kurumsal Güvenlik Önlemleri & Güvenlik Test Paketi
- **65.536 İterasyonlu Tuzlanmış PBKDF2-HMAC-SHA256 (`PasswordHasher`)** ve sabit zamanlı doğrulama.
- **Kaba Kuvvet (Brute-Force) Koruması (`LoginRateLimiter`)**: 5 hatalı denemede artan süreli (60s → 180s → 300s) otomatik kilitleme.
- **Güçlü Parola Politikası (`PasswordPolicy`)**: En az 8 karakter, büyük/küçük harf ve rakam zorunluluğu + varsayılan admin şifresi (`admin` / `12345`) için ilk girişte değiştirme uyarısı.
- **3 Seviyeli RBAC (`ADMIN` / `DEPO` / `USER`) + `AccessGuard`**, 15 dakika işlemsizlik oturum zaman aşımı, hassas ekranlarda `FLAG_SECURE`, **Güvenlik Denetim Günlüğü (`AdminYonetimActivity`)**, CSV Formula Injection nötralizasyonu ve tam sayı taşma koruması.

## Derleme ve Test

```bash
cd "E-Stock Noted"
sh ./gradlew testDebugUnitTest assembleDebug lintDebug
```