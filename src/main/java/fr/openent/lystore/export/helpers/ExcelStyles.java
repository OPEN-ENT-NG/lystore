package fr.openent.lystore.export.helpers;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.*;

/**
 * All the styles used in the xlsx exports
 */
public abstract class ExcelStyles {
    private Workbook wb;
    private Sheet sheet;
    public final CellStyle headCellStyle;
    public final CellStyle labelStyle;
    public final CellStyle tabNumeralStyle;
    public final CellStyle tabStringStyleCenter;
    public final CellStyle tabStringStyleCenterBold;
    public final CellStyle tabStringStyle;
    public final CellStyle totalStyle;
    public final CellStyle labelHeadStyle;
    public final CellStyle currencyStyle;
    public final CellStyle tabCurrencyStyle;
    public final CellStyle titleHeaderStyle;
    public final CellStyle yellowHeader;
    public final CellStyle underscoreHeader;
    public final CellStyle blackTitleHeaderStyle;
    public final CellStyle blueTitleHeaderStyle;
    public final CellStyle tabStringStyleRight;
    public final CellStyle doubleOnYellowStyle;
    public final CellStyle whiteOnBlueLabel;
    public final CellStyle blackOnGreenHeaderStyle;
    public final CellStyle blueTabStyle;
    public final CellStyle blackTitleHeaderBorderlessStyle;
    public final CellStyle blackTitleHeaderBorderlessCenteredStyle;
    public final CellStyle blueTitleHeaderBorderlessCenteredStyle;
    public final CellStyle blueTitleHeaderBorderlessCenteredCurrencyStyle;
    public final CellStyle labelBoldStyle;
    public final CellStyle tabIntStyleCenterBold;
    public final CellStyle standardTextStyle;
    public final CellStyle blackOnBlueHeader;
    public final CellStyle yellowTab;
    public final CellStyle yellowTabPrice;
    public final CellStyle dateFormatStyle;
    public final CellStyle currencyFormatStyle;
    public final CellStyle numberFormatStyle;
    public final CellStyle labelOnGrey;
    public final CellStyle labelOnBlueGrey;
    public final CellStyle labelOnOrange;
    public final CellStyle labelOnLightGreen;
    public final CellStyle labelOnLimeGreen;
    public final CellStyle labelOnLightYellow;
    public final CellStyle labelOnYellow;
    public final CellStyle labelOnPink;
    public final CellStyle LabelBlackOnRed;
    public final CellStyle tabIntStyleRight;
    public final CellStyle tabIntRedBoldRight;
    public final CellStyle tabIntStyleRightBold;

    protected static Logger log = LoggerFactory.getLogger(ExcelHelper.class);

    private DataFormat format;
    public static final String totalLabel = "Total";
    public static final String sumLabel = "Somme";

    public ExcelStyles(Workbook wb, Sheet sheet) {
        this.wb = wb;
        this.sheet = sheet;
        this.headCellStyle = wb.createCellStyle();
        this.labelStyle = wb.createCellStyle();
        this.tabNumeralStyle = wb.createCellStyle();
        this.tabStringStyle = wb.createCellStyle();
        this.tabCurrencyStyle = wb.createCellStyle();
        this.currencyStyle = wb.createCellStyle();
        this.tabStringStyleCenter = wb.createCellStyle();
        this.tabStringStyleCenterBold = wb.createCellStyle();
        this.tabIntStyleRight = wb.createCellStyle();
        this.tabIntRedBoldRight = wb.createCellStyle();
        this.totalStyle = wb.createCellStyle();
        this.yellowHeader = wb.createCellStyle();
        this.labelOnLightYellow = wb.createCellStyle();
        this.labelOnYellow = wb.createCellStyle();
        this.underscoreHeader = wb.createCellStyle();
        this.blackTitleHeaderStyle = wb.createCellStyle();
        this.labelHeadStyle = wb.createCellStyle();
        this.titleHeaderStyle = wb.createCellStyle();
        this.blueTitleHeaderStyle = wb.createCellStyle();
        this.tabStringStyleRight = wb.createCellStyle();
        this.doubleOnYellowStyle = wb.createCellStyle();
        this.blackOnGreenHeaderStyle = wb.createCellStyle();
        this.whiteOnBlueLabel = wb.createCellStyle();
        this.LabelBlackOnRed = wb.createCellStyle();
        this.blueTabStyle = wb.createCellStyle();
        this.blackTitleHeaderBorderlessStyle = wb.createCellStyle();
        this.blackTitleHeaderBorderlessCenteredStyle = wb.createCellStyle();
        this.blueTitleHeaderBorderlessCenteredStyle = wb.createCellStyle();
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle = wb.createCellStyle();
        this.labelBoldStyle = wb.createCellStyle();
        this.tabIntStyleCenterBold = wb.createCellStyle();
        this.standardTextStyle = wb.createCellStyle();
        this.blackOnBlueHeader = wb.createCellStyle();
        this.yellowTab = wb.createCellStyle();
        this.yellowTabPrice = wb.createCellStyle();
        this.dateFormatStyle = wb.createCellStyle();
        this.currencyFormatStyle = wb.createCellStyle();
        this.numberFormatStyle = wb.createCellStyle();
        this.labelOnGrey = wb.createCellStyle();
        this.labelOnBlueGrey = wb.createCellStyle();
        this.labelOnOrange = wb.createCellStyle();
        this.labelOnLightGreen = wb.createCellStyle();
        this.labelOnLimeGreen = wb.createCellStyle();
        this.labelOnPink = wb.createCellStyle();
        this.tabIntStyleRightBold = wb.createCellStyle();
        format = wb.createDataFormat();
        format.getFormat("#.#");

        this.initStyles();

    }


    /**
     * Init all the stles of the sheet
     */
    private void initStyles() {
        //INIT HEader style
        Font headerFont = this.wb.createFont();
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setFontName("Calibri");
        headerFont.setBold(true);
        this.headCellStyle.setBorderLeft(BorderStyle.THIN);
        this.headCellStyle.setBorderRight(BorderStyle.THIN);
        this.headCellStyle.setBorderTop(BorderStyle.THIN);
        this.headCellStyle.setBorderBottom(BorderStyle.THIN);
        this.headCellStyle.setWrapText(true);
        this.headCellStyle.setAlignment(HorizontalAlignment.CENTER);
        this.headCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.headCellStyle.setFont(headerFont);

        //init LabelStyle
        Font labelFont = this.wb.createFont();
        labelFont.setFontHeightInPoints((short) 11);
        labelFont.setFontName("Calibri");
        labelFont.setBold(false);
        this.labelStyle.setBorderLeft(BorderStyle.THIN);
        this.labelStyle.setBorderRight(BorderStyle.THIN);
        this.labelStyle.setBorderTop(BorderStyle.THIN);
        this.labelStyle.setBorderBottom(BorderStyle.THIN);
        this.labelStyle.setWrapText(true);
        this.labelStyle.setAlignment(HorizontalAlignment.LEFT);
        this.labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        this.labelBoldStyle.setBorderLeft(BorderStyle.THIN);
        this.labelBoldStyle.setBorderRight(BorderStyle.THIN);
        this.labelBoldStyle.setBorderTop(BorderStyle.THIN);
        this.labelBoldStyle.setBorderBottom(BorderStyle.THIN);
        this.labelBoldStyle.setWrapText(true);
        this.labelBoldStyle.setAlignment(HorizontalAlignment.LEFT);
        this.labelBoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelBoldStyle.setFont(headerFont);

        //TotalStyle
        Font totalFont = this.wb.createFont();
        totalFont.setFontHeightInPoints((short) 11);
        totalFont.setFontName("Calibri");
        totalFont.setBold(true);

        this.totalStyle.setBorderLeft(BorderStyle.THIN);
        this.totalStyle.setBorderRight(BorderStyle.THIN);
        this.totalStyle.setBorderTop(BorderStyle.THIN);
        this.totalStyle.setBorderBottom(BorderStyle.THIN);
        this.totalStyle.setWrapText(true);
        this.totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.totalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.totalStyle.setFont(totalFont);
        this.totalStyle.setDataFormat(format.getFormat("#,##0.00"));

        //TabStyle
        Font tabFont = this.wb.createFont();
        tabFont.setFontHeightInPoints((short) 11);
        tabFont.setFontName("Calibri");
        tabFont.setBold(false);
        this.tabNumeralStyle.setBorderLeft(BorderStyle.THIN);
        this.tabNumeralStyle.setBorderRight(BorderStyle.THIN);
        this.tabNumeralStyle.setBorderTop(BorderStyle.THIN);
        this.tabNumeralStyle.setBorderBottom(BorderStyle.THIN);
        this.tabNumeralStyle.setWrapText(true);
        this.tabNumeralStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.tabNumeralStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabNumeralStyle.setFont(tabFont);
        this.tabNumeralStyle.setDataFormat(format.getFormat("#,##0.00"));
        //TabStyle

        this.tabStringStyle.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyle.setBorderRight(BorderStyle.THIN);
        this.tabStringStyle.setBorderTop(BorderStyle.THIN);
        this.tabStringStyle.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyle.setWrapText(true);
        this.tabStringStyle.setAlignment(HorizontalAlignment.LEFT);
        this.tabStringStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyle.setFont(tabFont);
        this.tabStringStyle.setDataFormat(format.getFormat("#,##0.00"));

        this.tabIntStyleRight.setBorderLeft(BorderStyle.THIN);
        this.tabIntStyleRight.setBorderRight(BorderStyle.THIN);
        this.tabIntStyleRight.setBorderTop(BorderStyle.THIN);
        this.tabIntStyleRight.setBorderBottom(BorderStyle.THIN);
        this.tabIntStyleRight.setWrapText(true);
        this.tabIntStyleRight.setAlignment(HorizontalAlignment.RIGHT);
        this.tabIntStyleRight.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabIntStyleRight.setFont(tabFont);
        this.tabIntStyleRight.setDataFormat(format.getFormat("#"));

        Font tabRedBoldFont = this.wb.createFont();
        tabRedBoldFont.setFontHeightInPoints((short) 11);
        tabRedBoldFont.setFontName("Calibri");
        tabRedBoldFont.setBold(true);
        tabRedBoldFont.setColor(IndexedColors.RED.getIndex());
        this.tabIntRedBoldRight.setBorderLeft(BorderStyle.THIN);
        this.tabIntRedBoldRight.setBorderRight(BorderStyle.THIN);
        this.tabIntRedBoldRight.setBorderTop(BorderStyle.THIN);
        this.tabIntRedBoldRight.setBorderBottom(BorderStyle.THIN);
        this.tabIntRedBoldRight.setWrapText(true);
        this.tabIntRedBoldRight.setAlignment(HorizontalAlignment.RIGHT);
        this.tabIntRedBoldRight.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabIntRedBoldRight.setFont(tabRedBoldFont);
        this.tabIntRedBoldRight.setDataFormat(format.getFormat("#"));

        this.tabStringStyleCenter.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyleCenter.setBorderRight(BorderStyle.THIN);
        this.tabStringStyleCenter.setBorderTop(BorderStyle.THIN);
        this.tabStringStyleCenter.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyleCenter.setWrapText(true);
        this.tabStringStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        this.tabStringStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyleCenter.setFont(tabFont);
        this.tabStringStyleCenter.setDataFormat(format.getFormat("#,##0.00"));

        this.tabIntStyleCenterBold.setBorderLeft(BorderStyle.THIN);
        this.tabIntStyleCenterBold.setBorderRight(BorderStyle.THIN);
        this.tabIntStyleCenterBold.setBorderTop(BorderStyle.THIN);
        this.tabIntStyleCenterBold.setBorderBottom(BorderStyle.THIN);
        this.tabIntStyleCenterBold.setWrapText(true);
        this.tabIntStyleCenterBold.setAlignment(HorizontalAlignment.CENTER);
        this.tabIntStyleCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabIntStyleCenterBold.setFont(headerFont);
        this.tabIntStyleCenterBold.setDataFormat(format.getFormat("#"));



        this.tabIntStyleRightBold.setBorderLeft(BorderStyle.THIN);
        this.tabIntStyleRightBold.setBorderRight(BorderStyle.THIN);
        this.tabIntStyleRightBold.setBorderTop(BorderStyle.THIN);
        this.tabIntStyleRightBold.setBorderBottom(BorderStyle.THIN);
        this.tabIntStyleRightBold.setWrapText(true);
        this.tabIntStyleRightBold.setAlignment(HorizontalAlignment.RIGHT);
        this.tabIntStyleRightBold.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabIntStyleRightBold.setFont(headerFont);
        this.tabIntStyleRightBold.setDataFormat(format.getFormat("#"));


        Font tabFontBold = this.wb.createFont();
        tabFontBold.setFontHeightInPoints((short) 11);
        tabFontBold.setFontName("Calibri");
        tabFontBold.setBold(true);
        this.tabStringStyleCenterBold.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyleCenterBold.setBorderRight(BorderStyle.THIN);
        this.tabStringStyleCenterBold.setBorderTop(BorderStyle.THIN);
        this.tabStringStyleCenterBold.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyleCenterBold.setWrapText(true);
        this.tabStringStyleCenterBold.setAlignment(HorizontalAlignment.CENTER);
        this.tabStringStyleCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyleCenterBold.setFont(tabFontBold);
        this.tabStringStyleCenterBold.setDataFormat(format.getFormat("#,##0.00"));


        this.tabStringStyleRight.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyleRight.setBorderRight(BorderStyle.THIN);
        this.tabStringStyleRight.setBorderTop(BorderStyle.THIN);
        this.tabStringStyleRight.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyleRight.setWrapText(true);
        this.tabStringStyleRight.setAlignment(HorizontalAlignment.RIGHT);
        this.tabStringStyleRight.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyleRight.setFont(tabFont);
        this.tabStringStyleRight.setDataFormat(format.getFormat("#,##0.00"));

        //LabelHeadStyle
        Font labelHeadFont = this.wb.createFont();
        labelHeadFont.setFontHeightInPoints((short) 12);
        labelHeadFont.setFontName("Calibri");
        labelHeadFont.setBold(true);
        this.labelHeadStyle.setWrapText(true);
        this.labelHeadStyle.setAlignment(HorizontalAlignment.LEFT);
        this.labelHeadStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelHeadStyle.setFont(labelHeadFont);

        this.currencyStyle.setWrapText(true);
        this.currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.currencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.currencyStyle.setFont(labelHeadFont);
        this.currencyStyle.setDataFormat(format.getFormat("#,##0.00 €"));

        this.tabCurrencyStyle.setWrapText(true);
        this.tabCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.tabCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabCurrencyStyle.setBorderLeft(BorderStyle.THIN);
        this.tabCurrencyStyle.setBorderRight(BorderStyle.THIN);
        this.tabCurrencyStyle.setBorderTop(BorderStyle.THIN);
        this.tabCurrencyStyle.setBorderBottom(BorderStyle.THIN);
        this.tabCurrencyStyle.setFont(totalFont);
        this.tabCurrencyStyle.setDataFormat(format.getFormat("#,##0.00 €"));

        //init LabelStyle
        Font titleHeadFont = this.wb.createFont();
        titleHeadFont.setFontHeightInPoints((short) 14);
        titleHeadFont.setFontName("Calibri");
        titleHeadFont.setBold(true);
        this.titleHeaderStyle.setWrapText(true);
        this.titleHeaderStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        this.titleHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.titleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.titleHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.titleHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.titleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.titleHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        this.titleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.titleHeaderStyle.setFont(titleHeadFont);

        this.blackTitleHeaderStyle.setWrapText(true);
        this.blackTitleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blackTitleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blackTitleHeaderStyle.setFont(titleHeadFont);

        Font blueTitleHeadFont = this.wb.createFont();
        blueTitleHeadFont.setFontHeightInPoints((short) 14);
        blueTitleHeadFont.setFontName("Calibri");
        blueTitleHeadFont.setBold(true);
        blueTitleHeadFont.setColor(IndexedColors.BLUE.getIndex());
        this.blueTitleHeaderStyle.setWrapText(true);
        this.blueTitleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blueTitleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blueTitleHeaderStyle.setFont(blueTitleHeadFont);


        Font blueTitleTabFont = this.wb.createFont();
        blueTitleTabFont.setFontHeightInPoints((short) 12);
        blueTitleTabFont.setFontName("Calibri");
        blueTitleTabFont.setBold(false);
        blueTitleTabFont.setColor(IndexedColors.BLUE.getIndex());
        this.blueTabStyle.setWrapText(true);
        this.blueTabStyle.setBorderLeft(BorderStyle.THIN);
        this.blueTabStyle.setBorderRight(BorderStyle.THIN);
        this.blueTabStyle.setBorderTop(BorderStyle.THIN);
        this.blueTabStyle.setBorderBottom(BorderStyle.THIN);
        this.blueTabStyle.setAlignment(HorizontalAlignment.LEFT);
        this.blueTabStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blueTabStyle.setFont(blueTitleTabFont);


        //init LabelStyle
        this.yellowHeader.setWrapText(true);
        this.yellowHeader.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        this.yellowHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.yellowHeader.setBorderLeft(BorderStyle.THIN);
        this.yellowHeader.setBorderRight(BorderStyle.THIN);
        this.yellowHeader.setBorderTop(BorderStyle.THIN);
        this.yellowHeader.setBorderBottom(BorderStyle.THIN);
        this.yellowHeader.setAlignment(HorizontalAlignment.LEFT);
        this.yellowHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        this.yellowHeader.setFont(titleHeadFont);

        this.underscoreHeader.setWrapText(true);
        this.underscoreHeader.setBorderLeft(BorderStyle.NONE);
        this.underscoreHeader.setBorderTop(BorderStyle.NONE);
        this.underscoreHeader.setBorderRight(BorderStyle.NONE);
        this.underscoreHeader.setBorderBottom(BorderStyle.THIN);
        this.underscoreHeader.setAlignment(HorizontalAlignment.LEFT);
        this.underscoreHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        this.underscoreHeader.setFont(titleHeadFont);

        this.labelOnLightYellow.setWrapText(true);
        this.labelOnLightYellow.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        this.labelOnLightYellow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnLightYellow.setBorderLeft(BorderStyle.THIN);
        this.labelOnLightYellow.setBorderRight(BorderStyle.THIN);
        this.labelOnLightYellow.setBorderTop(BorderStyle.THIN);
        this.labelOnLightYellow.setBorderBottom(BorderStyle.THIN);
        this.labelOnLightYellow.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnLightYellow.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnLightYellow.setFont(labelHeadFont);

        this.labelOnYellow.setWrapText(true);
        this.labelOnYellow.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        this.labelOnYellow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnYellow.setBorderLeft(BorderStyle.THIN);
        this.labelOnYellow.setBorderRight(BorderStyle.THIN);
        this.labelOnYellow.setBorderTop(BorderStyle.THIN);
        this.labelOnYellow.setBorderBottom(BorderStyle.THIN);
        this.labelOnYellow.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnYellow.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnYellow.setFont(labelHeadFont);

        this.doubleOnYellowStyle.setWrapText(true);
        this.doubleOnYellowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        this.doubleOnYellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.doubleOnYellowStyle.setBorderLeft(BorderStyle.THIN);
        this.doubleOnYellowStyle.setBorderRight(BorderStyle.THIN);
        this.doubleOnYellowStyle.setBorderTop(BorderStyle.THIN);
        this.doubleOnYellowStyle.setBorderBottom(BorderStyle.THIN);
        this.doubleOnYellowStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.doubleOnYellowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.doubleOnYellowStyle.setFont(tabFont);
        this.doubleOnYellowStyle.setDataFormat(format.getFormat("#,##0.00"));


        Font whiteTabFont = this.wb.createFont();
        whiteTabFont.setFontHeightInPoints((short) 12);
        whiteTabFont.setFontName("Calibri");
        whiteTabFont.setBold(false);
        whiteTabFont.setColor(IndexedColors.WHITE.getIndex());
        this.whiteOnBlueLabel.setWrapText(true);
        this.whiteOnBlueLabel.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        this.whiteOnBlueLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.whiteOnBlueLabel.setBorderLeft(BorderStyle.THIN);
        this.whiteOnBlueLabel.setBorderRight(BorderStyle.THIN);
        this.whiteOnBlueLabel.setBorderTop(BorderStyle.THIN);
        this.whiteOnBlueLabel.setBorderBottom(BorderStyle.THIN);
        this.whiteOnBlueLabel.setAlignment(HorizontalAlignment.LEFT);
        this.whiteOnBlueLabel.setVerticalAlignment(VerticalAlignment.CENTER);
        this.whiteOnBlueLabel.setFont(whiteTabFont);


        Font blackOnGreenHeaderFont = this.wb.createFont();
        blackOnGreenHeaderFont.setFontHeightInPoints((short) 23);
        blackOnGreenHeaderFont.setFontName("Calibri");
        blackOnGreenHeaderFont.setBold(true);
        this.blackOnGreenHeaderStyle.setWrapText(true);
        this.blackOnGreenHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        this.blackOnGreenHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.blackOnGreenHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.blackOnGreenHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.blackOnGreenHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.blackOnGreenHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.blackOnGreenHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blackOnGreenHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blackOnGreenHeaderStyle.setFont(blackOnGreenHeaderFont);

        Font blackOnRedLabelFont = this.wb.createFont();
        blackOnRedLabelFont.setFontHeightInPoints((short) 20);
        blackOnRedLabelFont.setFontName("Calibri");
        blackOnRedLabelFont.setBold(true);
        this.LabelBlackOnRed.setWrapText(true);
        this.LabelBlackOnRed.setFillForegroundColor(IndexedColors.RED.getIndex());
        this.LabelBlackOnRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.LabelBlackOnRed.setBorderLeft(BorderStyle.THIN);
        this.LabelBlackOnRed.setBorderRight(BorderStyle.THIN);
        this.LabelBlackOnRed.setBorderTop(BorderStyle.THIN);
        this.LabelBlackOnRed.setBorderBottom(BorderStyle.THIN);
        this.LabelBlackOnRed.setAlignment(HorizontalAlignment.CENTER);
        this.LabelBlackOnRed.setVerticalAlignment(VerticalAlignment.CENTER);
        this.LabelBlackOnRed.setFont(blackOnRedLabelFont);

        this.blackTitleHeaderBorderlessStyle.setWrapText(true);
        this.blackTitleHeaderBorderlessStyle.setBorderLeft(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessStyle.setBorderRight(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessStyle.setBorderTop(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessStyle.setBorderBottom(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessStyle.setAlignment(HorizontalAlignment.LEFT);
        this.blackTitleHeaderBorderlessStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blackTitleHeaderBorderlessStyle.setFont(blackOnRedLabelFont);

        this.blackTitleHeaderBorderlessCenteredStyle.setWrapText(true);
        this.blackTitleHeaderBorderlessCenteredStyle.setBorderLeft(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessCenteredStyle.setBorderRight(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessCenteredStyle.setBorderTop(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessCenteredStyle.setBorderBottom(BorderStyle.NONE);
        this.blackTitleHeaderBorderlessCenteredStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blackTitleHeaderBorderlessCenteredStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blackTitleHeaderBorderlessCenteredStyle.setFont(titleHeadFont);

        this.blueTitleHeaderBorderlessCenteredStyle.setWrapText(true);
        this.blueTitleHeaderBorderlessCenteredStyle.setBorderLeft(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredStyle.setBorderRight(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredStyle.setBorderTop(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredStyle.setBorderBottom(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blueTitleHeaderBorderlessCenteredStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blueTitleHeaderBorderlessCenteredStyle.setFont(blueTitleHeadFont);
        this.blueTitleHeaderBorderlessCenteredStyle.setDataFormat(format.getFormat("#,##0.00"));

        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setWrapText(true);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setBorderLeft(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setBorderRight(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setBorderTop(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setBorderBottom(BorderStyle.NONE);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setFont(blueTitleHeadFont);
        this.blueTitleHeaderBorderlessCenteredCurrencyStyle.setDataFormat(format.getFormat("#,##0.00 €"));

        this.blackOnBlueHeader.setWrapText(true);
        this.blackOnBlueHeader.setBorderLeft(BorderStyle.THIN);
        this.blackOnBlueHeader.setBorderRight(BorderStyle.THIN);
        this.blackOnBlueHeader.setBorderTop(BorderStyle.THIN);
        this.blackOnBlueHeader.setBorderBottom(BorderStyle.THIN);
        this.blackOnBlueHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blackOnBlueHeader.setAlignment(HorizontalAlignment.CENTER);
        this.blackOnBlueHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.blackOnBlueHeader.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        this.blackOnBlueHeader.setFont(titleHeadFont);

        this.yellowTab.setWrapText(true);
        this.yellowTab.setBorderLeft(BorderStyle.THIN);
        this.yellowTab.setBorderRight(BorderStyle.THIN);
        this.yellowTab.setBorderTop(BorderStyle.THIN);
        this.yellowTab.setBorderBottom(BorderStyle.THIN);
        this.yellowTab.setVerticalAlignment(VerticalAlignment.CENTER);
        this.yellowTab.setAlignment(HorizontalAlignment.RIGHT);
        this.yellowTab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.yellowTab.setFillForegroundColor(IndexedColors.YELLOW1.getIndex());
        this.yellowTab.setFont(tabFontBold);

        this.yellowTabPrice.setWrapText(true);
        this.yellowTabPrice.setBorderLeft(BorderStyle.THIN);
        this.yellowTabPrice.setBorderRight(BorderStyle.THIN);
        this.yellowTabPrice.setBorderTop(BorderStyle.THIN);
        this.yellowTabPrice.setBorderBottom(BorderStyle.THIN);
        this.yellowTabPrice.setVerticalAlignment(VerticalAlignment.CENTER);
        this.yellowTabPrice.setAlignment(HorizontalAlignment.RIGHT);
        this.yellowTabPrice.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.yellowTabPrice.setFillForegroundColor(IndexedColors.YELLOW1.getIndex());
        this.yellowTabPrice.setFont(totalFont);
        this.yellowTabPrice.setDataFormat(format.getFormat("#,##0.00 €"));


        this.dateFormatStyle.setWrapText(true);
        this.dateFormatStyle.setBorderLeft(BorderStyle.THIN);
        this.dateFormatStyle.setBorderRight(BorderStyle.THIN);
        this.dateFormatStyle.setBorderTop(BorderStyle.THIN);
        this.dateFormatStyle.setBorderBottom(BorderStyle.THIN);
        this.dateFormatStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.dateFormatStyle.setAlignment(HorizontalAlignment.CENTER);
        this.dateFormatStyle.setFont(tabFont);
        this.dateFormatStyle.setDataFormat(format.getFormat("m/d/yy"));

        this.currencyFormatStyle.setWrapText(true);
        this.currencyFormatStyle.setBorderLeft(BorderStyle.THIN);
        this.currencyFormatStyle.setBorderRight(BorderStyle.THIN);
        this.currencyFormatStyle.setBorderTop(BorderStyle.THIN);
        this.currencyFormatStyle.setBorderBottom(BorderStyle.THIN);
        this.currencyFormatStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.currencyFormatStyle.setAlignment(HorizontalAlignment.CENTER);
        this.currencyFormatStyle.setFont(tabFont);
        this.currencyFormatStyle.setDataFormat(format.getFormat("#,##0.00€"));


        this.numberFormatStyle.setWrapText(true);
        this.numberFormatStyle.setBorderLeft(BorderStyle.THIN);
        this.numberFormatStyle.setBorderRight(BorderStyle.THIN);
        this.numberFormatStyle.setBorderTop(BorderStyle.THIN);
        this.numberFormatStyle.setBorderBottom(BorderStyle.THIN);
        this.numberFormatStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.numberFormatStyle.setAlignment(HorizontalAlignment.CENTER);
        this.numberFormatStyle.setFont(tabFont);
        this.numberFormatStyle.setDataFormat(format.getFormat("0"));

        this.standardTextStyle.setWrapText(true);
        this.standardTextStyle.setBorderLeft(BorderStyle.THIN);
        this.standardTextStyle.setBorderRight(BorderStyle.THIN);
        this.standardTextStyle.setBorderTop(BorderStyle.THIN);
        this.standardTextStyle.setBorderBottom(BorderStyle.THIN);
        this.standardTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.standardTextStyle.setAlignment(HorizontalAlignment.CENTER);

        this.labelOnGrey.setBorderLeft(BorderStyle.THIN);
        this.labelOnGrey.setBorderRight(BorderStyle.THIN);
        this.labelOnGrey.setBorderTop(BorderStyle.THIN);
        this.labelOnGrey.setBorderBottom(BorderStyle.THIN);
        this.labelOnGrey.setWrapText(true);
        this.labelOnGrey.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        this.labelOnGrey.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnGrey.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnGrey.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnGrey.setFont(headerFont);

        this.labelOnBlueGrey.setBorderLeft(BorderStyle.THIN);
        this.labelOnBlueGrey.setBorderRight(BorderStyle.THIN);
        this.labelOnBlueGrey.setBorderTop(BorderStyle.THIN);
        this.labelOnBlueGrey.setBorderBottom(BorderStyle.THIN);
        this.labelOnBlueGrey.setWrapText(true);
        this.labelOnBlueGrey.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        this.labelOnBlueGrey.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnBlueGrey.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnBlueGrey.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnBlueGrey.setFont(headerFont);

        this.labelOnOrange.setBorderLeft(BorderStyle.THIN);
        this.labelOnOrange.setBorderRight(BorderStyle.THIN);
        this.labelOnOrange.setBorderTop(BorderStyle.THIN);
        this.labelOnOrange.setBorderBottom(BorderStyle.THIN);
        this.labelOnOrange.setWrapText(true);
        this.labelOnOrange.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        this.labelOnOrange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnOrange.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnOrange.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnOrange.setFont(headerFont);

        this.labelOnLightGreen.setBorderLeft(BorderStyle.THIN);
        this.labelOnLightGreen.setBorderRight(BorderStyle.THIN);
        this.labelOnLightGreen.setBorderTop(BorderStyle.THIN);
        this.labelOnLightGreen.setBorderBottom(BorderStyle.THIN);
        this.labelOnLightGreen.setWrapText(true);
        this.labelOnLightGreen.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        this.labelOnLightGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnLightGreen.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnLightGreen.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnLightGreen.setFont(headerFont);

        this.labelOnLimeGreen.setBorderLeft(BorderStyle.THIN);
        this.labelOnLimeGreen.setBorderRight(BorderStyle.THIN);
        this.labelOnLimeGreen.setBorderTop(BorderStyle.THIN);
        this.labelOnLimeGreen.setBorderBottom(BorderStyle.THIN);
        this.labelOnLimeGreen.setWrapText(true);
        this.labelOnLimeGreen.setFillForegroundColor(IndexedColors.LIME.getIndex());
        this.labelOnLimeGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnLimeGreen.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnLimeGreen.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnLimeGreen.setFont(headerFont);



        this.labelOnPink.setBorderLeft(BorderStyle.THIN);
        this.labelOnPink.setBorderRight(BorderStyle.THIN);
        this.labelOnPink.setBorderTop(BorderStyle.THIN);
        this.labelOnPink.setBorderBottom(BorderStyle.THIN);
        this.labelOnPink.setWrapText(true);
        this.labelOnPink.setFillForegroundColor(IndexedColors.PINK.getIndex());
        this.labelOnPink.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.labelOnPink.setAlignment(HorizontalAlignment.CENTER);
        this.labelOnPink.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelOnPink.setFont(headerFont);
    }
}
