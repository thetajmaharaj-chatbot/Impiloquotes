# Impilo Drilling Quotes Android

Mobile quotation entry for the existing Impilo Drilling borehole estimate format.

## Scope of this first build

- Uses the supplied Excel borehole estimate as the source of truth.
- Keeps the same borehole line items and rates from the analysed template.
- Uses the same calculation: `quantity × rate`, subtotal, 15% VAT, total.
- Generates one fixed A4 PDF page using the extracted Excel design/letterhead as the background.
- Customer, project, date, estimate number, estimate wording, quantity and rate are entered on the phone.
- PDF can be previewed, shared/emailed, or sent to Android printing.

## Formula verification

The unit test reproduces Estimate No. 8632 from the supplied workbook:

- Subtotal: R125,640.00
- VAT 15%: R18,846.00
- Total: R144,486.00

## Build APK on GitHub

The workflow `.github/workflows/build-apk.yml` builds the debug APK automatically on every push to `main` and can also be run manually from GitHub Actions. The output artifact is named `ImpiloQuotes-debug-apk`.

## Android project

- Package: `za.co.impilodrilling.quotes`
- Minimum Android: 8.0 (API 26)
- Compile/target SDK: 35
- Java: 17
