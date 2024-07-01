package com.example.springapp.report;

import com.example.springapp.transaction.Transaction;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionPdfExporter {
    private List<Transaction> transactionList;

    public TransactionPdfExporter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public void export(HttpServletResponse response) throws IOException {
        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add logo
        Image logo = new Image(ImageDataFactory.create("D:\\Disertatie\\paymint-web-app-main\\paymint-web-app-main\\springapp\\src\\main\\resources\\logo.png"));
        logo.setWidth(UnitValue.createPercentValue(50));  // Scale logo to 50% of page width
        logo.setAutoScaleHeight(true);  // Maintain aspect ratio
        document.add(logo);

        // Add title
        Paragraph title = new Paragraph("Raport Tranzacții")
                .setFontSize(18)
                .setBold()
                .setFontColor(ColorConstants.BLUE)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(title);

        // Add table
        float[] columnWidths = {2, 2, 3, 2, 4, 2, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(20);

        // Add table header
        addTableHeader(table);

        // Add table rows
        addTableRows(table);

        document.add(table);
        document.close();
    }

    private void addTableHeader(Table table) {
        String[] headers = {"Data", "Suma", "Categoria", "Tip", "Descriere", "Cont", "Tipul platii"};

        for (String header : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(header)
                            .setBold()
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
    }

    private void addTableRows(Table table) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (Transaction transaction : transactionList) {
            Instant instant = Instant.ofEpochMilli(transaction.getDateTime());
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            String formattedDate = dateTime.format(dateFormatter);

            table.addCell(new Cell().add(new Paragraph(formattedDate)));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(transaction.getAmount())+" lei")));
            table.addCell(new Cell().add(new Paragraph(transaction.getCategory().getName())));
            String categoryTypeText = transaction.getCategory().getType().equals("expense") ?
                    "Cheltuială" : transaction.getCategory().getType().equals("income") ? "Venit" : transaction.getCategory().getType();
            table.addCell(new Cell().add(new Paragraph(categoryTypeText)));
            table.addCell(new Cell().add(new Paragraph(transaction.getDescription())));
            table.addCell(new Cell().add(new Paragraph(transaction.getAccount().getName())));
            table.addCell(new Cell().add(new Paragraph(transaction.getPaymentType())));
        }
    }
}
