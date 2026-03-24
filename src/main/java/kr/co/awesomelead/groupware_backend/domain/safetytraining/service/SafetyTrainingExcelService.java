package kr.co.awesomelead.groupware_backend.domain.safetytraining.service;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationMethod;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SafetyTrainingExcelService {

    private static final String TEMPLATE_PATH = "templates/safety-training-template.xlsx";

    public byte[] buildPreviewExcel(
            SafetyTrainingSessionCreateRequestDto requestDto,
            String educationDateText,
            List<User> attendees,
            String instructorName) {
        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

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

    private void fillEducationMethodsCheckboxes(Sheet sheet, List<SafetyEducationMethod> selectedMethods) {
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
                        sheet, row, labelCell.getRowIndex(), labelCell.getColumnIndex(), labelCell.getColumnIndex() + 1);

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
        Row headerRow = sheet.getRow(13);
        if (headerRow == null) {
            return;
        }

        int perBlockRows = 15;
        int index = 0;

        for (int col = headerRow.getFirstCellNum(); col <= headerRow.getLastCellNum(); col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null || !"성명".equals(normalize(cell.getStringCellValue()))) {
                continue;
            }

            for (int r = 14; r < 14 + perBlockRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    row = sheet.createRow(r);
                }
                Cell nameCell = row.getCell(col);
                if (nameCell == null) {
                    nameCell = row.createCell(col, CellType.STRING);
                }

                if (index < attendees.size()) {
                    nameCell.setCellValue(defaultString(attendees.get(index).getNameKor()));
                    index++;
                } else {
                    nameCell.setCellValue("");
                }
            }
        }
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
                        sheet, row, labelCell.getRowIndex(), labelCell.getColumnIndex(), preferredStartCol);
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

            String text = cell.getCellType() == CellType.STRING ? normalize(cell.getStringCellValue()) : "";
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

    private String isSelected(List<SafetyEducationMethod> selectedMethods, SafetyEducationMethod method) {
        return selectedMethods.contains(method) ? "■" : "□";
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
}
