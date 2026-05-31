package kr.co.awesomelead.groupware_backend.domain.payslip.service;

import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayslipOcrInfoExtractor {

    private final PayslipPdfInfoExtractor payslipPdfInfoExtractor;

    @Value("${payslip.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${payslip.ocr.tesseract-command:tesseract}")
    private String tesseractCommand;

    @Value("${payslip.ocr.language:kor+eng}")
    private String tesseractLanguage;

    @Value("${payslip.ocr.dpi:300}")
    private int renderDpi;

    @Value("${payslip.ocr.psm:6}")
    private int tesseractPsm;

    @Value("${payslip.ocr.oem:1}")
    private int tesseractOem;

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    public PayslipPdfInfoExtractor.PayslipPersonalInfo extract(MultipartFile file) {
        if (!ocrEnabled) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
        }

        String ocrText = extractOcrText(file);
        return payslipPdfInfoExtractor.extractFromText(ocrText);
    }

    private String extractOcrText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.getNumberOfPages() == 0) {
                throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder mergedText = new StringBuilder();

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage pageImage =
                        renderer.renderImageWithDPI(pageIndex, renderDpi, ImageType.RGB);
                Path imagePath = Files.createTempFile("payslip-ocr-", ".png");
                try {
                    ImageIO.write(pageImage, "png", imagePath.toFile());
                    mergedText.append(runTesseract(imagePath));
                    mergedText.append("\n");
                } finally {
                    Files.deleteIfExists(imagePath);
                }
            }

            if (mergedText.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
            }
            return mergedText.toString();
        } catch (IOException e) {
            log.warn("급여명세서 OCR 추출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
        }
    }

    private String runTesseract(Path imagePath) {
        List<String> command = new ArrayList<>();
        command.add(tesseractCommand);
        command.add(imagePath.toString());
        command.add("stdout");
        command.add("-l");
        command.add(tesseractLanguage);
        command.add("--oem");
        command.add(String.valueOf(tesseractOem));
        command.add("--psm");
        command.add(String.valueOf(tesseractPsm));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String outputText =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Tesseract 실행 실패(exitCode={}): {}", exitCode, outputText);
                throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
            }
            return outputText;
        } catch (IOException e) {
            log.warn("Tesseract 실행 중 I/O 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
        }
    }
}
