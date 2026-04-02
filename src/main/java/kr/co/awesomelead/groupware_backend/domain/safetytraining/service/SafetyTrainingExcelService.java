package kr.co.awesomelead.groupware_backend.domain.safetytraining.service;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationMethod;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SafetyTrainingExcelService {

    private static final String TEMPLATE_PATH = "templates/safety-training-template.xlsx";
    private static final int EDUCATION_CLASSIFICATION_START_ROW_INDEX = 3; // excel row 4
    private static final int EDUCATION_CLASSIFICATION_END_ROW_INDEX = 6; // excel row 7
    private static final float[] EDUCATION_CLASSIFICATION_MIN_ROW_HEIGHTS =
            new float[] {30.0f, 16.0f, 16.0f, 24.0f};
    private static final int ATTENDEE_HEADER_ROW_INDEX = 13;
    private static final int ATTENDEE_START_ROW_INDEX = 14;
    private static final int ATTENDEE_ROWS_PER_BLOCK = 15;
    private static final float DEFAULT_ATTENDEE_ROW_HEIGHT_IN_POINTS = 24.0f;

    public byte[] buildPreviewExcel(
            SafetyTrainingSessionCreateRequestDto requestDto,
            String educationDateText,
            List<User> attendees,
            String instructorName) {
        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            normalizeEducationClassificationLayout(sheet);
            fillEducationTypeCheckboxes(sheet, requestDto.getEducationType());
            fillEducationMethodsCheckboxes(sheet, requestDto.getEducationMethods());

            fillCellByLabel(sheet, "교육일시", 2, educationDateText);
            fillCellByLabel(sheet, "교육내용", 2, defaultString(requestDto.getEducationContent()));
            fillCellByLabel(sheet, "교육 장소", 2, requestDto.getPlace());
            fillCellByLabel(sheet, "교육 실시자", 2, instructorName + " (인)");

            fillCountByHeader(sheet, "교육대상", attendees.size());
            fillCountByHeader(sheet, "교육참석", 0);
            fillCountByHeader(sheet, "교육 미참석", 0);

            fillAttendeeNames(sheet, attendees);

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("안전보건 엑셀 미리보기 생성 중 오류가 발생했습니다.", e);
        }
    }

    public byte[] buildSessionReportExcel(
            SafetyTrainingSession session,
            List<SafetyEducationMethod> educationMethods,
            List<SafetyTrainingSessionAttendee> attendees,
            Map<Long, byte[]> signatureImagesByUserId) {
        List<SafetyTrainingSessionAttendee> attendeeRows =
                attendees == null ? Collections.emptyList() : attendees;
        Map<Long, byte[]> signatures =
                signatureImagesByUserId == null ? Collections.emptyMap() : signatureImagesByUserId;

        int attendedCount =
                (int)
                        attendeeRows.stream()
                                .filter(it -> it.getStatus() == SafetyTrainingAttendeeStatus.SIGNED)
                                .count();
        int absentCount =
                (int)
                        attendeeRows.stream()
                                .filter(it -> it.getStatus() == SafetyTrainingAttendeeStatus.ABSENT)
                                .count();

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            normalizeEducationClassificationLayout(sheet);
            fillEducationTypeCheckboxes(sheet, session.getEducationType());
            fillEducationMethodsCheckboxes(sheet, educationMethods);

            fillCellByLabel(sheet, "교육일시", 2, defaultString(session.getEducationDateText()));
            fillCellByLabel(sheet, "교육내용", 2, defaultString(session.getEducationContent()));
            fillCellByLabel(sheet, "교육 장소", 2, defaultString(session.getPlace()));
            fillCellByLabel(
                    sheet,
                    "교육 실시자",
                    2,
                    defaultString(session.getInstructorNameSnapshot()) + " (인)");
            fillCellByLabel(sheet, "교육 미참석 사유", 2, defaultString(session.getAbsentReasonSummary()));

            fillCountByHeader(sheet, "교육대상", attendeeRows.size());
            fillCountByHeader(sheet, "교육참석", attendedCount);
            fillCountByHeader(sheet, "교육 미참석", absentCount);

            fillAttendeeNamesAndSignatures(workbook, sheet, attendeeRows, signatures);

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("안전보건 엑셀 보고서 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void fillEducationTypeCheckboxes(Sheet sheet, SafetyEducationType selected) {
        Map<SafetyEducationType, String> map = new HashMap<>();
        map.put(SafetyEducationType.REGULAR, "정기교육");
        map.put(SafetyEducationType.HIRING, "채용시");
        map.put(SafetyEducationType.JOB_CHANGE, "작업내용 변경시");
        map.put(SafetyEducationType.SPECIAL, "특별교육");
        map.put(SafetyEducationType.MSDS, "MSDS교육");

        for (Map.Entry<SafetyEducationType, String> entry : map.entrySet()) {
            Cell cell = findCellContains(sheet, entry.getValue());
            if (cell == null) {
                continue;
            }
            String mark = entry.getKey() == selected ? "■" : "□";
            String base = entry.getValue();
            cell.setCellValue(base + mark);
        }
    }

    private void fillEducationMethodsCheckboxes(
            Sheet sheet, List<SafetyEducationMethod> selectedMethods) {
        Cell labelCell = findCellContains(sheet, "교육방법");
        if (labelCell == null) {
            return;
        }

        Row row = sheet.getRow(labelCell.getRowIndex());
        if (row == null) {
            row = sheet.createRow(labelCell.getRowIndex());
        }

        Cell targetCell =
                resolveValueCellRightOfLabel(
                        sheet,
                        row,
                        labelCell.getRowIndex(),
                        labelCell.getColumnIndex(),
                        labelCell.getColumnIndex() + 1);

        String methodsText =
                String.format(
                        "1. 강의%s   2. 시청각%s   3. 현장 교육%s   4. 시범 실습%s   5. 견학%s   6. 역할연기%s",
                        isSelected(selectedMethods, SafetyEducationMethod.LECTURE),
                        isSelected(selectedMethods, SafetyEducationMethod.AUDIOVISUAL),
                        isSelected(selectedMethods, SafetyEducationMethod.FIELD_TRAINING),
                        isSelected(selectedMethods, SafetyEducationMethod.DEMONSTRATION),
                        isSelected(selectedMethods, SafetyEducationMethod.TOUR),
                        isSelected(selectedMethods, SafetyEducationMethod.ROLE_PLAY));

        targetCell.setCellValue(methodsText);
    }

    private void fillCountByHeader(Sheet sheet, String headerText, int value) {
        Cell headerCell = findCellExact(sheet, headerText);
        if (headerCell == null) {
            return;
        }
        Row valueRow = sheet.getRow(headerCell.getRowIndex() + 1);
        if (valueRow == null) {
            valueRow = sheet.createRow(headerCell.getRowIndex() + 1);
        }
        Cell valueCell = valueRow.getCell(headerCell.getColumnIndex());
        if (valueCell == null) {
            valueCell = valueRow.createCell(headerCell.getColumnIndex(), CellType.NUMERIC);
        }
        valueCell.setCellValue(value);
    }

    private void fillAttendeeNames(Sheet sheet, List<User> attendees) {
        normalizeAttendeeGridLayout(sheet);

        Row headerRow = sheet.getRow(ATTENDEE_HEADER_ROW_INDEX);
        if (headerRow == null) {
            return;
        }

        List<Integer> nameColumns = findNameColumns(headerRow);
        if (nameColumns.isEmpty()) {
            return;
        }

        CellStyle canonicalNameStyle =
                buildCanonicalNameCellStyle(sheet.getWorkbook(), sheet, nameColumns.get(0));

        int index = 0;
        for (int col : nameColumns) {

            for (int r = ATTENDEE_START_ROW_INDEX;
                    r < ATTENDEE_START_ROW_INDEX + ATTENDEE_ROWS_PER_BLOCK;
                    r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    row = sheet.createRow(r);
                }
                Cell nameCell = row.getCell(col);
                if (nameCell == null) {
                    nameCell = row.createCell(col, CellType.STRING);
                }
                applyNameCellStyle(nameCell, canonicalNameStyle);

                if (index < attendees.size()) {
                    nameCell.setCellValue(normalizeDisplayName(attendees.get(index).getNameKor()));
                    index++;
                } else {
                    nameCell.setCellValue("");
                }
            }
        }
    }

    private void fillAttendeeNamesAndSignatures(
            XSSFWorkbook workbook,
            Sheet sheet,
            List<SafetyTrainingSessionAttendee> attendees,
            Map<Long, byte[]> signatureImagesByUserId) {
        normalizeAttendeeGridLayout(sheet);

        Row headerRow = sheet.getRow(ATTENDEE_HEADER_ROW_INDEX);
        if (headerRow == null) {
            return;
        }

        List<Integer> nameColumns = findNameColumns(headerRow);
        if (nameColumns.isEmpty()) {
            return;
        }

        CellStyle canonicalNameStyle =
                buildCanonicalNameCellStyle(workbook, sheet, nameColumns.get(0));

        int index = 0;
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper creationHelper = workbook.getCreationHelper();

        for (int col : nameColumns) {

            int signatureCol = resolveSignatureColumn(headerRow, col);

            for (int r = ATTENDEE_START_ROW_INDEX;
                    r < ATTENDEE_START_ROW_INDEX + ATTENDEE_ROWS_PER_BLOCK;
                    r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    row = sheet.createRow(r);
                }
                Cell nameCell = row.getCell(col);
                if (nameCell == null) {
                    nameCell = row.createCell(col, CellType.STRING);
                }
                applyNameCellStyle(nameCell, canonicalNameStyle);

                if (index < attendees.size()) {
                    SafetyTrainingSessionAttendee attendee = attendees.get(index++);
                    nameCell.setCellValue(normalizeDisplayName(attendee.getUser().getNameKor()));

                    byte[] signatureBytes = signatureImagesByUserId.get(attendee.getUser().getId());
                    if (signatureBytes != null && signatureBytes.length > 0) {
                        addSignatureImage(
                                workbook,
                                sheet,
                                drawing,
                                creationHelper,
                                signatureBytes,
                                signatureCol,
                                r);
                    }
                } else {
                    nameCell.setCellValue("");
                }
            }
        }
    }

    /** 상단 교육 구분 표의 행 높이를 최소값으로 보정한다. 폰트 대체/뷰어 차이로 줄바꿈 텍스트가 잘리는 현상을 줄이기 위한 정규화 단계다. */
    private void normalizeEducationClassificationLayout(Sheet sheet) {
        int rowIndex = EDUCATION_CLASSIFICATION_START_ROW_INDEX;
        for (float minHeight : EDUCATION_CLASSIFICATION_MIN_ROW_HEIGHTS) {
            if (rowIndex > EDUCATION_CLASSIFICATION_END_ROW_INDEX) {
                break;
            }
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            row.setZeroHeight(false);
            if (row.getHeightInPoints() < minHeight) {
                row.setHeightInPoints(minHeight);
            }
            rowIndex++;
        }
    }

    /** 참석자 표(번호/성명/서명)의 행 높이/컬럼 폭을 블록 간 동일하게 고정한다. 템플릿/뷰어 차이로 셀 크기가 들쭉날쭉해지는 현상을 줄이기 위한 정규화 단계다. */
    private void normalizeAttendeeGridLayout(Sheet sheet) {
        Row headerRow = sheet.getRow(ATTENDEE_HEADER_ROW_INDEX);
        if (headerRow == null) {
            return;
        }

        float targetRowHeight = DEFAULT_ATTENDEE_ROW_HEIGHT_IN_POINTS;
        Row firstDataRow = sheet.getRow(ATTENDEE_START_ROW_INDEX);
        if (firstDataRow != null && firstDataRow.getHeightInPoints() > 0) {
            targetRowHeight = firstDataRow.getHeightInPoints();
        }

        for (int r = ATTENDEE_START_ROW_INDEX;
                r < ATTENDEE_START_ROW_INDEX + ATTENDEE_ROWS_PER_BLOCK;
                r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                row = sheet.createRow(r);
            }
            row.setZeroHeight(false);
            row.setHeightInPoints(targetRowHeight);
        }

        List<Integer> noCols = new ArrayList<>();
        List<Integer> nameCols = new ArrayList<>();
        List<Integer> signCols = new ArrayList<>();
        int noWidth = -1;
        int signWidth = -1;

        for (int col = headerRow.getFirstCellNum(); col <= headerRow.getLastCellNum(); col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null || cell.getCellType() != CellType.STRING) {
                continue;
            }
            String text = normalize(cell.getStringCellValue());
            if ("NO".equalsIgnoreCase(text)) {
                noCols.add(col);
                if (noWidth < 0) {
                    noWidth = sheet.getColumnWidth(col);
                }
                continue;
            }
            if ("성명".equals(text)) {
                nameCols.add(col);
                continue;
            }
            if ("서명".equals(text)) {
                signCols.add(col);
                if (signWidth < 0) {
                    signWidth = sheet.getColumnWidth(col);
                }
            }
        }

        if (noWidth > 0) {
            for (int col : noCols) {
                sheet.setColumnWidth(col, noWidth);
            }
        }
        normalizeNameColumnWidths(sheet, nameCols);
        if (signWidth > 0) {
            for (int col : signCols) {
                sheet.setColumnWidth(col, signWidth);
            }
        }
    }

    /** 성명 칸 폭 정규화: 첫 번째 성명 칸의 "병합 포함 실폭"을 기준으로, 단일열(비병합) 성명 칸이 과도하게 좁은 경우만 확장한다. */
    private void normalizeNameColumnWidths(Sheet sheet, List<Integer> nameCols) {
        if (nameCols == null || nameCols.isEmpty()) {
            return;
        }

        int targetWidth = resolveHeaderRegionWidth(sheet, nameCols.get(0));
        if (targetWidth <= 0) {
            return;
        }

        for (int col : nameCols) {
            CellRangeAddress region = findMergedRegion(sheet, ATTENDEE_HEADER_ROW_INDEX, col);
            int span = region == null ? 1 : (region.getLastColumn() - region.getFirstColumn() + 1);

            // 병합되지 않은 좁은 성명 칸(H 등)만 기준 폭까지 확장한다.
            if (span == 1 && sheet.getColumnWidth(col) < targetWidth) {
                sheet.setColumnWidth(col, targetWidth);
            }
        }
    }

    private int resolveHeaderRegionWidth(Sheet sheet, int col) {
        CellRangeAddress region = findMergedRegion(sheet, ATTENDEE_HEADER_ROW_INDEX, col);
        if (region == null) {
            return sheet.getColumnWidth(col);
        }

        int width = 0;
        for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
            width += sheet.getColumnWidth(c);
        }
        return width;
    }

    private int resolveSignatureColumn(Row headerRow, int nameCol) {
        int maxCol = headerRow.getLastCellNum();
        for (int col = nameCol + 1; col <= Math.min(nameCol + 4, maxCol); col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null || cell.getCellType() != CellType.STRING) {
                continue;
            }
            if (normalize(cell.getStringCellValue()).contains("서명")) {
                return col;
            }
        }
        return nameCol + 1;
    }

    private List<Integer> findNameColumns(Row headerRow) {
        List<Integer> columns = new ArrayList<>();
        for (int col = headerRow.getFirstCellNum(); col <= headerRow.getLastCellNum(); col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null || cell.getCellType() != CellType.STRING) {
                continue;
            }
            if ("성명".equals(normalize(cell.getStringCellValue()))) {
                columns.add(col);
            }
        }
        return columns;
    }

    private CellStyle buildCanonicalNameCellStyle(Workbook workbook, Sheet sheet, int nameCol) {
        Row refRow = sheet.getRow(ATTENDEE_START_ROW_INDEX);
        Cell refCell = refRow == null ? null : refRow.getCell(nameCol);
        CellStyle style = workbook.createCellStyle();
        if (refCell != null && refCell.getCellStyle() != null) {
            style.cloneStyleFrom(refCell.getCellStyle());
        }
        // 좁은 성명 칸에서도 텍스트가 잘리지 않도록 줄바꿈을 허용하고,
        // 글자가 과도하게 작아지지 않도록 shrink-to-fit는 비활성화한다.
        style.setWrapText(true);
        style.setShrinkToFit(false);
        return style;
    }

    /** 성명 칸 스타일은 블록별 차이가 생기지 않도록 단일 기준 스타일로 통일한다. */
    private void applyNameCellStyle(Cell nameCell, CellStyle canonicalStyle) {
        if (canonicalStyle == null) {
            return;
        }
        nameCell.setCellStyle(canonicalStyle);
    }

    private void addSignatureImage(
            XSSFWorkbook workbook,
            Sheet sheet,
            Drawing<?> drawing,
            CreationHelper creationHelper,
            byte[] signatureBytes,
            int col,
            int row) {
        int pictureType = detectPictureType(signatureBytes);
        int pictureIndex = workbook.addPicture(signatureBytes, pictureType);

        ClientAnchor anchor = creationHelper.createClientAnchor();
        CellRangeAddress mergedRegion = findMergedRegion(sheet, row, col);
        if (mergedRegion != null) {
            anchor.setCol1(mergedRegion.getFirstColumn());
            anchor.setRow1(mergedRegion.getFirstRow());
            anchor.setCol2(mergedRegion.getLastColumn() + 1);
            anchor.setRow2(mergedRegion.getLastRow() + 1);
        } else {
            anchor.setCol1(col);
            anchor.setRow1(row);
            anchor.setCol2(col + 1);
            anchor.setRow2(row + 1);
        }
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        drawing.createPicture(anchor, pictureIndex);
    }

    private int detectPictureType(byte[] imageBytes) {
        if (imageBytes.length >= 8
                && (imageBytes[0] & 0xFF) == 0x89
                && (imageBytes[1] & 0xFF) == 0x50
                && (imageBytes[2] & 0xFF) == 0x4E
                && (imageBytes[3] & 0xFF) == 0x47) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (imageBytes.length >= 2
                && (imageBytes[0] & 0xFF) == 0xFF
                && (imageBytes[1] & 0xFF) == 0xD8) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        return Workbook.PICTURE_TYPE_PNG;
    }

    private void fillCellByLabel(Sheet sheet, String label, int offsetCol, String value) {
        Cell labelCell = findCellContains(sheet, label);
        if (labelCell == null) {
            return;
        }
        Row row = sheet.getRow(labelCell.getRowIndex());
        if (row == null) {
            row = sheet.createRow(labelCell.getRowIndex());
        }
        int preferredStartCol = labelCell.getColumnIndex() + Math.max(offsetCol, 1);
        Cell target =
                resolveValueCellRightOfLabel(
                        sheet,
                        row,
                        labelCell.getRowIndex(),
                        labelCell.getColumnIndex(),
                        preferredStartCol);
        target.setCellValue(defaultString(value));
    }

    private Cell resolveValueCellRightOfLabel(
            Sheet sheet, Row row, int rowIndex, int labelCol, int preferredStartCol) {
        int startCol = preferredStartCol;
        CellRangeAddress labelMerged = findMergedRegion(sheet, rowIndex, labelCol);
        if (labelMerged != null) {
            startCol = Math.max(startCol, labelMerged.getLastColumn() + 1);
        }

        // If the intended value start column is inside a merged range,
        // always write into that merged range's top-left cell.
        CellRangeAddress valueMerged = findMergedRegion(sheet, rowIndex, startCol);
        if (valueMerged != null) {
            int topLeftCol = valueMerged.getFirstColumn();
            Cell mergedTopLeftCell = row.getCell(topLeftCol);
            if (mergedTopLeftCell == null) {
                mergedTopLeftCell = row.createCell(topLeftCol, CellType.STRING);
            }
            return mergedTopLeftCell;
        }

        int lastCol = Math.max(row.getLastCellNum(), (short) (startCol + 1));
        for (int col = startCol; col <= lastCol; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col, CellType.STRING);
                return cell;
            }

            String text =
                    cell.getCellType() == CellType.STRING
                            ? normalize(cell.getStringCellValue())
                            : "";
            if (text.isEmpty()) {
                return cell;
            }
        }

        int newCol = lastCol + 1;
        return row.createCell(newCol, CellType.STRING);
    }

    private CellRangeAddress findMergedRegion(Sheet sheet, int rowIndex, int colIndex) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(rowIndex, colIndex)) {
                return region;
            }
        }
        return null;
    }

    private String isSelected(
            List<SafetyEducationMethod> selectedMethods, SafetyEducationMethod method) {
        return selectedMethods != null && selectedMethods.contains(method) ? "■" : "□";
    }

    private Cell findCellContains(Sheet sheet, String keyword) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String text = normalize(cell.getStringCellValue());
                if (text.contains(normalize(keyword))) {
                    return cell;
                }
            }
        }
        return null;
    }

    private Cell findCellExact(Sheet sheet, String keyword) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                if (normalize(cell.getStringCellValue()).equals(normalize(keyword))) {
                    return cell;
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\n", "").replace(" ", "").trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalizeDisplayName(String value) {
        return defaultString(value).replace("\r", " ").replace("\n", " ").trim();
    }
}
