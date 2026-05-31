package kr.co.awesomelead.groupware_backend.domain.payslip.service;

import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PayslipPdfInfoExtractor {

    private static final List<Pattern> NAME_PATTERNS =
            List.of(
                    Pattern.compile(
                            "(?:성명|이름|사원명|직원명)\\s*[:：]?\\s*([가-힣A-Za-z\\s]{1,40}?)(?=\\s*(?:생년월일|DOB|Birth\\s*Date|부\\s*서|직\\s*급|$))",
                            Pattern.CASE_INSENSITIVE));

    private static final List<Pattern> BIRTH_DATE_PATTERNS =
            List.of(
                    Pattern.compile(
                            "(?:생년월일|DOB|Birth\\s*Date)\\s*[:：]?\\s*([0-9]{8}|[0-9]{4}\\s*[./-]\\s*[0-9]{1,2}\\s*[./-]\\s*[0-9]{1,2})",
                            Pattern.CASE_INSENSITIVE));

    public PayslipPersonalInfo extract(MultipartFile file) {
        String text = extractText(file);
        return extractFromText(text);
    }

    public PayslipPersonalInfo extractFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
        }
        String text = Normalizer.normalize(rawText, Normalizer.Form.NFC);

        String name = extractName(text);
        LocalDate birthDate = extractBirthDate(text);

        return new PayslipPersonalInfo(name, birthDate);
    }

    public boolean isNameLikelyCorrupted(String name) {
        String normalized = normalizeName(name).replaceAll("\\s+", "");
        if (normalized.length() < 2) {
            return true;
        }

        return !normalized.toLowerCase(Locale.ROOT).matches("^[가-힣a-z]+$");
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String rawText = textStripper.getText(document);
            if (rawText == null || rawText.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
            }
            return rawText;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
        }
    }

    private String extractName(String text) {
        for (Pattern pattern : NAME_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String candidate = matcher.group(1).trim();
                String normalized = normalizeName(candidate);
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }
        throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
    }

    private LocalDate extractBirthDate(String text) {
        for (Pattern pattern : BIRTH_DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String raw = matcher.group(1);
                LocalDate parsed = parseDate(raw);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
    }

    private LocalDate parseDate(String raw) {
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        if (digitsOnly.length() != 8) {
            return null;
        }

        try {
            return LocalDate.parse(digitsOnly, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFC).replaceAll("\\s+", " ").trim();
    }

    public record PayslipPersonalInfo(String name, LocalDate birthDate) {}
}
