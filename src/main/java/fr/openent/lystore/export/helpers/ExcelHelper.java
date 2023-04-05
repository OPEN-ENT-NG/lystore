package fr.openent.lystore.export.helpers;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.RegionUtil;

public class ExcelHelper extends ExcelStyles{
    private Workbook wb;
    private Sheet sheet;


    protected static Logger log = LoggerFactory.getLogger(ExcelHelper.class);

    private DataFormat format;
    public static final String totalLabel = "Total";
    public static final String sumLabel = "Somme";

    public ExcelHelper(Workbook wb, Sheet sheet) {
        super(wb,sheet);
        this.wb = wb;
        this.sheet = sheet;
        format = wb.createDataFormat();
        format.getFormat("#.#");
    }


    /**
     * Init all the stles of the sheet
     */

    public void setBold(Cell cell) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        cell.setCellStyle(style);
    }

    public void setDefaultFont() {
        Font defaultFont = wb.createFont();
        defaultFont.setColor(IndexedColors.BLACK.getIndex());
        defaultFont.setBold(false);
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        CellStyle style = cell.getCellStyle();
        style.setFont(defaultFont);
        cell.setCellStyle(style);
    }

    public void setTitle(String title) {
        Row row = sheet.createRow(2);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        this.setBold(cell);
    }

    public void setSubTitle(String subtitle) {
        Row row = sheet.createRow(4);
        Cell cell = row.createCell(0);
        cell.setCellValue(subtitle);
        this.setBold(cell);
    }

    public void setCPNumber(String number) {
            setCPNumber(number,1,0);
    }

    /**
     * set at a particular line and column
     *
     * @param number
     * @param line
     * @param column
     */
    public void setCPNumber(String number, int line, int column) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(column);
            cell.setCellValue("N° CP " + number);
            this.setBold(cell);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(column);
            cell.setCellValue("N° CP " + number);
            this.setBold(cell);
        }
    }
    public void setInstructionNumber(String number) {
        setInstructionNumber(number,1,0);

    }
    public void setInstructionNumber(String number, int line, int column) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(column);
            cell.setCellValue("Numéro du rapport : " + number);
            this.setBold(cell);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(column);
            cell.setCellValue("Numéro du rapport : " + number);
            this.setBold(cell);
        }
    }

    /**
     * Adding borders to a merged region
     *
     * @param merge
     * @param sheet
     */
    public void setRegionHeader(CellRangeAddress merge, Sheet sheet) {
        RegionUtil.setBorderTop(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, merge, sheet);

    }

    public void setRegionHeaderStyle(CellRangeAddress merge, Sheet sheet, CellStyle style) {
        RegionUtil.setBorderTop(style.getBorderTop(), merge, sheet);
        RegionUtil.setBorderBottom(style.getBorderBottom(), merge, sheet);
        RegionUtil.setBorderRight(style.getBorderRight(), merge, sheet);
        RegionUtil.setBorderLeft(style.getBorderLeft(), merge, sheet);
    }

    public void setRegionUnderscoreHeader(CellRangeAddress merge, Sheet sheet) {
        RegionUtil.setBorderBottom(BorderStyle.THIN, merge, sheet);
    }

    /**
     * @param cellColumn x
     * @param line       y
     * @param data       data to insert (any type of Object)
     * @param style      cell's style
     */
    public void insertWithStyle(int cellColumn, int line, Object data, CellStyle style) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);

            setDataInCell(cell, data);
            cell.setCellStyle(style);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            setDataInCell(cell, data);
            cell.setCellStyle(style);
        }
    }

    private void setDataInCell(Cell cell, Object data) {
        switch (data.getClass().getName().replace("java.lang.", "")) {
            case "String":
                cell.setCellValue((String) data);
                break;
            case "Double":
                cell.setCellValue((Double) data);
                break;
            case "Integer":
                cell.setCellValue((Integer) data);
                break;
            case "Long":
                cell.setCellValue((Long) data);
                break;
            default:
                cell.setCellValue(data.toString());
                break;
        }
    }


    public void insertFormula(int cellColumn, int line, String data) {
        insertFormulaWithStyle(cellColumn, line, data,currencyStyle);
    }

    public void insertFormulaWithStyle(int cellColumn, int line, String data, CellStyle style) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellFormula(data);
            cell.setCellStyle(style);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellFormula(data);
            cell.setCellStyle(style);
        }
    }

    /**
     * insert a cell with doulbe in the tab
     *
     * @param cellColumn
     * @param line
     * @param data       data to insert
     */
    public void insertCellTabDouble(int cellColumn, int line, Double data) {
        insertWithStyle(cellColumn, line, data, tabNumeralStyle);
    }


    public void insertCellTabStringRight(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, tabStringStyleRight);
    }

    /**
     * insert a label in a tab at line,column
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertLabel(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, labelStyle);
    }

    public void insertLabelBold(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, labelBoldStyle);
    }

    public void insertLabelHead(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, labelHeadStyle);
    }

    public void insertHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, headCellStyle);
    }

    public void insertBlackOnGreenHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blackOnGreenHeaderStyle);
    }

    public void insertDoubleYellow(int cellColumn, int line, Double data) {
        insertWithStyle(cellColumn, line, data, doubleOnYellowStyle);
    }

    /**
     * Insert a price in a array
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTabDoubleWithPrice(int cellColumn, int line, Double data) {
        insertWithStyle(cellColumn, line, data, tabCurrencyStyle);

    }

    /**
     * insert a cell in the tab
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTab(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, tabStringStyle);
    }

    public void insertCellTab(int cellColumn, int line, Object data) {
        insertWithStyle(cellColumn, line, data, tabStringStyle);
    }

    /**
     * insert a cell with an int in the tab
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTabInt(int cellColumn, int line, int data) {
        insertWithStyle(cellColumn, line, data, tabIntStyleRight);
    }

    public void insertCellTabCenterBold(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, tabStringStyleCenterBold);
    }

    /**
     * insert a header with yellow background
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertYellowHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, yellowHeader);
    }

    /**
     * insert a label with yellow background
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertYellowLabel(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, labelOnLightYellow);
    }


    /**
     * insert a cell in a tab ith blue background and white font
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertWhiteOnBlueTab(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, whiteOnBlueLabel);
    }

    public void insertLabelOnRed(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, LabelBlackOnRed);
    }

    /**
     * insert a header with blue background
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertTitleHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, titleHeaderStyle);
    }

    /**
     * insert a header with black police
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertBlackTitleHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blackTitleHeaderStyle);
    }

    /**
     * insert a header with black police without border
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertBlackTitleHeaderBorderless(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blackTitleHeaderBorderlessStyle);
    }

    public void insertBlackTitleHeaderBorderlessCenter(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blackTitleHeaderBorderlessCenteredStyle);
    }

    public void insertBlueTitleHeaderBorderlessCenter(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blueTitleHeaderBorderlessCenteredStyle);
    }

    public void insertBlueTitleHeaderBorderlessCenterDoubleCurrency(int cellColumn, int line, Double data) {
        insertWithStyle(cellColumn, line, data, blueTitleHeaderBorderlessCenteredCurrencyStyle);
    }

    /**
     * insert a header with blue police
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertBlueTitleHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blueTitleHeaderStyle);
    }


    /**
     * insert a data in an array wich will be centered in the cell
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTabCenter(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, tabStringStyleCenter);
    }

    public void insertCellTabBlue(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, blueTabStyle);
    }

    /**
     * insert an header with an underscore
     *
     * @param cellColumn
     * @param cellColumn
     * @param data
     */
    public void insertUnderscoreHeader(int cellColumn, int line, String data) {
        insertWithStyle(cellColumn, line, data, underscoreHeader);
    }

    public void insertStandardText(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(standardTextStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(standardTextStyle);
        }
    }

    /**
     * Set default style for a tab and init all non init cells of the tab
     *
     * @param columnStart
     * @param columnEnd
     * @param lineStart
     * @param lineEnd
     */
    public void fillTab(int columnStart, int columnEnd, int lineStart, int lineEnd) {
        fillTabWithStyle(columnStart, columnEnd, lineStart, lineEnd, tabNumeralStyle);
    }


    /**
     * Set specific style for a tab and init all non init cells of the tab
     *
     * @param columnStart
     * @param columnEnd
     * @param lineStart
     * @param lineEnd
     * @param style       style to insert
     */
    public void fillTabWithStyle(int columnStart, int columnEnd, int lineStart, int lineEnd, CellStyle style) {
        Row tab;
        Cell cell;
        for (int line = lineStart; line < lineEnd; line++) {
            try {
                tab = sheet.getRow(line);

                for (int column = columnStart; column < columnEnd; column++) {
                    try {
                        cell = tab.getCell(column);
                        cell.setCellStyle(style);
                    } catch (NullPointerException e) {
                        cell = tab.createCell(column);
                        cell.setCellStyle(style);
                    }
                }
            } catch (NullPointerException e) {
                tab = sheet.createRow(line);
                for (int column = columnStart; column < columnEnd; column++) {
                    try {
                        cell = tab.getCell(column);
                        cell.setCellStyle(style);
                    } catch (NullPointerException ee) {
                        cell = tab.createCell(column);
                        cell.setCellStyle(style);
                    }
                }
            }
        }
    }

    /**
     * set total of a column
     *
     * @param lineStart  start of the column
     * @param lineEnd    end of the column
     * @param column     number of the column
     * @param lineInsert line where the total is insert
     */
    public void setTotalX(int lineStart, int lineEnd, int column, int lineInsert) {
        setTotalX(lineStart, lineEnd, column, lineInsert, column);
    }

    /**
     * Set total of a line
     *
     * @param columnStart  start of the line
     * @param columnEnd    end of the line
     * @param line         number of the line to make total
     * @param columnInsert column where to insert the total
     */
    public void setTotalY(int columnStart, int columnEnd, int line, int columnInsert) {
        setTotalY(columnStart, columnEnd, line, columnInsert, line);
    }


    /**
     * Set total column and  line of a tab
     *
     * @param lineStart
     * @param lineEnd
     * @param columnStart
     * @param columnEnd
     */
    public void setTotal(int lineStart, int lineEnd, int columnStart, int columnEnd) {
        setTotal(lineStart, lineEnd, columnStart, columnEnd, lineEnd, columnEnd, tabCurrencyStyle);
    }

    public void setTotal(int lineStart, int lineEnd, int columnStart, int columnEnd, int lineInsert, int columnInsert, CellStyle style) {
        setTotalWithStyle(lineStart,lineEnd,columnStart,columnEnd,lineInsert,columnInsert,style,style,style);
    }


    /**
     * Get an excel address of a cell
     *
     * @param line
     * @param column
     * @return
     */
    public String getCellReference(int line, int column) {
        Row tab;
        Cell cell;
        tab = sheet.getRow(line);
        cell = tab.getCell(column);
        return new CellReference(cell).formatAsString();
    }


    /**
     * autosize the number of  columns of the page
     *
     * @param arrayLength number of columns to autosize
     */
    public void autoSize(int arrayLength) {
        try {
            for (int i = 0; i < arrayLength; i++) {
                sheet.autoSizeColumn(i);
            }
        }catch (Exception e){
            return;
        }
    }


    /**
     * set total of a column
     *
     * @param lineStart    start of the column
     * @param lineEnd      end of the column
     * @param column       number of the column
     * @param lineInsert   line where the total is insert
     * @param columnInsert column where the total will be insert
     */
    public void setTotalX(int lineStart, int lineEnd, int column, int lineInsert, int columnInsert) {
        setTotalXWithStyle(lineStart, lineEnd, column, lineInsert, columnInsert, tabCurrencyStyle);
    }

    public void setTotalXWithStyle(int lineStart, int lineEnd, int column, int lineInsert,
                                   int columnInsert, CellStyle style) {
        try {
            Row tab, tabStart, tabEnd;
            tabStart = sheet.getRow(lineStart);
            tabEnd = sheet.getRow(lineEnd);
            Cell cell, cellStartSum, cellEndSum;
            try {
                tab = sheet.getRow(lineInsert);
                cell = tab.createCell(columnInsert);
            } catch (NullPointerException e) {
                tab = sheet.createRow(lineInsert);
                cell = tab.createCell(columnInsert);
            }
            cell.setCellStyle(style);
            cell.setCellValue("total");
            cellStartSum = tabStart.getCell(column);
            cellEndSum = tabEnd.getCell(column);
            cell.setCellStyle(style);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        } catch (NullPointerException e) {
            log.error("Trying to sum a non init cell , init cells before calling this function x");
        }
    }


    public void setTotalXWithStyle(int lineStart, int lineEnd, int column, int lineInsert, CellStyle style) {
        setTotalXWithStyle(lineStart, lineEnd, column, lineInsert, column, style);
    }

    public void setRowBreak(int line) {
        sheet.setRowBreak(line);
    }

    /**
     * Set total of a line
     *
     * @param columnStart  start of the line
     * @param columnEnd    end of the line
     * @param line         number of the line to make total of
     * @param columnInsert column where to insert the total
     * @param lineInsert   line where to insert the total
     */
    public void setTotalY(int columnStart, int columnEnd, int line, int columnInsert, int lineInsert) {
        try {
            Row tab, tabInsert;
            tab = sheet.getRow(line);
            try {
                tabInsert = sheet.getRow(lineInsert);
            } catch (NullPointerException e) {
                tabInsert = sheet.createRow(lineInsert);
            }
            Cell cell, cellStartSum, cellEndSum;
            cell = tabInsert.createCell(columnInsert);
            cellStartSum = tab.getCell(columnStart);
            cellEndSum = tab.getCell(columnEnd);
            cell.setCellStyle(tabCurrencyStyle);
            cell.setCellValue("total");
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        } catch (NullPointerException e) {
            log.error("Trying to sum a non init cell , init cells before calling this function");
        }
    }

    public void setTotalWithStyle(int lineStart, int lineEnd, int columnStart, int columnEnd, int lineInsert, int columnInsert,
                                  CellStyle rowStyle,CellStyle columnStyle,CellStyle totalStyle) {
        Row tab, tabInsert, tabStart, tabEnd;
        Cell cell, cellStartSum, cellEndSum;
        // totalY
        tabStart = sheet.getRow(lineStart);
        tabEnd = sheet.getRow(lineEnd - 1);

        for (int i = lineStart; i < lineEnd; i++) {
            tab = sheet.getRow(i);
            cell = tab.createCell(columnEnd);
            cellStartSum = tab.getCell(columnStart);
            cellEndSum = tab.getCell(columnEnd - 1);
            cell.setCellStyle(rowStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        }
        //totalX
        tab = sheet.getRow(lineEnd);

        for (int i = columnStart; i < columnEnd; i++) {
            cell = tab.createCell(i);
            cell.setCellStyle(columnStyle);
            cell.setCellValue("total");

            cellStartSum = tabStart.getCell(i);
            cellEndSum = tabEnd.getCell(i);


            cell.setCellStyle(columnStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        }
        cellStartSum = tabStart.getCell(columnEnd);
        cellEndSum = tabEnd.getCell(columnEnd);

        try {
            tabInsert = sheet.getRow(lineInsert);
            cell = tabInsert.createCell(columnInsert);
            cell.setCellStyle(totalStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        } catch (Exception e) {
            tabInsert = sheet.createRow(lineInsert);
            cell = tabInsert.createCell(columnInsert);
            cell.setCellStyle(totalStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        }

    }
}
