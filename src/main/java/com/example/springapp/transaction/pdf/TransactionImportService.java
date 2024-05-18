package com.example.springapp.transaction.pdf;

import com.example.springapp.transaction.Transaction;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TransactionImportService {

    public void importPdf(String filePath, String userName) throws IOException {
        File file = new File(filePath);
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);

        String[] splitDocument= text.split("\n");
        for (int i = 0; i < splitDocument.length; i++) {

            if(isTransactionStarted(splitDocument[i])){
                String headerInfo = splitDocument[i];
                log.info("Transaction starts: {}", splitDocument[i]);
                List<String> transactionDetails = new ArrayList<>();
                while(i+1 < splitDocument.length - 1 && !isTransactionStarted(splitDocument[i+1]) && !isPageDetail(splitDocument[i+1])){
                    i++;
                    transactionDetails.add(splitDocument[i+1]);
                }
                
                Transaction transaction = createTransaction(headerInfo, transactionDetails);
            }
        }
        document.close();
    }

    private Transaction createTransaction(String transactionHeaderInformation, List<String> transactionDetails) {
        String[] split = transactionHeaderInformation.split(" ");

        //TODO: get the date split[0] and set it to transaction
        //process transaction details and add them to the transaction
        return new Transaction();
    }

    private boolean isPageDetail(String line) {
        String regex = ".*\\bPagina\\b.*";

        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(line);

        return matcher.matches();
    }

    private boolean isTransactionStarted(String line){
        String regex = "\\b\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}\\.\\d{2}\\.\\d{4}\\b.*";

        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(line);
        return matcher.matches();
    }
}
